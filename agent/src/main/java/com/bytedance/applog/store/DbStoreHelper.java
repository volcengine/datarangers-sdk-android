// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.store;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.bytedance.applog.engine.Engine;
import com.bytedance.applog.event.EventBasisParser;
import com.bytedance.applog.event.EventPolicy;
import com.bytedance.applog.event.EventType;
import com.bytedance.applog.event.IEventHandler;
import com.bytedance.applog.log.LogInfo;
import com.bytedance.applog.log.LogUtils;
import com.bytedance.applog.util.JsonUtils;
import com.bytedance.applog.util.Utils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库工具类
 *
 * @author luodong.seu
 */
public class DbStoreHelper {

    private final Engine mEngine;
    private final DbOpenHelper mOpenHelper;

    public DbStoreHelper(final Engine engine, DbOpenHelper dbOpenHelper) {
        this.mEngine = engine;
        this.mOpenHelper = dbOpenHelper;
    }

    /** 保存数据列表 */
    public void save(final List<BaseData> dataList) {
        if (null == dataList || dataList.isEmpty()) {
            return;
        }

        // 如果关闭埋点事件上报，则不存库
        final boolean trackDisabled =
                null != mEngine.getConfig()
                        && null != mEngine.getConfig().getInitConfig()
                        && !mEngine.getConfig().getInitConfig().isTrackEventEnabled();
        if (trackDisabled) {
            return;
        }

        ArrayList<Launch> launches = new ArrayList<>(4);
        ArrayList<Page> pages = new ArrayList<>(4);
        ArrayList<BaseData> events = new ArrayList<>(4);

        // 事件处理器
        IEventHandler eventHandler = mEngine.getAppLog().getEventHandler();

        SQLiteDatabase db = null;
        try {
            db = mOpenHelper.getWritableDatabase();
            db.beginTransaction();
            ContentValues cv = null;
            for (final BaseData data : dataList) {
                // 处理自定义事件处理器
                boolean isLaunchEnable = mEngine.getConfig().getInitConfig().isLaunchTerminateEnabled();
                if (!filterByEventHandler(eventHandler, data, isLaunchEnable)) {
                    return;
                }

                // 修复在init前采集的数据没有appId的问题
                if (TextUtils.isEmpty(data.appId)) {
                    data.setAppId(mEngine.getAppLog().getAppId());
                }
                if (data instanceof EventV3) {
                    // 补充事件 AppVersion 信息
                    BaseData.fillEventAppVersion(data, mEngine.getDm().getVersionName());
                }

                // hook
                mEngine.getLifeHook().beforeEventSave(data);

                data.dbId = db.insert(data.getTableName(), null, cv = data.toValues(cv));
                if ((EventV3.TABLE).equals(data.getTableName())) {
                    events.add(data);
                } else if (data instanceof Launch) {
                    launches.add((Launch) data);
                } else if (data instanceof Page) {
                    pages.add((Page) data);
                }

                // 发送event信息
                LogUtils.sendObject("event_save_db", data);
            }
            db.setTransactionSuccessful();
        } catch (Throwable t) {
            mEngine.getAppLog()
                    .getLogger()
                    .debug(LogInfo.Category.DATABASE, "Insert to table failed", t);
        } finally {
            Utils.endDbTransactionSafely(db);
        }

        // 回调通知
        notifyEventV3Observer(events);
        notifyPageObserver(pages);
        notifySessionStart(launches);
    }

    private void notifyPageObserver(ArrayList<Page> pages) {
        try {
            for (Page page : pages) {
                if (page.duration == -1) {
                    mEngine.getAppLog()
                            .getEventObserverHolder().onPageEnter(page.properties);
                } else {
                    mEngine.getAppLog()
                            .getEventObserverHolder().onPageLeave(page.properties);
                }
            }
        } catch (Throwable t) {
            mEngine.getAppLog()
                    .getLogger()
                    .debug(LogInfo.Category.DATABASE, "Notify event observer failed", t);
        }
    }

    public void notifyEventV3Observer(List<BaseData> events) {
        try {
            for (BaseData data : events) {
                if (EventV3.TABLE.equals(data.getTableName())) {
                    EventV3 event = (EventV3) data;
                    mEngine.getAppLog()
                            .getEventObserverHolder()
                            .onEventV3(
                                    event.event,
                                    event.param != null ? new JSONObject(event.param) : null);
                }
            }
        } catch (Throwable t) {
            mEngine.getAppLog()
                    .getLogger()
                    .debug(LogInfo.Category.DATABASE, "Notify event observer failed", t);
        }
    }

    private void notifySessionStart(ArrayList<Launch> launches) {
        try {
            for (Launch launch : launches) {
                mEngine.getAppLog()
                        .getSessionObserverHolder()
                        .onSessionStart(launch.dbId, launch.sid);
            }

            for (Launch launch : launches) {
                mEngine.getAppLog()
                        .getEventObserverHolder().onLaunch(launch.properties);
            }
        } catch (Throwable t) {
            mEngine.getAppLog()
                    .getLogger()
                    .debug(LogInfo.Category.DATABASE, "Notify session observer failed ", t);
        }
    }

    /**
     * 通过EventHandler回调过滤事件
     *
     * @param data BaseData
     * @param isLaunchEnable boolean
     * @return true:不过滤
     */
    private boolean filterByEventHandler(IEventHandler eventHandler, BaseData data, boolean isLaunchEnable) {
        if (null != data) {
            EventPolicy policy = null;
            if (null != eventHandler) {
                int acceptTypes = eventHandler.acceptType();
                if (data instanceof Click) {
                    if (EventType.hasEventType(acceptTypes, EventType.EVENT_CLICK)) {
                        policy =
                                doEventHandlerAndFillProperties(
                                        eventHandler,
                                        EventType.EVENT_CLICK,
                                        Click.EVENT_KEY,
                                        data,
                                        data.properties);
                    }
                } else if (data instanceof EventV3) {
                    if (EventType.hasEventType(acceptTypes, EventType.USER_EVENT)) {
                        policy =
                                doEventHandlerAndFillProperties(
                                        eventHandler,
                                        EventType.USER_EVENT,
                                        Utils.toString(((EventV3) data).getEvent()),
                                        data,
                                        data.properties);
                    }
                } else if (data instanceof Page) {
                    if (EventType.hasEventType(acceptTypes, EventType.EVENT_PAGE)) {
                        policy =
                                doEventHandlerAndFillProperties(
                                        eventHandler,
                                        EventType.EVENT_PAGE,
                                        Page.EVENT_KEY,
                                        data,
                                        data.properties);
                    }
                } else if (data instanceof Profile) {
                    if (EventType.hasEventType(acceptTypes, EventType.EVENT_PROFILE)) {
                        policy =
                                doEventHandlerAndFillProperties(
                                        eventHandler,
                                        EventType.EVENT_PROFILE,
                                        Utils.toString(((Profile) data).getEvent()),
                                        data,
                                        data.properties);
                    }
                }
            }
            if (data instanceof Launch) {
                policy = isLaunchEnable ? EventPolicy.ACCEPT : EventPolicy.DENY;
            }
            //noinspection RedundantIfStatement
            if (policy == EventPolicy.DENY) {
                // 丢弃
                return false;
            }
        }
        return true;
    }

    /**
     * 处理EventHandler并补充properties
     *
     * @param eventName 事件名
     * @param data BaseData
     * @return EventPolicy
     */
    private EventPolicy doEventHandlerAndFillProperties(
            IEventHandler eventHandler,
            int eventType,
            String eventName,
            BaseData data,
            JSONObject extraProps) {
        // 先准备params
        data.toPackJson();

        String propertiesString = data.getParam();
        JSONObject properties = new JSONObject();
        if (!TextUtils.isEmpty(propertiesString)) {
            try {
                properties = new JSONObject(propertiesString);
            } catch (Throwable ignored) {
                mEngine.getAppLog()
                        .getLogger()
                        .error(
                                LogInfo.Category.DATABASE,
                                "Param:[{}] is not a json string",
                                propertiesString);
            }
        }

        // 合并之前的props
        if (null != extraProps) {
            JsonUtils.mergeJsonObject(extraProps, properties);
        }
        EventPolicy policy = eventHandler.onReceive(eventType, eventName, properties, EventBasisParser.parseEvent(data));
        data.setProperties(properties);
        return policy;
    }
}
