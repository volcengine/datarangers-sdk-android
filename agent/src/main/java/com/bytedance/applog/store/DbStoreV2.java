// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.store;

import android.content.ContentValues;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.bytedance.applog.engine.Engine;
import com.bytedance.applog.engine.Session;
import com.bytedance.applog.monitor.TraceAggregation;
import com.bytedance.applog.server.Api;
import com.bytedance.applog.util.HardwareUtils;
import com.bytedance.applog.util.TLog;
import com.bytedance.applog.util.Utils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 重构打包逻辑
 *
 * @author luodong.seu
 */
public class DbStoreV2 {
    private static final int DB_VERSION = 47;
    private static final int LIMIT_SELECT_PACK = 8;
    private static final long LIMIT_INTERVAL_SEND_FAIL =
            10 * 24 * 60 * 60 * 1000L; // pack保存最长时间：10天，单位：ms

    private final DbOpenHelper mOpenHelper;
    private final Engine mEngine;
    private final DbStoreHelper mStoreHelper;

    public DbStoreV2(final Engine engine, final String dbName) {
        mOpenHelper = new DbOpenHelper(engine, dbName, null, DB_VERSION);
        mEngine = engine;
        mStoreHelper = new DbStoreHelper(engine, mOpenHelper);
    }

    /** 保存所有的BaseData */
    public void saveAll(List<BaseData> baseDataList) {
        // 读取屏幕方向
        for (BaseData data : baseDataList) {
            loadScreenOrientation(data);
        }
        this.mStoreHelper.save(baseDataList);
    }

    /** 清理所有数据 */
    public synchronized void clear() {
        SQLiteDatabase db = null;
        try {
            db = mOpenHelper.getWritableDatabase();
            db.beginTransaction();
            final HashMap<String, BaseData> ZYGOTES = BaseData.getAllBaseDataObj();
            for (BaseData data : ZYGOTES.values()) {
                if (Utils.isNotEmpty(data.createTable())) {
                    db.delete(data.getTableName(), null, null);
                }
            }
            db.setTransactionSuccessful();
        } catch (Throwable e) {
            TLog.ysnp(e);
            mOpenHelper.traceDbError(e);
        } finally {
            if (null != db) {
                Utils.endDbTransactionSafely(db);
            }
        }
    }

    /**
     * 查询Pack列表
     *
     * @param appId 应用ID
     * @return List
     */
    public List<PackV2> queryPacks(String appId) {
        List<PackV2> results = new ArrayList<>();
        SQLiteDatabase db;
        Cursor cursor = null;
        try {
            db = mOpenHelper.getReadableDatabase();
            cursor =
                    db.rawQuery(
                            "SELECT * FROM "
                                    + PackV2.TABLE
                                    + " WHERE "
                                    + BaseData.COL_APP_ID
                                    + "= ?"
                                    + " ORDER BY "
                                    + PackV2.COL_ID
                                    + " DESC LIMIT "
                                    + LIMIT_SELECT_PACK,
                            new String[] {appId});
            while (cursor.moveToNext()) {
                PackV2 pack = new PackV2();
                pack.readDb(cursor);
                results.add(pack);
            }
        } catch (Throwable t) {
            TLog.ysnp(t);
            mOpenHelper.traceDbError(t);
        } finally {
            Utils.closeSafely(cursor);
        }
        return results;
    }

    /**
     * 打包数据库中的数据
     *
     * <pre>
     *     1. 联合3个表查询所有uuid
     * 2. 遍历uuid打包（事务中处理）
     *   1. 按uuid查询Launch
     *   2. 按uuid查询pages生成Terminate（非当前session_id）
     *   3. 按uuid查询EventV3（累计数量<200）
     *   4. 组装Pack对象（data为JSON原始数据）
     *   5. 存储Pack到数据库，删除上述数据
     * </pre>
     */
    public synchronized void pack(String appId, JSONObject header) {
        Set<String> uuidSet = queryAllUnionUuid(appId);
        if (uuidSet.isEmpty()) {
            return;
        }
        try {
            SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            for (String uuid : uuidSet) {
                PackV2 pack = new PackV2();
                pack.setAppId(appId);

                // header信息更新
                JSONObject tempHeader = new JSONObject();
                Utils.copy(tempHeader, header);

                // ssid单独处理
                tempHeader.remove(Api.KEY_SSID);
                tempHeader.put(
                        Api.KEY_USER_UNIQUE_ID, Utils.isEmpty(uuid) ? JSONObject.NULL : uuid);
                pack.setHeader(tempHeader);

                // query Launch
                List<Launch> launchList = queryAllLaunchByUuid(db, appId, uuid);
                pack.setLaunchList(launchList);

                // query page
                List<Page> pageList = queryAllPageByUuid(db, appId, uuid);
                List<Page> combinedPageList = new ArrayList<>();

                // query terminate
                List<Terminate> terminateList =
                        combinePagesAndCreateTerminate(pageList, combinedPageList);
                pack.setPageList(combinedPageList);
                pack.setTerminateList(terminateList);

                // query custom events
                List<CustomEvent> customEvents =
                        queryAllCustomEventByUuid(db, appId, uuid, pack.calcMaxEventCount());
                pack.setCustomEventList(customEvents);

                // query eventV3
                List<EventV3> eventV3List =
                        queryAllEventV3ByUuid(db, appId, uuid, pack.calcMaxEventV3Count());
                pack.setEventV3List(eventV3List);

                // 判断是否有数据
                if (pack.isEmpty()) {
                    continue;
                }

                // 从event中读取ssid
                pack.reloadSsidFromEvent();

                // 无ssid的pack重新发起一次注册获取ssid
                if (!mEngine.fetchIfNoSsidInHeader(tempHeader)) {
                    // 注册失败后先放弃本地打包，等待下次打包后重新注册
                    TLog.w("Register to get ssid by temp header failed.");
                    continue;
                }

                // 保存pack
                savePackAndDelete(db, pack);
            }
        } catch (Throwable e) {
            TLog.ysnp(e);
            mOpenHelper.traceDbError(e);
        }
    }

    /**
     * 打包监控数据
     *
     * <pre>
     *     一次性读取所有trace数据
     *     </>
     */
    public synchronized void packTrace(
            String appId, JSONObject header, TraceAggregation aggregation) {
        try {
            SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            List<Trace> traces = queryAllTrace(db, appId);
            PackV2 pack = new PackV2();
            JSONObject tempHeader = new JSONObject();
            Utils.copy(tempHeader, header);
            pack.setHeader(tempHeader);
            pack.setAppId(appId);
            if (null != aggregation) {
                pack.setTraceList(aggregation.aggregate(traces));
            } else {
                pack.setTraceList(traces);
            }
            savePackAndDelete(db, pack);
        } catch (Throwable e) {
            TLog.ysnp(e);
            mOpenHelper.traceDbError(e);
        }
    }

    /** Pack发送后删除记录或更新失败次数 */
    public synchronized void doAfterPackSend(List<PackV2> packs) {
        if (null == packs) {
            return;
        }

        SQLiteDatabase db = null;
        try {
            db = mOpenHelper.getWritableDatabase();
            db.beginTransaction();
            for (PackV2 pack : packs) {
                if (pack.fail == 0
                        || (pack.fail > 0
                                && Math.abs(System.currentTimeMillis() - pack.ts)
                                        > LIMIT_INTERVAL_SEND_FAIL)) {
                    // 发送成功或者重试失败且已经到达pack留存最长时间，删除pack
                    db.execSQL(
                            "DELETE FROM " + PackV2.TABLE + " WHERE " + PackV2.COL_ID + "=?",
                            new Object[] {pack.dbId});
                    continue;
                }

                // 失败的pack更新fail字段
                if (pack.fail > 0) {
                    db.execSQL(
                            "UPDATE "
                                    + PackV2.TABLE
                                    + " SET "
                                    + PackV2.COL_FAIL
                                    + "= ? WHERE "
                                    + PackV2.COL_ID
                                    + "= ?",
                            new Object[] {pack.fail, pack.dbId});
                }
            }
            db.setTransactionSuccessful();
        } catch (Throwable e) {
            TLog.ysnp(e);
            mOpenHelper.traceDbError(e);
        } finally {
            Utils.endDbTransactionSafely(db);
        }
    }

    /**
     * 查询所有的Profile事件
     *
     * <p>返回uuid-[profile]列表
     */
    public synchronized Map<String, List<Profile>> queryAllProfiles(final String appId) {
        Map<String, List<Profile>> uuidProfiles = new HashMap<>();
        Cursor cursor = null;
        try {
            SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            cursor =
                    db.rawQuery(
                            "SELECT * FROM "
                                    + Profile.TABLE
                                    + " WHERE "
                                    + BaseData.COL_APP_ID
                                    + "=? ORDER BY "
                                    + BaseData.COL_ID
                                    + " DESC LIMIT "
                                    + PackV2.LIMIT_EVENT_COUNT,
                            new String[] {appId});
            while (cursor.moveToNext()) {
                Profile profile = new Profile();
                profile.readDb(cursor);

                String uuid = Utils.toString(profile.uuid);
                List<Profile> profileList = uuidProfiles.get(uuid);
                if (null == profileList) {
                    profileList = new ArrayList<>();
                    uuidProfiles.put(uuid, profileList);
                }
                profileList.add(profile);
            }
        } catch (Throwable e) {
            TLog.ysnp(e);
            mOpenHelper.traceDbError(e);
        } finally {
            Utils.closeSafely(cursor);
        }
        return uuidProfiles;
    }

    /** 删除查询的profile列表 */
    public synchronized void deleteProfiles(List<Profile> profileList) {
        SQLiteDatabase db = null;
        try {
            db = mOpenHelper.getWritableDatabase();
            db.beginTransaction();
            for (Profile profile : profileList) {
                db.delete(
                        Profile.TABLE,
                        BaseData.COL_ID + "=?",
                        new String[] {String.valueOf(profile.dbId)});
            }
            db.setTransactionSuccessful();
        } catch (Throwable e) {
            TLog.ysnp(e);
            mOpenHelper.traceDbError(e);
        } finally {
            Utils.endDbTransactionSafely(db);
        }
    }

    /** 保存profiles */
    public synchronized void saveProfiles(List<Profile> profileList) {
        SQLiteDatabase db = null;
        try {
            db = mOpenHelper.getWritableDatabase();
            db.beginTransaction();
            ContentValues cv = null;
            for (Profile data : profileList) {
                db.insert(Profile.TABLE, null, cv = data.toValues(cv));
            }
            db.setTransactionSuccessful();
        } catch (Throwable e) {
            TLog.ysnp(e);
            mOpenHelper.traceDbError(e);
        } finally {
            Utils.endDbTransactionSafely(db);
        }
    }

    /** 更新所有指定uuid的ssid */
    public synchronized void updateSsid2Uuid(String uuid, String ssid) {
        SQLiteDatabase db = null;
        try {
            db = mOpenHelper.getWritableDatabase();
            db.beginTransaction();

            // 更新launch
            db.execSQL(
                    "UPDATE "
                            + Launch.TABLE
                            + " SET "
                            + BaseData.COL_SSID
                            + " = ? WHERE "
                            + BaseData.COL_UUID
                            + " = ? AND LENGTH("
                            + BaseData.COL_SSID
                            + ") = 0",
                    new String[] {ssid, uuid});

            // 更新Page
            db.execSQL(
                    "UPDATE "
                            + Page.TABLE
                            + " SET "
                            + BaseData.COL_SSID
                            + " = ? WHERE "
                            + BaseData.COL_UUID
                            + " = ? AND LENGTH("
                            + BaseData.COL_SSID
                            + ") = 0",
                    new String[] {ssid, uuid});

            // 更新Event
            db.execSQL(
                    "UPDATE "
                            + EventV3.TABLE
                            + " SET "
                            + BaseData.COL_SSID
                            + " = ? WHERE "
                            + BaseData.COL_UUID
                            + " = ? AND LENGTH("
                            + BaseData.COL_SSID
                            + ") = 0",
                    new String[] {ssid, uuid});

            // 更新Profile
            db.execSQL(
                    "UPDATE "
                            + Profile.TABLE
                            + " SET "
                            + BaseData.COL_SSID
                            + " = ? WHERE "
                            + BaseData.COL_UUID
                            + " = ? AND LENGTH("
                            + BaseData.COL_SSID
                            + ") = 0",
                    new String[] {ssid, uuid});

            // 更新Trace
            db.execSQL(
                    "UPDATE "
                            + Trace.TABLE
                            + " SET "
                            + BaseData.COL_SSID
                            + " = ? WHERE "
                            + BaseData.COL_UUID
                            + " = ? AND LENGTH("
                            + BaseData.COL_SSID
                            + ") = 0",
                    new String[] {ssid, uuid});
            db.setTransactionSuccessful();
        } catch (Throwable e) {
            TLog.ysnp(e);
            mOpenHelper.traceDbError(e);
        } finally {
            Utils.endDbTransactionSafely(db);
        }
    }

    /** 持久化Pack对象并删除本地的相关数据 */
    private synchronized void savePackAndDelete(SQLiteDatabase db, PackV2 pack) {
        try {
            db.beginTransaction();

            // 先保存Pack对象
            long dbId = db.insert(PackV2.TABLE, null, pack.toValues(null));
            if (dbId < 0) {
                return;
            }

            // 删除launch
            if (null != pack.getLaunchList()) {
                for (Launch l : pack.getLaunchList()) {
                    db.delete(
                            Launch.TABLE,
                            BaseData.COL_ID + " = ?",
                            new String[] {String.valueOf(l.dbId)});
                }
            }

            // 删除Page
            if (null != pack.getPageList()) {
                for (Page p : pack.getPageList()) {
                    db.delete(
                            Page.TABLE,
                            BaseData.COL_SID + " = ? and " + Page.COL_NAME + " = ?",
                            new String[] {String.valueOf(p.sid), p.name});
                }
            }

            // 删除CustomEvent
            if (null != pack.getCustomEventList()) {
                for (CustomEvent e : pack.getCustomEventList()) {
                    db.delete(
                            CustomEvent.TABLE,
                            BaseData.COL_ID + " = ?",
                            new String[] {String.valueOf(e.dbId)});
                }
            }

            // 删除Event
            if (null != pack.getEventV3List()) {
                for (EventV3 e : pack.getEventV3List()) {
                    db.delete(
                            EventV3.TABLE,
                            BaseData.COL_ID + " = ?",
                            new String[] {String.valueOf(e.dbId)});
                }
            }

            // 删除Trace
            if (null != pack.getTraceList()) {
                for (Trace e : pack.getTraceList()) {
                    db.delete(
                            Trace.TABLE,
                            BaseData.COL_ID + " = ?",
                            new String[] {String.valueOf(e.dbId)});
                }
            }
            db.setTransactionSuccessful();
        } catch (Throwable e) {
            TLog.ysnp(e);
            mOpenHelper.traceDbError(e);
        } finally {
            Utils.endDbTransactionSafely(db);
        }
    }

    /**
     * 通过本地的页面记录来创建所有已结束的Terminate事件
     *
     * @param combinedPageList 合并pause和resume事件后的page列表，用于eventV3事件上报
     */
    private List<Terminate> combinePagesAndCreateTerminate(
            List<Page> pageList, List<Page> combinedPageList) {
        String curSessionId = mEngine.getSessionId();

        List<Terminate> terminateList = new ArrayList<>();

        // 按sid分page列表
        Map<String, List<Page>> sessionPageMap = new HashMap<>();
        for (Page page : pageList) {
            if (Utils.equals(page.sid, curSessionId)) {
                // 不处理当前session
                // 当前session的terminate由下次计算
                continue;
            }
            final String sid = Utils.toString(page.sid);
            List<Page> pages = sessionPageMap.get(sid);
            if (null == pages) {
                pages = new ArrayList<>();
                sessionPageMap.put(sid, pages);
            }
            pages.add(page);
        }

        // 按sid来计算terminate
        for (Map.Entry<String, List<Page>> entry : sessionPageMap.entrySet()) {
            long duration = 0L;
            Map<String, Integer> pauseCount = new HashMap<>();

            // 计算terminate时间
            long terminateTs = 0L;
            Page lastPage = entry.getValue().get(0);
            for (Page page : entry.getValue()) {
                Integer count = pauseCount.get(page.name);
                if (page.isResumeEvent()) {
                    // resume事件
                    if (null != count) {
                        count = count - 1;
                        if (count > 0) {
                            pauseCount.put(page.name, count);
                        } else {
                            pauseCount.remove(page.name);
                        }
                    } else {
                        // 有resume事件但没有pause事件，补充一个page:1s
                        page.duration = 1000L;
                        duration += page.duration;
                        combinedPageList.add(page);
                    }
                } else {
                    // pause事件
                    page.duration = Math.max(1000L, page.duration);
                    duration += page.duration;
                    pauseCount.put(page.name, count != null ? count + 1 : 1);
                    combinedPageList.add(page);
                }
                if (page.ts + page.duration > terminateTs) {
                    terminateTs = Math.max(terminateTs, page.ts + page.duration);
                    lastPage = page;
                }
            }

            Terminate terminate = new Terminate();
            terminate.sid = entry.getKey();
            terminate.duration = duration;
            terminate.ts = terminateTs;
            terminate.uid = lastPage.uid;
            terminate.uuid = lastPage.uuid;
            terminate.ssid = lastPage.ssid;
            terminate.abSdkVersion = lastPage.abSdkVersion;
            terminate.stopTs = terminateTs;
            terminate.eid = Session.nextEventId();
            terminate.lastSession = null;
            if (!TextUtils.isEmpty(lastPage.lastSession)) {
                terminate.lastSession = lastPage.lastSession;
            }

            // 屏幕方向: 取最后一个页面的方向
            if (null != lastPage.getProperties()
                    && lastPage.getProperties().has(Api.KEY_SCREEN_ORIENTATION)) {
                try {
                    JSONObject props = new JSONObject();
                    props.put(
                            Api.KEY_SCREEN_ORIENTATION,
                            lastPage.getProperties().optString(Api.KEY_SCREEN_ORIENTATION));
                    terminate.setProperties(props);
                } catch (Throwable e) {
                    TLog.e(e);
                }
            }

            terminateList.add(terminate);
        }
        return terminateList;
    }

    /**
     * 查询出数据库中的所有用户uuid
     *
     * @param appId 应用id
     * @return Set
     */
    private Set<String> queryAllUnionUuid(String appId) {
        Set<String> result = new HashSet<>();
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor =
                    db.rawQuery(
                            "SELECT `"
                                    + BaseData.COL_UUID
                                    + "` FROM "
                                    + Launch.TABLE
                                    + " WHERE "
                                    + BaseData.COL_APP_ID
                                    + "= ?",
                            new String[] {appId});
            while (cursor.moveToNext()) {
                result.add(cursor.getString(0));
            }
        } finally {
            Utils.closeSafely(cursor);
        }
        try {
            cursor =
                    db.rawQuery(
                            "SELECT `"
                                    + BaseData.COL_UUID
                                    + "` FROM "
                                    + Page.TABLE
                                    + " WHERE "
                                    + BaseData.COL_APP_ID
                                    + "= ?",
                            new String[] {appId});
            while (cursor.moveToNext()) {
                result.add(cursor.getString(0));
            }
        } catch (Throwable e) {
            TLog.ysnp(e);
            mOpenHelper.traceDbError(e);
        } finally {
            Utils.closeSafely(cursor);
        }
        try {
            cursor =
                    db.rawQuery(
                            "SELECT `"
                                    + BaseData.COL_UUID
                                    + "` FROM "
                                    + EventV3.TABLE
                                    + " WHERE "
                                    + BaseData.COL_APP_ID
                                    + "= ?",
                            new String[] {appId});
            while (cursor.moveToNext()) {
                result.add(cursor.getString(0));
            }
        } catch (Throwable e) {
            TLog.ysnp(e);
            mOpenHelper.traceDbError(e);
        } finally {
            Utils.closeSafely(cursor);
        }
        return result;
    }

    /** 查询用户的所有Launch事件 */
    private List<Launch> queryAllLaunchByUuid(SQLiteDatabase db, String appId, String uuid) {
        List<Launch> launchList = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor =
                    db.rawQuery(
                            "SELECT * FROM "
                                    + Launch.TABLE
                                    + " WHERE "
                                    + BaseData.COL_APP_ID
                                    + "= ? and "
                                    + BaseData.COL_UUID
                                    + " = ?",
                            new String[] {appId, uuid});
            while (cursor.moveToNext()) {
                Launch launch = new Launch();
                launch.readDb(cursor);
                launchList.add(launch);

                // 查询是否存在page
                boolean hasSessionPage =
                        countByTableWhere(
                                        db,
                                        Page.TABLE,
                                        BaseData.COL_SID + " = ? LIMIT 1",
                                        new String[] {launch.sid})
                                > 0;
                // 是否后台启动
                launch.mBg = !hasSessionPage;
            }
        } catch (Throwable e) {
            TLog.ysnp(e);
            mOpenHelper.traceDbError(e);
        } finally {
            Utils.closeSafely(cursor);
        }
        return launchList;
    }

    /** SQL计数 */
    @SuppressWarnings("SameParameterValue")
    private int countByTableWhere(
            SQLiteDatabase db, String tableName, String where, String[] args) {
        if (null == db) {
            return 0;
        }
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT count(1) FROM " + tableName + " WHERE " + where, args);
            if (cursor.moveToNext()) {
                return cursor.getInt(0);
            }
        } catch (Throwable e) {
            TLog.ysnp(e);
            mOpenHelper.traceDbError(e);
        } finally {
            Utils.closeSafely(cursor);
        }
        return 0;
    }

    /** 查询页面记录。顺序：有duration的page在前面（即pause事件的page先处理） */
    private List<Page> queryAllPageByUuid(SQLiteDatabase db, String appId, String uuid) {
        List<Page> pageList = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor =
                    db.rawQuery(
                            "SELECT * FROM "
                                    + Page.TABLE
                                    + " WHERE "
                                    + BaseData.COL_APP_ID
                                    + "= ? and "
                                    + BaseData.COL_UUID
                                    + " = ? order by "
                                    + Page.COL_DURATION
                                    + " desc",
                            new String[] {appId, uuid});
            while (cursor.moveToNext()) {
                Page page = new Page();
                page.readDb(cursor);
                pageList.add(page);
            }
        } catch (Throwable e) {
            TLog.ysnp(e);
            mOpenHelper.traceDbError(e);
        } finally {
            Utils.closeSafely(cursor);
        }
        return pageList;
    }

    /** 查询所有自定义的事件 */
    private List<CustomEvent> queryAllCustomEventByUuid(
            SQLiteDatabase db, String appId, String uuid, int maxCount) {
        if (maxCount <= 0) {
            return new ArrayList<>();
        }
        List<CustomEvent> customEvents = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor =
                    db.rawQuery(
                            "SELECT * FROM "
                                    + CustomEvent.TABLE
                                    + " WHERE "
                                    + BaseData.COL_APP_ID
                                    + "= ? and "
                                    + BaseData.COL_UUID
                                    + " = ? limit 0, ?",
                            new String[] {appId, uuid, String.valueOf(maxCount)});
            while (cursor.moveToNext()) {
                CustomEvent customEvent = new CustomEvent();
                customEvent.readDb(cursor);
                customEvents.add(customEvent);
            }
        } finally {
            Utils.closeSafely(cursor);
        }
        return customEvents;
    }

    /**
     * 查询用户的所有eventV3事件（最多取maxCount个）
     *
     * @param maxCount 最大记录数
     */
    private List<EventV3> queryAllEventV3ByUuid(
            SQLiteDatabase db, String appId, String uuid, int maxCount) {
        if (maxCount <= 0) {
            return new ArrayList<>();
        }
        List<EventV3> eventV3List = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor =
                    db.rawQuery(
                            "SELECT * FROM "
                                    + EventV3.TABLE
                                    + " WHERE "
                                    + BaseData.COL_APP_ID
                                    + "= ? and "
                                    + BaseData.COL_UUID
                                    + " = ? limit 0, ?",
                            new String[] {appId, uuid, String.valueOf(maxCount)});
            while (cursor.moveToNext()) {
                EventV3 eventV3 = new EventV3();
                eventV3.readDb(cursor);
                eventV3List.add(eventV3);
            }
        } catch (Throwable e) {
            TLog.ysnp(e);
            mOpenHelper.traceDbError(e);
        } finally {
            Utils.closeSafely(cursor);
        }
        return eventV3List;
    }

    /** 加载屏幕方向 */
    private void loadScreenOrientation(@NotNull BaseData data) {
        if (!mEngine.getConfig().isScreenOrientationEnabled()) {
            return;
        }
        try {
            JSONObject properties =
                    null != data.getProperties() ? data.getProperties() : new JSONObject();
            int ori = HardwareUtils.getScreenOrientation(mEngine.getAppLog().getContext());
            properties.put(
                    Api.KEY_SCREEN_ORIENTATION,
                    ori == Configuration.ORIENTATION_LANDSCAPE ? "landscape" : "portrait");
            data.setProperties(properties);
        } catch (Throwable e) {
            TLog.e(e);
        }
    }

    /** 查询所有的监控记录 */
    private List<Trace> queryAllTrace(SQLiteDatabase db, String appId) {
        List<Trace> traces = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor =
                    db.rawQuery(
                            "SELECT * FROM "
                                    + Trace.TABLE
                                    + " WHERE "
                                    + BaseData.COL_APP_ID
                                    + "= ? ",
                            new String[] {appId});
            while (cursor.moveToNext()) {
                Trace trace = new Trace();
                trace.readDb(cursor);
                traces.add(trace);
            }
        } catch (Throwable e) {
            TLog.ysnp(e);
            mOpenHelper.traceDbError(e);
        } finally {
            Utils.closeSafely(cursor);
        }
        return traces;
    }
}
