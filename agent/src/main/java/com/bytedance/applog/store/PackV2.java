// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.store;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.bytedance.applog.AppLogHelper;
import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.event.AutoTrackEventType;
import com.bytedance.applog.log.LogInfo;
import com.bytedance.applog.server.Api;
import com.bytedance.applog.util.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 重构后的打包对象
 *
 * @author luodong.seu
 */
public class PackV2 extends BaseData {
    public static final int LIMIT_EVENT_COUNT = 200;
    public static final String TABLE = "packV2";

    public static final String COL_DATA = "_data";
    public static final String COL_FAIL = "_fail";
    public static final String COL_EVENT_LOCAL_IDS = "e_ids";

    private List<EventV3> eventV3List;
    private List<CustomEvent> customEventList;
    private List<Page> pageList;
    private List<Launch> launchList;
    private List<Terminate> terminateList;
    private JSONObject mHeader;
    public byte[] data;
    public int fail;
    private String eventLocalIds;

    public PackV2() {}

    @Override
    protected List<String> getColumnDef() {
        return Arrays.asList(
                COL_ID, "integer primary key autoincrement",
                COL_TS, "integer",
                COL_DATA, "blob",
                COL_FAIL, "integer",
                COL_EVENT_TYPE, "integer",
                COL_APP_ID, "varchar",
                COL_EVENT_LOCAL_IDS, "varchar");
    }

    @Override
    public int readDb(@NonNull final Cursor c) {
        int i = 0;
        dbId = c.getLong(i++);
        ts = c.getLong(i++);
        data = c.getBlob(i++);
        fail = c.getInt(i++);
        eventType = c.getInt(i++);
        appId = c.getString(i++);
        eventLocalIds = c.getString(i++);
        sid = "";
        return i;
    }

    @Override
    protected void writeDb(@NonNull final ContentValues cv) {
        cv.put(COL_TS, ts);
        cv.put(COL_DATA, toBytes());
        cv.put(COL_EVENT_TYPE, eventType);
        cv.put(COL_APP_ID, appId);
        cv.put(COL_EVENT_LOCAL_IDS, eventLocalIds);
    }

    private byte[] toBytes() {
        try {
            JSONObject packJson = toPackJson();
            return packJson.toString().getBytes("UTF-8");
        } catch (Throwable error) {
            getLogger()
                    .error(
                            LogInfo.Category.EVENT,
                            loggerTags,
                            "Convert json to bytes failed",
                            error);
        }
        return null;
    }

    @Override
    protected void writeIpc(@NonNull final JSONObject obj) {
        getLogger().error(LogInfo.Category.EVENT, loggerTags, "Not allowed");
    }

    @Override
    protected JSONObject writePack() throws JSONException {
        AppLogInstance appLogInstance = AppLogHelper.getInstanceByAppId(appId);
        final JSONObject obj = new JSONObject();
        obj.put(Api.KEY_MAGIC, Api.MSG_MAGIC);
        obj.put(Api.KEY_HEADER, mHeader);
        obj.put(Api.KEY_TIME_SYNC, Api.mTimeSync);
        // 事件ID
        Set<String> eventLocalIdList = new HashSet<>();

        if (launchList != null && !launchList.isEmpty()) {
            final JSONArray launchArray = new JSONArray();
            for (Launch l : launchList) {
                launchArray.put(l.toPackJson());
                eventLocalIdList.add(l.localEventId);
            }
            obj.put(Api.KEY_LAUNCH, launchArray);
        }

        if (terminateList != null && !terminateList.isEmpty()) {
            final JSONArray termArray = new JSONArray();
            for (Terminate terminate : terminateList) {
                JSONObject termJson = terminate.toPackJson();

                // launch_from
                if (null != appLogInstance && appLogInstance.getLaunchFrom() > 0) {
                    termJson.put(Api.KEY_LAUNCH_FROM, appLogInstance.getLaunchFrom());
                    appLogInstance.setLaunchFrom(0);
                }

                if (pageList == null) {
                    continue;
                }
                // 筛选出同session id的页面
                List<Page> terminatePages = new ArrayList<>();
                for (Page p : pageList) {
                    if (Utils.equals(p.sid, terminate.sid)) {
                        terminatePages.add(p);
                    }
                }
                if (terminatePages.size() == 0) {
                    continue;
                }
                // 需要把page放到term中作为activity，向前兼容
                final int count = terminatePages.size();
                JSONArray activities = new JSONArray();
                long pageTimeMs = 0;
                for (int i = 0; i < count; ++i) {
                    Page page = terminatePages.get(i);
                    JSONArray act = new JSONArray();
                    act.put(0, page.name);
                    act.put(1, (page.duration + 999) / 1000);
                    activities.put(act);

                    // 预置事件补充，最后一个page的信息
                    long localTimeMs = page.ts;
                    if (localTimeMs > pageTimeMs) {
                        termJson.put("$page_title", Utils.toString(page.title));
                        termJson.put("$page_key", Utils.toString(page.name));
                        pageTimeMs = localTimeMs;
                    }
                }
                termJson.put(Api.KEY_ACTIVITES, activities);

                termArray.put(termJson);
                eventLocalIdList.add(terminate.localEventId);
            }
            obj.put(Api.KEY_TERMINATE, termArray);
        }

        JSONArray eventArray = getEventV3Array(eventLocalIdList);
        final int v3Count = eventArray.length();
        if (v3Count > 0) {
            obj.put(Api.KEY_V3, eventArray);
        }

        // custom event
        if (null != customEventList && !customEventList.isEmpty()) {
            Map<String, JSONArray> categoryCustomMap = new HashMap<>();
            for (CustomEvent ce : customEventList) {
                JSONArray events = categoryCustomMap.get(ce.getCategory());
                if (null == events) {
                    events = new JSONArray();
                    categoryCustomMap.put(ce.getCategory(), events);
                }
                events.put(ce.toPackJson());
                eventLocalIdList.add(ce.localEventId);
            }
            for (Map.Entry<String, JSONArray> entry : categoryCustomMap.entrySet()) {
                obj.put(entry.getKey(), entry.getValue());
            }
        }

        // 事件ID以逗号分割
        eventLocalIds = TextUtils.join(",", eventLocalIdList);

        getLogger().debug(LogInfo.Category.EVENT, loggerTags, "Pack success ts:{}", ts);

        return obj;
    }

    @Override
    protected BaseData readIpc(@NonNull final JSONObject obj) {
        getLogger().error(LogInfo.Category.EVENT, loggerTags, "Not allowed");
        return null;
    }

    @NonNull
    @Override
    String getTableName() {
        return TABLE;
    }

    @Override
    protected String getDetail() {
        return String.valueOf(dbId);
    }

    public void setPageList(List<Page> pageList) {
        this.pageList = pageList;
    }

    public void setLaunchList(List<Launch> launchList) {
        this.launchList = launchList;
    }

    public void setTerminateList(List<Terminate> terminateList) {
        this.terminateList = terminateList;
    }

    public void setEventV3List(List<EventV3> eventV3s) {
        this.eventV3List = eventV3s;
    }

    public void setHeader(JSONObject header) {
        this.mHeader = header;
    }

    public List<EventV3> getEventV3List() {
        return eventV3List;
    }

    public List<Page> getPageList() {
        return pageList;
    }

    public List<Launch> getLaunchList() {
        return launchList;
    }

    public void setCustomEventList(List<CustomEvent> events) {
        this.customEventList = events;
    }

    public List<CustomEvent> getCustomEventList() {
        return customEventList;
    }

    /**
     * 计算最多的event事件数量
     *
     * @return number may < 0
     */
    public int calcMaxEventCount() {
        int count = LIMIT_EVENT_COUNT;
        if (null != launchList) {
            count -= launchList.size();
        }
        if (null != terminateList) {
            count -= terminateList.size();
        }
        AppLogInstance appLogInstance = AppLogHelper.getInstanceByAppId(appId);
        if (null != appLogInstance && appLogInstance.isBavEnabled() && null != pageList) {
            count -= pageList.size();
        }
        return count;
    }

    /** 计算最多的eventV3事件数量 */
    public int calcMaxEventV3Count() {
        int maxEventCount = calcMaxEventCount();
        if (null != customEventList) {
            maxEventCount -= customEventList.size();
        }
        return maxEventCount;
    }

    /**
     * 是否没有实际数据
     *
     * @return true:无数据
     */
    public boolean isEmpty() {
        return (null == launchList || launchList.isEmpty())
                && (null == terminateList || terminateList.isEmpty())
                && getEventV3Array(null).length() == 0
                && (null == customEventList || customEventList.isEmpty());
    }

    /** 重新加载ssid：从所有数据中读取ssid */
    public void reloadSsidFromEvent() {
        if (null == mHeader) {
            return;
        }
        mHeader.remove(Api.KEY_SSID);
        try {
            if (null != launchList) {
                for (Launch launch : launchList) {
                    if (Utils.isNotEmpty(launch.ssid)) {
                        mHeader.put(Api.KEY_SSID, launch.ssid);
                        return;
                    }
                }
            }
            if (null != pageList) {
                for (Page page : pageList) {
                    if (Utils.isNotEmpty(page.ssid)) {
                        mHeader.put(Api.KEY_SSID, page.ssid);
                        return;
                    }
                }
            }
            if (null != customEventList) {
                for (CustomEvent customEvent : customEventList) {
                    if (Utils.isNotEmpty(customEvent.ssid)) {
                        mHeader.put(Api.KEY_SSID, customEvent.ssid);
                        return;
                    }
                }
            }
            if (null != eventV3List) {
                for (EventV3 eventV3 : eventV3List) {
                    if (Utils.isNotEmpty(eventV3.ssid)) {
                        mHeader.put(Api.KEY_SSID, eventV3.ssid);
                        return;
                    }
                }
            }
        } catch (Throwable e) {
            getLogger()
                    .error(LogInfo.Category.EVENT, loggerTags, "Reload ssid from event failed", e);
        }
    }

    /** 重新加载uuid type：从所有数据中读取uuidType */
    public void reloadUuidTypeFromEvent() {
        if (null == mHeader) {
            return;
        }
        mHeader.remove(Api.KEY_USER_UNIQUE_ID_TYPE);
        try {
            if (null != launchList) {
                for (Launch launch : launchList) {
                    if (Utils.isNotEmpty(launch.uuidType)) {
                        mHeader.put(Api.KEY_USER_UNIQUE_ID_TYPE, launch.uuidType);
                        return;
                    }
                }
            }
            if (null != pageList) {
                for (Page page : pageList) {
                    if (Utils.isNotEmpty(page.uuidType)) {
                        mHeader.put(Api.KEY_USER_UNIQUE_ID_TYPE, page.uuidType);
                        return;
                    }
                }
            }
            if (null != customEventList) {
                for (CustomEvent customEvent : customEventList) {
                    if (Utils.isNotEmpty(customEvent.uuidType)) {
                        mHeader.put(Api.KEY_USER_UNIQUE_ID_TYPE, customEvent.uuidType);
                        return;
                    }
                }
            }
            if (null != eventV3List) {
                for (EventV3 eventV3 : eventV3List) {
                    if (Utils.isNotEmpty(eventV3.uuidType)) {
                        mHeader.put(Api.KEY_USER_UNIQUE_ID_TYPE, eventV3.uuidType);
                        return;
                    }
                }
            }
        } catch (Throwable e) {
            getLogger()
                    .error(
                            LogInfo.Category.EVENT,
                            loggerTags,
                            "Reload uuid type from event failed",
                            e);
        }
    }

    /**
     * 处理eventV3字段
     *
     * @return JSONArray
     */
    private JSONArray getEventV3Array(Set<String> eventLocalIds) {
        AppLogInstance appLogInstance = AppLogHelper.getInstanceByAppId(appId);
        JSONArray eventArray = new JSONArray();

        // bav2b_page
        if (null != appLogInstance && appLogInstance.isBavEnabled()) {
            if (null != pageList) {
                boolean notTrackBav2bPage =
                        null != appLogInstance.getInitConfig()
                                && !AutoTrackEventType.hasEventType(
                                        appLogInstance.getInitConfig().getAutoTrackEventType(),
                                        AutoTrackEventType.PAGE);
                if (!notTrackBav2bPage) {
                    for (Page p : pageList) {
                        eventArray.put(p.toPackJson());
                        if (null != eventLocalIds) {
                            eventLocalIds.add(p.localEventId);
                        }
                    }
                }
            }
        } else if (null != pageList) {
            for (Page p : pageList) {
                if (p.isCustom) {
                    // 自定义埋点不受全埋点控制
                    eventArray.put(p.toPackJson());
                    if (null != eventLocalIds) {
                        eventLocalIds.add(p.localEventId);
                    }
                }
            }
        }

        // eventV3
        if (null != eventV3List && !eventV3List.isEmpty()) {
            for (EventV3 e : eventV3List) {
                eventArray.put(e.toPackJson());
                if (null != eventLocalIds) {
                    eventLocalIds.add(e.localEventId);
                }
            }
        }
        return eventArray;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("Pack detail:");
        int eventCount = 0;
        if (null != eventV3List) {
            eventCount += eventV3List.size();
        }
        if (null != customEventList) {
            eventCount += customEventList.size();
        }
        if (eventCount > 0) {
            stringBuilder.append("\teventCount=").append(eventCount);
        }
        if (null != pageList && !pageList.isEmpty()) {
            stringBuilder.append("\tpageCount=").append(pageList.size());
        }
        if (null != launchList && !launchList.isEmpty()) {
            stringBuilder.append("\tlaunchCount=").append(launchList.size());
        }
        if (null != terminateList && !terminateList.isEmpty()) {
            stringBuilder.append("\tterminateCount=").append(terminateList.size());
        }
        if (fail > 0) {
            stringBuilder.append("\tfailCount=").append(fail);
        }
        return stringBuilder.toString();
    }

    public Set<String> getEventLocalIds() {
        Set<String> idSet = new HashSet<>();
        if (TextUtils.isEmpty(eventLocalIds)) {
            return idSet;
        }
        String[] ids = eventLocalIds.split(",");
        idSet.addAll(Arrays.asList(ids));
        return idSet;
    }

    public void writePackToData() {
        data = toBytes();
    }
}
