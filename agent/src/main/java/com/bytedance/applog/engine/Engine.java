// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.engine;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.bytedance.applog.AppLogHelper;
import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.IPullAbTestConfigCallback;
import com.bytedance.applog.InitConfig;
import com.bytedance.applog.UriConfig;
import com.bytedance.applog.alink.ALinkManager;
import com.bytedance.applog.collector.Collector;
import com.bytedance.applog.collector.Navigator;
import com.bytedance.applog.concurrent.TTExecutors;
import com.bytedance.applog.filter.AbstractEventFilter;
import com.bytedance.applog.log.EventBus;
import com.bytedance.applog.log.LogUtils;
import com.bytedance.applog.manager.AppLogCache;
import com.bytedance.applog.manager.ConfigManager;
import com.bytedance.applog.manager.DeviceManager;
import com.bytedance.applog.network.RangersHttpTimeoutException;
import com.bytedance.applog.oneid.IDBindCallback;
import com.bytedance.applog.oneid.OneIDManager;
import com.bytedance.applog.profile.ProfileController;
import com.bytedance.applog.profile.UserProfileCallback;
import com.bytedance.applog.profile.UserProfileHelper;
import com.bytedance.applog.server.Api;
import com.bytedance.applog.store.BaseData;
import com.bytedance.applog.store.CustomEvent;
import com.bytedance.applog.store.DbStoreV2;
import com.bytedance.applog.store.EventV3;
import com.bytedance.applog.store.Launch;
import com.bytedance.applog.store.PackV2;
import com.bytedance.applog.store.Page;
import com.bytedance.applog.store.Profile;
import com.bytedance.applog.util.PrivateAgreement;
import com.bytedance.applog.util.ReflectUtils;
import com.bytedance.applog.util.Utils;

import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 上报引擎，用来管理{@link Session}，调度线程，驱动各个{@link BaseWorker}工作
 *
 * @author shiyanlong
 * @date 2019/1/16
 */
public class Engine implements Callback, Comparator<BaseData> {
    private static final int MSG_BG_START = 1;
    private static final int MSG_WORK_START = 2;
    private static final int MSG_PROCESS = 4;
    private static final int MSG_CHECK_WORKER = 6;
    private static final int MSG_TERM = 7;
    private static final int MSG_SAVE_REAL_TIME = 8;
    private static final int MSG_SEND_DOM = 9;
    private static final int MSG_PROCESS_CACHE = 10;
    private static final int MSG_SET_UUID = 12;
    private static final int MSG_CHECK_AB_CONFIG = 13;
    private static final int MSG_SEND_IMMEDIATELY = 14;
    private static final int MSG_RANGERS_EVENT_VERIFY = 15;
    private static final int MSG_SEND_RANGERS_EVENT_VERIFY = 16;
    private static final int MSG_PULL_ABCONFIG = 18;
    private static final long REAL_FAIL_INTERVAL = 60 * 1000;
    /**
     * 每累积INTERVAL_PROCESS个数据，强制process一次。
     */
    private static final int INTERVAL_PROCESS = 10;

    public static final long TIME_CHECK_INTERVAL = 5 * 1000;

    /**
     * 拉取ab实验配置的频控时间间隔
     */
    private long mPullAbTestConfigsThrottleMills = 10 * 1000L;
    private final AppLogInstance mAppLogInst;
    private final ConfigManager mConfig;
    private Configure mConfigure;
    private final ArrayList<BaseData> mDataList = new ArrayList<>(32);
    private volatile DbStoreV2 mDbStoreV2;
    private final DeviceManager mDevice;
    private volatile Handler mNetHandler;
    private Register mRegister;
    private Sender mSender;
    private volatile AbConfigure mAbConfigure;
    private final Session mSession;
    private UriConfig mUriConfig;
    private final Handler mWorkHandler;
    private volatile boolean mStarted;
    private BaseWorker mDomSender;
    private volatile RangersEventVerifyHeartBeater mRangersEventVerifyHeartBeater;
    private final CopyOnWriteArrayList<BaseWorker> mWorkers = new CopyOnWriteArrayList<>();
    public volatile boolean mUuidChanged;
    private volatile long mLastFlushTime;
    private final List<AbsDelayedTask> mDelayedTasks = new ArrayList<>();
    private volatile AbstractEventFilter mEventFilter;
    private volatile InitConfig.IpcDataChecker mIpcDataChecker;
    private final ProfileController profileController;
    private final ALinkManager aLinkManager;
    /** 缓存对象 */
    private final AppLogCache appLogCache;
    private long mRealFailTs;
    private volatile boolean isRealTimeEventEnabled = false;

    /**
     * 生命钩子
     */
    private final LifeHook lifeHook;

    private final OneIDManager oneIdManager;

    abstract static class AbsDelayedTask<T> {
        protected T mParam;

        AbsDelayedTask(T param) {
            mParam = param;
        }

        protected abstract void deferExecute();
    }

    class DelayedSetUuid extends AbsDelayedTask<String> {
        DelayedSetUuid(String param) {
            super(param);
        }

        @Override
        protected void deferExecute() {
            registerNewUuid(mParam);
        }
    }

    public AppLogInstance getAppLog() {
        return mAppLogInst;
    }

    public Session getSession() {
        return mSession;
    }

    public String getSessionId() {
        if (mSession != null) {
            return mSession.getId();
        }
        return null;
    }

    public Engine(
            final AppLogInstance app,
            final ConfigManager cm,
            final DeviceManager dm,
            final AppLogCache cache) {
        mAppLogInst = app;
        mConfig = cm;
        mDevice = dm;
        appLogCache = cache;
        mSession = new Session(this);
        lifeHook = new LifeHookImpl(this);

        HandlerThread ht = new HandlerThread("bd_tracker_w:" + app.getAppId());
        ht.start();
        mWorkHandler = new Handler(ht.getLooper(), this);
        aLinkManager = new ALinkManager(this);
        if (mConfig.isDeferredALinkEnable()) {
            mAppLogInst.addDataObserver(aLinkManager);
        }
        mDevice.setProviderHandler(mWorkHandler);
        profileController = new ProfileController(this);

        if (mConfig.isClearDidAndIid()) {
            mDevice.clearDidAndIid(mConfig.getClearKey());
        }

        if (mConfig.getInitConfig().getIpcDataChecker() != null && !mConfig.isMainProcess()) {
            this.mIpcDataChecker = mConfig.getInitConfig().getIpcDataChecker();
        }

        mWorkHandler.sendEmptyMessage(MSG_PROCESS_CACHE);

        if (mConfig.autoStart()) {
            startInner();
        }

        oneIdManager = new OneIDManager(this);
    }

    public LifeHook getLifeHook() {
        return lifeHook;
    }

    public Context getContext() {
        return getAppLog().getContext();
    }

    public void setPullAbTestConfigsThrottleMills(Long mills) {
        mPullAbTestConfigsThrottleMills = null != mills && mills > 0L ? mills : 0L;
    }

    public long getPullAbTestConfigsThrottleMills() {
        return mPullAbTestConfigsThrottleMills;
    }

    /** 初始化DB耗时较大，所以用懒加载方式 */
    public DbStoreV2 getDbStoreV2() {
        if (mDbStoreV2 == null) {
            synchronized (this) {
                DbStoreV2 db = mDbStoreV2;
                if (db == null) {
                    db = new DbStoreV2(this, getConfig().getInitConfig().getDbName());
                }
                mDbStoreV2 = db;
            }
        }
        return mDbStoreV2;
    }

    public DeviceManager getDm() {
        return mDevice;
    }

    public void setLanguageAndRegion(final String language, final String region) {
        boolean change = mDevice.setAppLanguage(language) | mDevice.setAppRegion(region);
        if (change) {
            workImmediately(mRegister);
            workImmediately(mConfigure);
        }
    }

    public void setUriConfig(final UriConfig config) {
        mUriConfig = config;
        workImmediately(mRegister);
    }

    @NonNull
    public UriConfig getUriConfig() {
        if (mUriConfig == null) {
            mUriConfig = getConfig().getInitConfig().getUriConfig();
        }
        return mUriConfig;
    }

    public void start() {
        if (!mStarted) {
            startInner();
        }
    }

    /**
     * SDK Start
     */
    private void startInner() {
        mStarted = true;
        mWorkHandler.sendEmptyMessage(MSG_BG_START);
    }

    /**
     * 立即发送日志
     */
    public void sendLogImmediately() {
        workImmediately(mSender);
    }

    @Override
    public boolean handleMessage(final Message msg) {
        switch (msg.what) {
            case MSG_BG_START:
                getAppLog().getLogger().info("AppLog is starting...");
                mConfig.setEnableBav(mConfig.isBavEnable());
                if (mDevice.load()) {
                    if (mConfig.isMainProcess()) {
                        HandlerThread nt =
                                new HandlerThread("bd_tracker_n:" + getAppLog().getAppId());
                        nt.start();
                        mNetHandler = new Handler(nt.getLooper(), this);
                        mNetHandler.sendEmptyMessage(MSG_WORK_START);
                        if (mDataList.size() > 0) {
                            mWorkHandler.removeMessages(MSG_PROCESS);
                            mWorkHandler.sendEmptyMessageDelayed(MSG_PROCESS, 1000);
                        }
                        PrivateAgreement.setAccepted(getAppLog().getContext());
                        getAppLog().getLogger().info("AppLog started on main process.");
                    } else {
                        getAppLog().getLogger().info("AppLog started on secondary process.");
                    }
                    if (!LogUtils.isDisabled()) {
                        LogUtils.sendJsonFetcher(
                                "start_end",
                                new EventBus.DataFetcher() {
                                    @Override
                                    public Object fetch() {
                                        JSONObject data = new JSONObject();
                                        try {
                                            data.put("appId", mAppLogInst.getAppId());
                                            data.put("isMainProcess", mConfig.isMainProcess());
                                        } catch (Throwable ignored) {

                                        }
                                        return data;
                                    }
                                });
                    }
                } else {
                    getAppLog()
                            .getLogger()
                            .info("AppLog is not ready, will try start again after 1 second...");
                    mWorkHandler.removeMessages(MSG_BG_START);
                    mWorkHandler.sendEmptyMessageDelayed(MSG_BG_START, 1000);
                }
                break;
            case MSG_WORK_START:
                mRegister = new Register(this);
                mWorkers.add(mRegister);

                // 开启事件上报后再初始化mSender，默认开启
                final boolean trackEventDisabled = null != mConfig.getInitConfig()
                        && !mConfig.getInitConfig().isTrackEventEnabled();
                if (!trackEventDisabled) {
                    mSender = new Sender(this);
                    mWorkers.add(mSender);
                    isRealTimeEventEnabled = true;
                }

                UriConfig uri = getUriConfig();
                if (!TextUtils.isEmpty(uri.getSettingUri())) {
                    mConfigure = new Configure(this);
                    mWorkers.add(mConfigure);
                }
                if (!TextUtils.isEmpty(uri.getProfileUri())) {
                    profileController.profileFlush();
                }

                checkAbConfiger();
                checkAppUpdate();

                mNetHandler.removeMessages(MSG_CHECK_WORKER);
                mNetHandler.sendEmptyMessage(MSG_CHECK_WORKER);
                break;
            case MSG_CHECK_WORKER:
                mNetHandler.removeMessages(MSG_CHECK_WORKER);

                long delay = TIME_CHECK_INTERVAL;
                if (!getAppLog().isPrivacyMode()
                        && (!mConfig.getInitConfig().isSilenceInBackground()
                        || mSession.isResume())) {
                    long nextTime = Long.MAX_VALUE;
                    for (BaseWorker worker : mWorkers) {
                        if (!worker.isStop()) {
                            long next = worker.checkToWork();
                            if (next < nextTime) {
                                nextTime = next;
                            }
                        }
                    }
                    delay = nextTime - System.currentTimeMillis();
                    if (delay > TIME_CHECK_INTERVAL) {
                        delay = TIME_CHECK_INTERVAL;
                    }
                }
                mNetHandler.sendEmptyMessageDelayed(MSG_CHECK_WORKER, delay);

                if (mDelayedTasks.size() > 0) {
                    synchronized (mDelayedTasks) {
                        for (AbsDelayedTask<?> delayedTask : mDelayedTasks) {
                            if (delayedTask != null) {
                                delayedTask.deferExecute();
                            }
                        }
                        mDelayedTasks.clear();
                    }
                }
                break;
            case MSG_SEND_DOM:
                BaseWorker worker = mDomSender;
                if (!worker.isStop()) {
                    long time = worker.checkToWork();
                    if (!worker.isStop()) {
                        mNetHandler.sendEmptyMessageDelayed(
                                MSG_SEND_DOM, time - System.currentTimeMillis());
                    }
                }
                break;
            case MSG_PROCESS_CACHE:
                synchronized (mDataList) {
                    appLogCache.dumpData(mDataList, getAppLog(), getSession());
                }
                process(appLogCache.getArray(), false);
                break;
            case MSG_PROCESS:
                process((String[]) msg.obj, false);
                break;
            case MSG_TERM:
                synchronized (mDataList) {
                    mDataList.add(Session.getTermTrigger());
                }
                process(null, false);
                break;
            case MSG_SAVE_REAL_TIME:
                ArrayList<BaseData> datas = (ArrayList<BaseData>) msg.obj;
                getDbStoreV2().saveAll(datas);
                break;
            case MSG_SET_UUID:
                String newUuid = null != msg.obj ? msg.obj.toString() : null;
                registerNewUuid(newUuid);
                break;
            case MSG_CHECK_AB_CONFIG:
                if (isAbEnabled()) {
                    if (mAbConfigure == null) {
                        mAbConfigure = new AbConfigure(this);
                    }
                    if (!mWorkers.contains(mAbConfigure)) {
                        mWorkers.add(mAbConfigure);
                    }
                    workImmediately(mAbConfigure);
                } else {
                    if (mAbConfigure != null) {
                        mAbConfigure.setStop(true);
                        mWorkers.remove(mAbConfigure);
                        mAbConfigure = null;
                    }
                    mDevice.clearAllAb();
                }
                break;
            case MSG_SEND_IMMEDIATELY:
                process(null, true);
                break;
            case MSG_RANGERS_EVENT_VERIFY:
                Object[] params = (Object[]) msg.obj;
                doRangersEventVerify((Boolean) params[0], (String) params[1]);
                break;
            case MSG_SEND_RANGERS_EVENT_VERIFY:
                BaseData data = (BaseData) msg.obj;
                sendToRangersEventVerify(data);
                break;
            case MSG_PULL_ABCONFIG:
                if (msg.obj instanceof IPullAbTestConfigCallback) {
                    doFetchAbConfig(msg.arg1, (IPullAbTestConfigCallback) msg.obj);
                } else {
                    workAbConfiger();
                }
                break;
            default:
                getAppLog().getLogger().error("Unknown handler message type");
        }
        return true;
    }

    public void userProfileExec(
            final int method, final JSONObject jsonObject, final UserProfileCallback callback) {
        if (mNetHandler != null) {
            UserProfileHelper.exec(this, method, jsonObject, callback, mNetHandler, false);
        }
    }

    public void checkAbConfiger() {
        mNetHandler.removeMessages(MSG_CHECK_AB_CONFIG);
        mNetHandler.sendEmptyMessage(MSG_CHECK_AB_CONFIG);
    }

    public void pullAbTestConfigs(int timeout, IPullAbTestConfigCallback callback) {
        mWorkHandler.sendMessage(
                mWorkHandler.obtainMessage(MSG_PULL_ABCONFIG, timeout, -1, callback));
    }

    /**
     * 开启热力图
     *
     * @param cookie cookie
     */
    public void startSimulator(String cookie) {
        if (mDomSender != null) {
            mDomSender.setStop(true);
        }
        Class<?> clz = ReflectUtils.getClassByName("com.bytedance.applog.picker.DomSender");
        if (null != clz) {
            try {
                // 为了解包,即base包不含picker相关,这里DomSender采用反射的方式创建
                Constructor<?> constructor = clz.getConstructor(Engine.class, String.class);
                mDomSender = (BaseWorker) constructor.newInstance(this, cookie);
                mNetHandler.sendMessage(mNetHandler.obtainMessage(MSG_SEND_DOM, mDomSender));
            } catch (Throwable e) {
                getAppLog().getLogger().error("Start simulator failed.", e);
            }
        }
    }

    /**
     * 处理埋点事件
     *
     * @param strings ipc收到子进程埋点数据
     * @param isFlush 是否强制立即发送
     */
    public void process(String[] strings, boolean isFlush) {
        // 埋点开关
        final boolean trackDisabled = null != mConfig.getInitConfig()
                && !mConfig.getInitConfig().isTrackEventEnabled();
        // 不上报埋点事件场景
        // 1. privacy mode
        // 2. trackEventEnabled = false
        if (getAppLog().isPrivacyMode() || trackDisabled) {
            return;
        }

        ArrayList<BaseData> datas;
        synchronized (mDataList) {
            datas = (ArrayList<BaseData>) mDataList.clone();
            mDataList.clear();
        }

        if (strings != null) {
            datas.ensureCapacity(datas.size() + strings.length);
            for (String string : strings) {
                BaseData data = BaseData.fromIpc(string);
                getSession().fillSessionParams(getAppLog(), data);
                datas.add(data);
            }
        }

        filterEvent(datas);
        boolean filterLoaded = mConfig.filterBlockAndWhite(datas, this);

        if (datas.size() > 0) {
            if (mConfig.isMainProcess()) {
                if (filterLoaded || datas.size() > 100) {
                    saveAndSend(datas);
                } else {
                    // 等待loadConfig。
                    for (BaseData d : datas) {
                        receive(d);
                    }
                }
            } else {
                Intent intent = new Intent(getAppLog().getContext(), Collector.class);
                final int size = datas.size();
                strings = new String[size];
                int bytes = 0;
                for (int i = 0; i < size; ++i) {
                    strings[i] = datas.get(i).toIpcJson().toString();
                    bytes += strings[i].length();
                }

                boolean needSend = true; // default open

                // if over 300k and need listen the checker, notify the checker
                if (bytes >= 300 * 1024 && mIpcDataChecker != null) {
                    try { // try catch for protecting the business exception
                        needSend = mIpcDataChecker.checkIpcData(strings);
                    } catch (Throwable e) {
                        getAppLog().getLogger().warn("check ipc data", e);
                    }
                }

                if (needSend) {
                    intent.putExtra(Collector.KEY_DATA, strings);
                    getAppLog().getContext().sendBroadcast(intent);
                }
            }
        }

        if (isFlush && mConfig.isMainProcess()) {
            long curTime = System.currentTimeMillis();
            if (Math.abs(curTime - mLastFlushTime) > 10 * 1000L) {
                mLastFlushTime = curTime;
                sendLogImmediately();
            }
        }
    }

    public ConfigManager getConfig() {
        return mConfig;
    }

    public boolean isEnableBav() {
        return getConfig().isEnableBavToB();
    }

    public boolean isResume() {
        return mSession.isResume();
    }

    public void setRangersEventVerifyEnable(boolean enable, String cookie) {
        mNetHandler.removeMessages(MSG_RANGERS_EVENT_VERIFY);
        Message msg =
                mNetHandler.obtainMessage(MSG_RANGERS_EVENT_VERIFY, new Object[]{enable, cookie});
        msg.sendToTarget();
    }

    /**
     * 发送埋点验证
     *
     * @param data BaseData
     */
    public void sendToRangersEventVerify(BaseData data) {
        if (null == mRangersEventVerifyHeartBeater) {
            return;
        }
        if (data instanceof EventV3
                || (data instanceof Page && isEnableBav())
                || data instanceof CustomEvent
                || data instanceof Profile) {
            JSONObject json = data.toPackJson();

            if (data instanceof Page) {
                if (!((Page) data).isResumeEvent()) {
                    return;
                }
                JSONObject params = json.optJSONObject(BaseData.COL_PARAM);
                if (null != params) {
                    try {
                        params.remove(Page.COL_DURATION);
                        json.put(BaseData.COL_PARAM, params);
                    } catch (Throwable ignored) {

                    }
                }
            }

            if (data instanceof CustomEvent) {
                if (!json.has("event")) {
                    try {
                        json.put(
                                "event",
                                json.optString("log_type", ((CustomEvent) data).getCategory()));
                    } catch (Throwable ignored) {

                    }
                }
            }

            getAppLog()
                    .getApi()
                    .sendToRangersEventVerify(json, mRangersEventVerifyHeartBeater.getCookie());
        }
    }

    /**
     * 统一扔到worker线程去处理；有一定延迟，以积攒做批量处理。
     *
     * @param data 待处理的数据
     */
    public void receive(BaseData data) {
        if (data.ts == 0) {
            getAppLog().getLogger().warn("Data ts is 0");
        }
        int size;
        synchronized (mDataList) {
            size = mDataList.size();
            mDataList.add(data);
            getSession().process(getAppLog(), data, mDataList);
        }
        boolean isPage = data instanceof Page;
        if (size % INTERVAL_PROCESS == 0 || isPage) {
            mWorkHandler.removeMessages(MSG_PROCESS);
            if (isPage || size != 0) {
                mWorkHandler.sendEmptyMessage(MSG_PROCESS);
            } else {
                mWorkHandler.sendEmptyMessageDelayed(MSG_PROCESS, 200);
            }
        }
    }

    /**
     * 统一扔到子线程去处理
     *
     * @param array 待处理的数据
     */
    public void receive(final String[] array) {
        mWorkHandler.removeMessages(MSG_PROCESS);
        mWorkHandler.obtainMessage(MSG_PROCESS, array).sendToTarget();
    }

    @Override
    public int compare(final BaseData left, final BaseData right) {
        long result = left.ts - right.ts;
        if (result < 0) {
            return -1;
        } else if (result > 0) {
            return 1;
        } else {
            return 0;
        }
    }

    private void saveAndSend(final List<BaseData> datas) {
        Collections.sort(datas, this);
        List<BaseData> saveData = new ArrayList<>(datas.size());
        boolean hasPage = false;
        boolean isResume = false;
        for (BaseData data : datas) {
            getSession().addDataToList(data, saveData, getAppLog());
            if (data instanceof Page) {
                hasPage = true;
                isResume = Session.isResumeEvent(data);
            }
            if (Looper.myLooper() == Looper.getMainLooper()) {
                if (null != mNetHandler) {
                    mNetHandler.obtainMessage(MSG_SEND_RANGERS_EVENT_VERIFY, data).sendToTarget();
                }
            } else {
                sendToRangersEventVerify(data);
            }

            LogUtils.sendObject("event_process", data);
        }

        filterRealTimeEvent(saveData);

        getDbStoreV2().saveAll(saveData);

        if (hasPage && null != mWorkHandler) {
            if (isResume) {
                mWorkHandler.removeMessages(MSG_TERM);
            } else {
                mWorkHandler.sendEmptyMessageDelayed(MSG_TERM, mConfig.getSessionLife());
            }
        }

        if (getSession().hasNewSession() || isTriggerEventSize()) {
            sendLogImmediately();
        }
    }

    private boolean isTriggerEventSize() {
        int configTriggerEventSize = mConfig.getEventSize();
        if (!mConfig.isValidEventSize(configTriggerEventSize)) {
            return false;
        }
        int size = mDbStoreV2.queryAllEventV3ByUuid(getAppLog().getAppId());
        return size >= configTriggerEventSize;
    }

    private void filterRealTimeEvent(final List<BaseData> saveData) {
        if (isRealTimeEventEnabled) {
            if (mDevice.isHeaderReady()
                    && System.currentTimeMillis() - mRealFailTs >= REAL_FAIL_INTERVAL) {
                final List<BaseData> realData = mConfig.filterRealTime(saveData);
                if (realData != null && realData.size() > 0) {
                    if (realData.size() <= PackV2.LIMIT_EVENT_COUNT) {
                        sendRealTimeEvent(realData);
                    } else {
                        int packCount = realData.size() / PackV2.LIMIT_EVENT_COUNT
                                + ((realData.size() % PackV2.LIMIT_EVENT_COUNT == 0) ? 0 : 1);
                        for (int i = 0; i < packCount; i++) {
                            int start = i * PackV2.LIMIT_EVENT_COUNT;
                            int end = Math.min(PackV2.LIMIT_EVENT_COUNT + start, realData.size());
                            sendRealTimeEvent(realData.subList(start, end));
                        }
                    }
                }
            }
        } else {
            getAppLog().getLogger().warn("can't not use realtime event");
        }
    }

    /**
     * 以 Pack 为单位发送数据
     *
     * @param realData 待发送的实时数据
     */
    private void sendRealTimeEvent(final List<BaseData> realData) {
        TTExecutors.getNormalExecutor().execute(new Runnable() {
            @Override
            public void run() {
                Sender sender = mSender;
                if (realData != null && realData.size() > 0) {
                    PackV2 pack = new PackV2();
                    pack.setHeader(mDevice.getHeader());
                    pack.setAppId(getAppLog().getAppId());
                    List<EventV3> v3List = new ArrayList<>();
                    for (BaseData item : realData) {
                        if (item instanceof EventV3) {
                            v3List.add((EventV3) item);
                        }
                    }
                    pack.setEventV3List(v3List);
                    pack.reloadSsidFromEvent();
                    pack.reloadUuidTypeFromEvent();
                    pack.writePackToData();
                    if (sender != null && sender.singlePackSend(pack)) {
                        mRealFailTs = 0;
                        getDbStoreV2().notifyEventV3Observer(realData);
                    } else {
                        mRealFailTs = System.currentTimeMillis();
                        mWorkHandler.obtainMessage(MSG_SAVE_REAL_TIME, realData).sendToTarget();
                    }
                }
            }
        });
    }

    private void workImmediately(BaseWorker baseWorker) {
        if (mNetHandler != null && baseWorker != null && !getAppLog().isPrivacyMode()) {
            baseWorker.setImmediately();
            if (Looper.myLooper() == mNetHandler.getLooper()) {
                baseWorker.checkToWork();
            } else {
                mNetHandler.removeMessages(MSG_CHECK_WORKER);
                mNetHandler.sendEmptyMessage(MSG_CHECK_WORKER);
            }
        }
    }

    private void doRangersEventVerify(boolean enable, String cookie) {
        if (mRangersEventVerifyHeartBeater != null) {
            mRangersEventVerifyHeartBeater.setStop(true);
            mWorkers.remove(mRangersEventVerifyHeartBeater);
            mRangersEventVerifyHeartBeater = null;
        }
        if (enable) {
            mRangersEventVerifyHeartBeater = new RangersEventVerifyHeartBeater(this, cookie);
            mWorkers.add(mRangersEventVerifyHeartBeater);
            mNetHandler.removeMessages(MSG_CHECK_WORKER);
            mNetHandler.sendEmptyMessage(MSG_CHECK_WORKER);
        }
    }

    /**
     * 切换用户uuid
     */
    public void setUserUniqueId(final String id, final String type) {
        String curUuid = mDevice.getUserUniqueId();
        String currUuidType = mDevice.getUserUniqueIdType();
        if (Utils.equals(id, curUuid) && Utils.equals(type, currUuidType)) {
            getAppLog().getLogger().debug("setUserUniqueId not change");
            return;
        }

        List<BaseData> dataList = new ArrayList<>();

        long curTime = System.currentTimeMillis();
        Page page = Navigator.getCurPage();

        boolean hasSession = Utils.isNotEmpty(mSession.getId());

        // 补充一个页面结束事件
        if (hasSession && page != null) {
            page = (Page) page.clone();
            page.setAppId(getAppLog().getAppId());
            long duration = curTime - page.ts;
            page.setTs(curTime);
            page.duration = duration >= 0 ? duration : 0;
            page.lastSession = mSession.getLastFgId();
            mSession.fillSessionParams(getAppLog(), page);
            dataList.add(page);
        }

        syncSetUuid(id, type);

        boolean hasCurrPage = true;
        if (page == null) {
            page = Navigator.getLastPage();
            hasCurrPage = false;
        }
        if (hasSession && page != null) {
            page = (Page) page.clone();
            page.setTs(curTime + 1);
            page.duration = -1L;
            Launch launch = mSession.startSession(getAppLog(), page, dataList, true);
            launch.lastSession = mSession.getLastFgId();
            if (hasCurrPage) {
                mSession.fillSessionParams(getAppLog(), page);
                dataList.add(page);
            }
        }

        for (BaseData baseData : dataList) {
            receive(baseData);
        }

        mWorkHandler.sendEmptyMessage(MSG_SEND_IMMEDIATELY);
    }

    /**
     * 同步设置uuid
     */
    private void syncSetUuid(final String id, final String type) {
        boolean isAnonymousUser = TextUtils.isEmpty(mDevice.getUserUniqueId());
        // 同步修改uuid和ssid
        mDevice.setUserUniqueId(id);
        mDevice.setUserUniqueIdType(type);
        mDevice.setSsid("");
        mDevice.removeHeaderInfo(ALinkManager.TR_WEB_SSID);
        if (getConfig().isClearABCacheOnUserChange() && !isAnonymousUser) {
            mDevice.setAbSdkVersion(null);
        }
        mUuidChanged = true;

        if (null != mNetHandler) {
            mNetHandler.sendMessage(mNetHandler.obtainMessage(MSG_SET_UUID, id));
        } else {
            synchronized (mDelayedTasks) {
                mDelayedTasks.add(new DelayedSetUuid(id));
            }
        }
    }

    /**
     * 注册新的用户ID
     */
    private void registerNewUuid(String uuid) {
        JSONObject newHeader = new JSONObject();
        Utils.copy(newHeader, mDevice.getHeader());
        try {
            if (null != mRegister && mRegister.doRegister(newHeader)) {
                if (Utils.isNotEmpty(uuid)) {
                    mConfig.setIsFirstTimeLaunch(1);
                }
            }
        } catch (Throwable e) {
            getAppLog().getLogger().error("Register new uuid:{} failed", e, uuid);
        }
    }

    /**
     * 使用header重新注册后获取ssid信息
     */
    public String getSsidByRegister(JSONObject header) {
        JSONObject newHeader = new JSONObject();
        Utils.copy(newHeader, header);
        JSONObject res = mRegister.invokeRegister(newHeader);
        if (null == res) {
            return null;
        }
        String ssid = res.optString(Api.KEY_SSID, "");
        if (Utils.isEmpty(ssid)) {
            return null;
        }
        return ssid;
    }

    /**
     * 获取ssid如果没有ssid的情况
     *
     * @param header Header信息（包含SSID)
     * @return true: 有ssid false: 没有ssid
     */
    public boolean fetchIfNoSsidInHeader(JSONObject header) {
        if (null == header) {
            return false;
        }
        boolean hasSsid = Utils.isNotEmpty(header.optString(Api.KEY_SSID, ""));
        if (hasSsid) {
            return true;
        }
        getAppLog().getLogger().debug("Register to get ssid by temp header...");
        try {
            String ssid = getSsidByRegister(header);
            if (Utils.isNotEmpty(ssid)) {
                getAppLog().getLogger().debug("Register to get ssid by header success.");
                header.put(Api.KEY_SSID, ssid);
                return true;
            }
        } catch (Throwable e) {
            getAppLog().getLogger().error("JSON handle failed", e);
        }
        return false;
    }

    public void workAbConfiger() {
        workImmediately(mAbConfigure);
    }

    public void setEventFilter(AbstractEventFilter eventFilter) {
        mEventFilter = eventFilter;
    }

    public void profileSet(JSONObject jsonObject) {
        if (jsonObject == null || jsonObject.length() == 0) {
            return;
        }
        profileController.profileSet(jsonObject);
    }

    public void profileSetOnce(JSONObject jsonObject) {
        if (jsonObject == null || jsonObject.length() == 0) {
            return;
        }
        profileController.profileSetOnce(jsonObject);
    }

    public void profileUnset(JSONObject jsonObject) {
        if (jsonObject == null || jsonObject.length() == 0) {
            return;
        }
        profileController.profileUnset(jsonObject);
    }

    public void profileIncrement(JSONObject jsonObject) {
        if (jsonObject == null || jsonObject.length() == 0) {
            return;
        }
        profileController.profileIncrement(jsonObject);
    }

    public void profileAppend(JSONObject jsonObject) {
        if (jsonObject == null || jsonObject.length() == 0) {
            return;
        }
        profileController.profileAppend(jsonObject);
    }

    public void onDeepLinked(final Uri uri) {
        aLinkManager.onDeepLinked(uri);
    }

    /**
     * 是否允许读取粘贴板
     *
     * @param enabled true: 允许
     */
    public void setClipboardEnabled(boolean enabled) {
        aLinkManager.setClipboardEnabled(enabled);
    }

    public String getDeepLinkUrl() {
        return aLinkManager.getDeepLinkUrl();
    }

    private void filterEvent(List<BaseData> eventList) {
        if (eventList.isEmpty()) {
            return;
        }
        boolean enableFilter = getConfig().getInitConfig().isEventFilterEnable();
        AbstractEventFilter eventFilter = mEventFilter;
        AbstractEventFilter eventFilterFromClient = getAppLog().getEventFilterByClient();
        if ((!enableFilter || eventFilter == null) && eventFilterFromClient == null) {
            return;
        }
        Iterator<BaseData> dataIterator = eventList.iterator();
        while (dataIterator.hasNext()) {
            BaseData data = dataIterator.next();
            if (data instanceof EventV3) {
                EventV3 eventV3 = (EventV3) data;
                String eventName = eventV3.getEvent();
                String eventParam = eventV3.getParam();
                if (eventFilterFromClient != null
                        && !eventFilterFromClient.filter(eventName, eventParam)
                        || eventFilter != null && !eventFilter.filter(eventName, eventParam)) {
                    dataIterator.remove();
                }
            }
        }
    }

    private void checkAppUpdate() {
        String spName =
                AppLogHelper.getInstanceSpName(getAppLog(), AbstractEventFilter.SP_FILTER_NAME);
        if (isVersionCodeOrChannelUpdate()) {
            if (mRegister != null) {
                mRegister.setImmediately();
            }
            if (mConfigure != null) {
                mConfigure.setImmediately();
            }
            if (getConfig().getInitConfig().isEventFilterEnable()) {
                setEventFilter(
                        AbstractEventFilter.parseFilterFromServer(
                                getAppLog().getContext(), spName, null));
            }
        } else {
            if (getConfig().getInitConfig().isEventFilterEnable()) {
                setEventFilter(
                        AbstractEventFilter.parseFilterFromLocal(getAppLog().getContext(), spName));
            }
        }
    }

    /**
     * 如需判断是否更新，请在设备注册完成之前判断，否则设备注册成功后会更新 lastChannel 以及 lastVersionCode
     *
     * @return App 是否升级版本或者更新过 Channel
     */
    public boolean isVersionCodeOrChannelUpdate() {
        return mDevice.getLastVersionCode() != mDevice.getVersionCode()
                || !TextUtils.equals(mConfig.getLastChannel(), mConfig.getChannel());
    }

    private boolean isAbEnabled() {
        return mConfig.isAbEnable()
                && !TextUtils.isEmpty(getUriConfig().getAbUri());
    }

    /**
     * 拉取AB实验
     *
     * @param timeout  超时时间
     * @param callback 回调
     */
    private void doFetchAbConfig(int timeout, IPullAbTestConfigCallback callback) {
        if (!isAbEnabled()) {
            mAppLogInst.getLogger().warn("ABTest is not enabled");
            return;
        }
        if (null == mAbConfigure) {
            mAbConfigure = new AbConfigure(this);
        }
        AbConfigure configure = mAbConfigure;
        try {
            JSONObject config = configure.fetchAbConfig(timeout);
            if (null != callback) callback.onRemoteConfig(config);
        } catch (RangersHttpTimeoutException e) {
            if (null != callback) callback.onTimeoutError();
        }
    }

    /**
     * 用户多口径，绑定 ID
     *
     * @param identities
     * @param callback
     */
    public void bind(Map<String, String> identities, IDBindCallback callback) {
        if (identities == null) {
            mAppLogInst.getLogger().warn("BindID identities is null");
            return;
        }
        oneIdManager.bind(identities, callback);
    }
}
