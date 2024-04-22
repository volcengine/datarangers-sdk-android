// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.engine;

import android.os.Bundle;
import android.text.TextUtils;

import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.IAppLogInstance;
import com.bytedance.applog.collector.Navigator;
import com.bytedance.applog.exception.ExceptionHandler;
import com.bytedance.applog.log.EventBus;
import com.bytedance.applog.log.LogUtils;
import com.bytedance.applog.manager.ConfigManager;
import com.bytedance.applog.manager.DeviceManager;
import com.bytedance.applog.store.BaseData;
import com.bytedance.applog.store.EventV3;
import com.bytedance.applog.store.Launch;
import com.bytedance.applog.store.Page;
import com.bytedance.applog.store.Terminate;
import com.bytedance.applog.util.NetworkUtils;
import com.bytedance.applog.util.Utils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 管理"会话"的生命期，并处理"跨进程的页面流转"的信息。
 *
 * <ul>
 *   <li>会话：用户的一串连续操作，称为一次会话。分为两种:
 *       <ul>
 *         <li>前台会话：打开UI，则会开启一次前台会话；前台会话切后台停留30s（可配置），会话终止。前台会话上报一个{@link Launch}和一个{@link
 *             Terminate}事件。
 *         <li>后台会话：未打开UI，收到上报事件，则开启一个后台会话。后台会话仅在启动UI时终止。后台会话仅上报一个{@link Launch}事件。
 *       </ul>
 *   <li>页面流转：进程内的页面流转，在{@link Navigator}中处理，跨进程的页面流转，在{@link Session#(BaseData, ArrayList)}中处理。
 * </ul>
 *
 * @author shiyanlong
 * @date 2019/1/29
 */
public class Session {

    private long sUserId;

    private final Engine mEngine;

    private Page mLastActivity;

    private Page mLastFragment;

    private String mId;

    private static final AtomicLong sEventId = new AtomicLong(1000L);

    private volatile long mLastPlayTs;

    private int mPlayCount;

    private long mStartTs = -1;

    private volatile boolean mHadUi;

    private long mLastPauseTs;

    private int mSessionOfDay;

    private String mLastDay;

    /**
     * 第一次切换uuid前，最近的一次前台launch的session id
     */
    private volatile String mLastFgId;

    /**
     * 是否是从后台切换到前台
     */
    private volatile boolean resumeFromBackground = false;
    private volatile boolean hasNewSession = false;

    Session(Engine engine) {
        mEngine = engine;
    }

    public String getId() {
        return mId;
    }

    public String getLastFgId() {
        return mLastFgId;
    }

    public long getUserId() {
        return sUserId;
    }

    public void setUserId(long userId) {
        sUserId = userId;
    }

    synchronized Bundle getPlayBundle(final long currentTs, final long interval) {
        Bundle bundle = null;
        long lastPlayTs = mLastPlayTs;
        if (mEngine.getConfig().isPlayEnable()
                && isResume()
                && lastPlayTs > 0
                && currentTs - lastPlayTs > interval) {
            bundle = new Bundle();
            bundle.putInt("session_no", mSessionOfDay);
            bundle.putInt("send_times", ++mPlayCount);
            bundle.putLong("current_duration", (currentTs - lastPlayTs) / 1000);
            bundle.putString("session_start_time", BaseData.formatDateMS(mStartTs));
            mLastPlayTs = currentTs;
        }
        return bundle;
    }

    public boolean hadUi() {
        return mHadUi;
    }

    boolean isResume() {
        return hadUi() && mLastPauseTs == 0L;
    }

    /**
     * 启动一个Session
     *
     * @param data     当前处理的事件
     * @param saveList 要保存的数据列表
     * @param hasUi    是否是在前台（在init之后）
     * @return Launch
     */
    synchronized Launch startSession(
            final AppLogInstance appLogInstance,
            final BaseData data,
            final List<BaseData> saveList,
            final boolean hasUi) {
        final long ts = data instanceof TermTrigger ? -1 : data.ts;

        mId = UUID.randomUUID().toString();

        LogUtils.sendJsonFetcher(
                "session_start",
                new EventBus.DataFetcher() {
                    @Override
                    public Object fetch() {
                        JSONObject data = new JSONObject();
                        try {
                            data.put("appId", appLogInstance.getAppId());
                            data.put("sessionId", mId);
                            data.put("isBackground", !hasUi);
                            data.put("newLaunch", ts != -1);
                        } catch (Throwable ignored) {
                        }
                        return data;
                    }
                });

        if (hasUi && !mEngine.mUuidChanged && TextUtils.isEmpty(mLastFgId)) {
            mLastFgId = mId;
        }
        sEventId.set(1000L);
        mStartTs = ts;
        mHadUi = hasUi;
        mLastPauseTs = 0L;
        mLastPlayTs = 0L;

        if (hasUi) {
            final Calendar c = Calendar.getInstance();
            final String today =
                    ""
                            + c.get(Calendar.YEAR)
                            + c.get(Calendar.MONTH)
                            + c.get(Calendar.DAY_OF_MONTH);

            ConfigManager cm = mEngine.getConfig();

            if (TextUtils.isEmpty(mLastDay)) {
                mLastDay = cm.getLastDay();
                mSessionOfDay = cm.getSessionOrder();
            }
            if (!today.equals(mLastDay)) {
                mLastDay = today;
                mSessionOfDay = 1;
            } else {
                ++mSessionOfDay;
            }
            cm.setLastDay(today, mSessionOfDay);
            mPlayCount = 0;
            mLastPlayTs = data.ts;
        }

        Launch launch = null;

        if (ts != -1) {
            launch = new Launch();
            launch.setAppId(data.getAppId());
            launch.sid = mId;
            launch.mBg = !mHadUi;
            launch.eid = nextEventId();
            launch.setTs(mStartTs);
            launch.verName = mEngine.getDm().getVersionName();
            launch.verCode = mEngine.getDm().getVersionCode();
            launch.uid = sUserId;
            launch.uuid = mEngine.getDm().getUserUniqueId();
            launch.uuidType = mEngine.getDm().getUserUniqueIdType();
            launch.ssid = appLogInstance.getSsid();
            launch.abSdkVersion = appLogInstance.getAbSdkVersion();
            launch.isFirstTime = hasUi ? mEngine.getConfig().getIsFirstTimeLaunch() : 0;
            if (hasUi && launch.isFirstTime == 1) {
                mEngine.getConfig().setIsFirstTimeLaunch(0);
            }

            // 预置事件补充
            Page curPage = Navigator.getCurPage();
            if (null != curPage) {
                launch.pageKey = curPage.name;
                launch.pageTitle = curPage.title;
            }
            // 仅处理从后台进入前台后产生的Launch
            if (mHadUi && resumeFromBackground) {
                launch.resumeFromBackground = resumeFromBackground;
                // 从后台进入前台后，重置launch标志
                resumeFromBackground = false;
            }
            this.mEngine.getAppLog().getLogger().debug("fillSessionParams launch: " + launch);
            saveList.add(launch);
        }
        if (mEngine.getAppLog().getLaunchFrom() <= 0) {
            mEngine.getAppLog().setLaunchFrom(6);
        }

        appLogInstance.getLogger().debug("Start new session:{} with background:{}", mId, !mHadUi);
        return launch;
    }

    static boolean isResumeEvent(BaseData data) {
        if (data instanceof Page) {
            Page page = (Page) data;
            return page.isResumeEvent();
        }
        return false;
    }

    /**
     * 处理每一个事件，增加辅助信息，将需要存储的事件放到{@param saveList}中, 并记录是否切换了session
     *
     * @param data     data
     * @param saveList save
     */
    public void process(
            final AppLogInstance appLogInstance, BaseData data, List<BaseData> saveList) {
        if (mEngine.getConfig().isMainProcess()) {
            final boolean isResume = Session.isResumeEvent(data);
            boolean sessionChanged = true;
            if (mStartTs == -1) {
                // 全新启动，生成一个session
                startSession(appLogInstance, data, saveList, isResume);
            } else if (!mHadUi && isResume) {
                // 首次切换到前台，生成一个session
                startSession(appLogInstance, data, saveList, true);
            } else if (mLastPauseTs != 0L
                    && data.ts > mLastPauseTs + mEngine.getConfig().getSessionLife()) {
                // 只要是进入后台，就设置resumeFromBackground = true，进入前台且发送Launch事件后重置为false
                resumeFromBackground = true;

                // 有ui的session，在后台停留时间超过一定时间，则结束session。若非TermTrigger，则重启动一个新session
                startSession(appLogInstance, data, saveList, isResume);
            } else if (mStartTs > data.ts + 2 * 60 * 60 * 1000) {
                // 启动时间比事件生成时间晚，可能是调整时间了，重新生成一个session。
                startSession(appLogInstance, data, saveList, isResume);
            } else {
                sessionChanged = false;
            }

            fillSessionParams(appLogInstance, data);
            hasNewSession = sessionChanged;
        }
    }

    void addDataToList(BaseData data, List<BaseData> saveList,
                       final AppLogInstance appLogInstance) {
        final boolean isPage = data instanceof Page;
        if (isPage) {
            Page page = (Page) data;
            if (page.isResumeEvent()) {
                mLastPauseTs = 0L;

                // resume时候也需要上报，用来提升页面pv uv数据准确率
                saveList.add(data);

                if (TextUtils.isEmpty(page.last)) {
                    if (mLastFragment != null
                            && page.ts - mLastFragment.ts - mLastFragment.duration
                            < Navigator.DELAY_MULTY_PROC) {
                        page.last = mLastFragment.name;
                    } else if (mLastActivity != null
                            && page.ts - mLastActivity.ts - mLastActivity.duration
                            < Navigator.DELAY_MULTY_PROC) {
                        page.last = mLastActivity.name;
                    }
                }
            } else {
                Bundle playBundle = getPlayBundle(data.ts, 0L);
                if (null != appLogInstance && playBundle != null) {
                    appLogInstance.onEventV3(
                            "play_session", playBundle, AppLogInstance.BUSINESS_EVENT);
                }

                mLastPauseTs = page.ts;

                saveList.add(data);

                if (page.isActivity()) {
                    mLastActivity = page;
                } else {
                    mLastFragment = page;
                    mLastActivity = null;
                }
            }
        } else if (!(data instanceof TermTrigger)) {
            saveList.add(data);
        }
    }

    public void fillSessionParams(IAppLogInstance appLogInstance, BaseData data) {
        if (data != null) {
            DeviceManager device = mEngine.getDm();
            data.setAppId(appLogInstance.getAppId());
            data.uid = sUserId;
            data.uuid = device.getUserUniqueId();
            data.uuidType = device.getUserUniqueIdType();
            data.ssid = device.getSsid();
            data.sid = mId;
            data.eid = nextEventId();
            data.abSdkVersion = device.getAbSdkVersionAndMergeCustomVid(data.abSdkVersion);
            data.nt = NetworkUtils.getNetworkTypeFast(mEngine.getContext()).getValue();
            if (data instanceof EventV3
                    && mStartTs > 0
                    && Utils.equals(((EventV3) data).getEvent(), ExceptionHandler.CRASH_EVENT)
                    && null != data.properties) {
                try {
                    data.properties.put("$session_duration", System.currentTimeMillis() - mStartTs);
                } catch (Throwable ignored) {
                }
            }
            this.mEngine.getAppLog().getLogger().debug("fillSessionParams data: " + data);
        }
    }

    public static long nextEventId() {
        return sEventId.incrementAndGet();
    }

    public boolean hasNewSession() {
        boolean hasNewSession = this.hasNewSession;
        this.hasNewSession = false;
        return hasNewSession;
    }

    /**
     * 辅助事件，仅用来触发切后台的超时。
     */
    private static class TermTrigger extends Terminate {
        // nothing to do
    }

    private static TermTrigger mTermTrigger;

    static TermTrigger getTermTrigger() {
        if (mTermTrigger == null) {
            mTermTrigger = new TermTrigger();
        }
        mTermTrigger.setTs(0);
        return mTermTrigger;
    }
}
