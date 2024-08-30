// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import static com.bytedance.applog.store.Page.EVENT_KEY;

import android.accounts.Account;
import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.bytedance.applog.alink.IALinkListener;
import com.bytedance.applog.bean.GpsLocationInfo;
import com.bytedance.applog.collector.Navigator;
import com.bytedance.applog.encryptor.CustomEncryptor;
import com.bytedance.applog.encryptor.IEncryptor;
import com.bytedance.applog.encryptor.IEncryptorType;
import com.bytedance.applog.engine.Engine;
import com.bytedance.applog.event.AutoTrackEventType;
import com.bytedance.applog.event.DurationEvent;
import com.bytedance.applog.event.EventBuilder;
import com.bytedance.applog.event.EventObserverImpl;
import com.bytedance.applog.event.IEventHandler;
import com.bytedance.applog.exception.AppCrashType;
import com.bytedance.applog.exception.ExceptionHandler;
import com.bytedance.applog.exposure.ViewExposureManager;
import com.bytedance.applog.filter.AbstractEventFilter;
import com.bytedance.applog.holder.DataObserverHolder;
import com.bytedance.applog.holder.EventObserverHolder;
import com.bytedance.applog.holder.SessionObserverHolder;
import com.bytedance.applog.log.ConsoleLogProcessor;
import com.bytedance.applog.log.CustomLogProcessor;
import com.bytedance.applog.log.EventBus;
import com.bytedance.applog.log.IAppLogLogger;
import com.bytedance.applog.log.LogProcessorHolder;
import com.bytedance.applog.log.LogUtils;
import com.bytedance.applog.log.LoggerImpl;
import com.bytedance.applog.manager.AppLogCache;
import com.bytedance.applog.manager.ConfigManager;
import com.bytedance.applog.manager.DeviceManager;
import com.bytedance.applog.manager.DeviceRegisterParameterFactory;
import com.bytedance.applog.network.DefaultClient;
import com.bytedance.applog.network.INetworkClient;
import com.bytedance.applog.oneid.IDBindCallback;
import com.bytedance.applog.profile.UserProfileCallback;
import com.bytedance.applog.profile.UserProfileHelper;
import com.bytedance.applog.server.Api;
import com.bytedance.applog.server.ApiParamsUtil;
import com.bytedance.applog.simulate.SimulateLaunchActivity;
import com.bytedance.applog.simulate.SimulateLoginTask;
import com.bytedance.applog.store.BaseData;
import com.bytedance.applog.store.Click;
import com.bytedance.applog.store.CustomEvent;
import com.bytedance.applog.store.EventV3;
import com.bytedance.applog.store.Page;
import com.bytedance.applog.util.Assert;
import com.bytedance.applog.util.InitValue;
import com.bytedance.applog.util.JsonUtils;
import com.bytedance.applog.util.PageUtils;
import com.bytedance.applog.util.ReflectUtils;
import com.bytedance.applog.util.TLog;
import com.bytedance.applog.util.Utils;
import com.bytedance.applog.util.Validator;
import com.bytedance.applog.util.ViewHelper;
import com.bytedance.applog.util.WidgetUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 统一的埋点实例实现类
 *
 * @author luodong.seu
 */
public final class AppLogInstance implements IAppLogInstance {
    private static final List<AppLogInstance> sInstances = new CopyOnWriteArrayList<>();
    private static final AtomicInteger sInstId = new AtomicInteger(0);
    public static final int DEFAULT_EVENT = 0;
    public static final int BUSINESS_EVENT = 1;

    private final ConcurrentHashMap<String, JSONObject> viewPropertiesMap =
            new ConcurrentHashMap<>();
    private final SessionObserverHolder sessionObserverHolder = new SessionObserverHolder();
    private final EventObserverHolder eventObserverHolder = new EventObserverHolder();
    private final DeviceRegisterParameterFactory deviceRegisterParameterFactory =
            new DeviceRegisterParameterFactory();
    private final AppLogCache cache = new AppLogCache();

    private final Set<Integer> ignoredPageHashCodes = new HashSet<>();
    private final Set<String> ignoredViewHasCodes = new HashSet<>();
    private final Set<Class<?>> ignoredViewTypes = new HashSet<>();
    private final Map<String, DurationEvent> durationEventMap = new ConcurrentHashMap<>();

    private final ApiParamsUtil apiParamsUtil;
    private final Api api;

    private int sLaunchFrom = 0;
    private String appId = "";
    private volatile Application mApp = null;
    private volatile IAppContext mAppContext = null;
    private volatile ConfigManager mConfig;
    private volatile DeviceManager mDevice;
    private volatile Engine mEngine;
    private volatile Navigator mNav;
    private volatile ViewExposureManager viewExposureManager;
    private volatile INetworkClient defaultNetworkClient;

    private volatile boolean mStarted = false;
    private volatile AbstractEventFilter sEventFilterFromClient;
    private volatile boolean sPrivacyMode = false;
    private DataObserverHolder dataObserverHolder;
    private IALinkListener aLinkListener;

    /**
     * GPS位置信息
     */
    private volatile GpsLocationInfo gpsLocation;

    /**
     * 埋点事件处理器
     */
    private IEventHandler eventHandler;

    /**
     * 日志
     */
    private final IAppLogLogger logger;

    /**
     * 是否压缩加密，仅debug版有效
     */
    private volatile boolean sEncryptAndCompress = true;

    /**
     * 最后一次拉取ab实验的时间
     */
    private long mLastPullAbTestConfigsTime = 0L;

    /**
     * 是否处于debug状态
     */
    private volatile boolean mDebugMode = false;

    /**
     * 初始化之前的uuid
     */
    private final InitValue<String> postInitUserUniqueId = new InitValue<String>();

    /**
     * 初始化之前的uuidtype
     */
    private final InitValue<String> postInitUserUniqueIdType = new InitValue<String>();

    public AppLogInstance() {
        sInstId.incrementAndGet();

        // 初始化对象
        logger = new LoggerImpl();
        apiParamsUtil = new ApiParamsUtil(this);
        api = new Api(this);

        sInstances.add(this);
    }

    public AppLogInstance(@NonNull Context context, @NonNull InitConfig config) {
        this();
        init(context, config);
    }

    public AppLogInstance(@NonNull Context context, @NonNull InitConfig config, Activity activity) {
        this();
        init(context, config, activity);
    }

    @Override
    public void init(@NonNull Context context, @NonNull final InitConfig config) {
        synchronized (AppLogInstance.class) {
            long startTime = SystemClock.elapsedRealtime();
            if (Utils.isEmpty(config.getAid())) {
                Log.e(ConsoleLogProcessor.TAG, "Init failed. App id must not be empty!");
                return;
            }
            if (Utils.isEmpty(config.getChannel())) {
                Log.e(ConsoleLogProcessor.TAG, "Channel must not be empty!");
                return;
            }
            if (AppLogHelper.hasInstanceByAppId(config.getAid())) {
                Log.e(
                        ConsoleLogProcessor.TAG,
                        "The app id: " + config.getAid() + " has initialized already");
                return;
            }

            // 初始化logger appid
            getLogger().setAppId(config.getAid());

            appId = config.getAid();
            mApp = (Application) context.getApplicationContext();

            // 默认日志处理器
            if (config.isLogEnable()) {
                if (null != config.getLogger()) {
                    LogProcessorHolder.setProcessor(
                            appId, new CustomLogProcessor(config.getLogger()));
                } else {
                    LogProcessorHolder.setProcessor(appId, new ConsoleLogProcessor(this));
                }
            }

            getLogger().info("AppLog init begin...");

            // 多实例的SharedPreferenceName
            if (TextUtils.isEmpty(config.getSpName())) {
                config.setSpName(AppLogHelper.getInstanceSpName(this, ConfigManager.SP_FILE));
            }

            mConfig = new ConfigManager(this, mApp, config);
            mDevice = new DeviceManager(this, mApp, mConfig);
            postSetUuidAfterDm();

            mEngine = new Engine(this, mConfig, mDevice, cache);

            // 通知初始化
            sendConfig2DevTools(config);

            // 多实例下仅第一个生效
            mNav = Navigator.registerGlobalListener(mApp);

            // View 曝光事件管理器
            viewExposureManager = new ViewExposureManager(this);

            //  crash异常采集
            //  监控逻辑调整为主动开启崩溃采集
            if (AppCrashType.hasJavaCrashType(config.getTrackCrashType())) {
                ExceptionHandler.init();
            }

            sLaunchFrom = 1;
            mStarted = config.autoStart();

            // 通知初始化完成
            LogUtils.sendString("init_end", appId);

            getLogger().info("AppLog init end");

            handleAfterInit(startTime);
        }
    }

    @Override
    public void init(@NonNull Context context, @NonNull InitConfig config, Activity activity) {
        init(context, config);
        if (mNav != null && activity != null) {
            mNav.onActivityCreated(activity, null);
            mNav.onActivityResumed(activity);
        }
    }

    public IAppLogLogger getLogger() {
        return logger;
    }

    @Override
    public String toString() {
        return "AppLogInstance{id:" + sInstId.get() + ";appId:" + appId + "}@" + hashCode();
    }

    public static List<AppLogInstance> getAllInstances() {
        return sInstances;
    }

    @NonNull
    @Override
    public String getAppId() {
        return appId;
    }

    @Override
    public void receive(final BaseData data) {
        if (null == data) {
            return;
        }

        data.setAppId(getAppId());

        if (null == mEngine) {
            cache.cache(data);
        } else {
            mEngine.receive(data);
        }

        LogUtils.sendObject("event_receive", data);
    }

    @Override
    public void receive(String[] data) {
        if (null == data || data.length == 0) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (String d : data) {
            sb.append(d);
            sb.append(",");
        }

        //        TLog.d(() -> toString() + " received data: [" + sb.toString() + "]");

        if (null == mEngine) {
            cache.cache(data);
        } else {
            mEngine.receive(data);
        }
    }

    @Override
    public void setAppContext(@NonNull IAppContext appContext) {
        mAppContext = appContext;
    }

    @Override
    public IAppContext getAppContext() {
        return mAppContext;
    }

    @Override
    public Context getContext() {
        return mApp;
    }

    @Override
    public void start() {
        if (breakIfEngineIsNull("start")) {
            return;
        }
        if (!mStarted) {
            mStarted = true;
            mEngine.start();
        }
    }

    @Override
    public boolean hasStarted() {
        return mStarted;
    }

    @Override
    public InitConfig getInitConfig() {
        return mConfig != null ? mConfig.getInitConfig() : null;
    }

    @Override
    public boolean isBavEnabled() {
        return null != mEngine && mEngine.isEnableBav();
    }

    @WorkerThread
    @Override
    public void flush() {
        if (breakIfEngineIsNull("flush")) {
            return;
        }
        long s = SystemClock.elapsedRealtime();
        mEngine.process(null, true);
    }

    @Override
    public boolean isH5BridgeEnable() {
        return getInitConfig() != null && getInitConfig().isH5BridgeEnable();
    }

    @Override
    public boolean isH5CollectEnable() {
        return getInitConfig() != null && getInitConfig().isH5CollectEnable();
    }

    @Override
    public void setUserID(long id) {
        if (breakIfEngineIsNull("setUserID")) {
            return;
        }
        mEngine.getSession().setUserId(id);
    }

    @Override
    public void setAppLanguageAndRegion(@NonNull String language, @NonNull String region) {
        if (breakIfEngineIsNull("setAppLanguageAndRegion")) {
            return;
        }
        mEngine.setLanguageAndRegion(language, region);
    }

    @Override
    public void setGoogleAid(@NonNull String gaid) {
        if (breakIfDeviceIsNull("setGoogleAid")) {
            return;
        }
        mDevice.setGoogleAid(gaid);
    }

    @Override
    public String addNetCommonParams(Context context, String url, boolean isApi, Level level) {
        return apiParamsUtil.appendNetParams(
                mDevice != null ? mDevice.getHeader() : null, url, isApi, level);
    }

    @Override
    public void putCommonParams(
            Context context, Map<String, String> params, boolean isApi, Level level) {
        apiParamsUtil.appendParamsToMap(
                mDevice != null ? mDevice.getHeader() : null, isApi, params, level);
    }

    @Override
    public void setUserUniqueID(@Nullable final String id) {
        if (null == mDevice) {
            postInitUserUniqueId.setValue(id);
            getLogger().debug("cache uuid before init id -> " + id);
            return;
        }
        setUserUniqueID(id, mDevice.getUserUniqueIdType());
    }

    @Override
    public void setUserUniqueID(@Nullable final String id, @Nullable final String type) {
        if (null == mDevice) {
            postInitUserUniqueId.setValue(id);
            getLogger().debug("cache uuid before init id -> " + id);
            postInitUserUniqueIdType.setValue(type);
            getLogger().debug("cache uuid before init type -> " + type);
            return;
        }
        long s = SystemClock.elapsedRealtime();
        mEngine.setUserUniqueId(id, type);
    }

    @Override
    public void setExtraParams(IExtraParams iExtraParams) {
        apiParamsUtil.setsExtraParams(iExtraParams);
    }

    @Override
    public void setTouchPoint(@NonNull String touchPoint) {
        setHeaderInfo(Api.KEY_TOUCH_POINT, touchPoint);
    }

    @Override
    public void setHeaderInfo(HashMap<String, Object> custom) {
        if (breakIfDeviceIsNull("setHeaderInfo")) {
            return;
        }
        Validator.testCustomHeaders(getLogger(), custom);
        mDevice.setCustom(custom);
    }

    @Override
    public void setHeaderInfo(String key, Object value) {
        if (breakIfDeviceIsNull("setHeaderInfo")) {
            return;
        }
        if (!TextUtils.isEmpty(key)) {
            HashMap<String, Object> map = new HashMap<>();
            map.put(key, value);
            Validator.testCustomHeaders(getLogger(), map);
            mDevice.setCustom(map);
        }
    }

    @Override
    public void removeHeaderInfo(String key) {
        if (breakIfDeviceIsNull("removeHeaderInfo")) {
            return;
        }
        mDevice.removeHeaderInfo(key);
    }

    @Override
    public void setExternalAbVersion(@NonNull String version) {
        if (breakIfDeviceIsNull("setExternalAbVersion")) {
            return;
        }
        mDevice.setExternalAbVersion(version);
    }

    @Nullable
    @Override
    public String getExternalAbVersion() {
        if (breakIfDeviceIsNull("setExternalAbVersion")) {
            return null;
        }
        return mConfig.getExternalAbVersion();
    }

    @NonNull
    @Override
    public String getAbSdkVersion() {
        if (breakIfDeviceIsNull("getAbSdkVersion")) {
            return "";
        }
        return mDevice.getAbSdkVersion();
    }

    @Nullable
    @Override
    public <T> T getAbConfig(String key, T defaultValue) {
        if (breakIfDeviceIsNull("getAbConfig")) {
            return null;
        }
        long s = SystemClock.elapsedRealtime();
        T value = mDevice.getAbConfig(key, defaultValue);
        return value;
    }

    @Override
    public void pullAbTestConfigs() {
        pullAbTestConfigs(-1, null);
    }

    @Override
    public void pullAbTestConfigs(final int timeout, final IPullAbTestConfigCallback callback) {
        assert null != mEngine : "Please initialize first";
        long s = SystemClock.elapsedRealtime();
        long curTime = System.currentTimeMillis();
        long remainingTime =
                mEngine.getPullAbTestConfigsThrottleMills()
                        - Math.abs(curTime - mLastPullAbTestConfigsTime);
        if (remainingTime < 0) {
            mLastPullAbTestConfigsTime = curTime;
            mEngine.pullAbTestConfigs(timeout, callback);
        } else {
            if (null != callback) {
                callback.onThrottle(remainingTime);
            } else {
                getLogger().warn("Pull ABTest config too frequently");
            }
        }
    }

    @Override
    public void setPullAbTestConfigsThrottleMills(Long mills) {
        assert null != mEngine : "Please initialize first";
        mEngine.setPullAbTestConfigsThrottleMills(mills);
    }

    @Override
    public void clearAbTestConfigsCache() {
        assert null != mDevice : "Please initialize first";
        mDevice.clearAllAb();
    }

    @Deprecated
    @Override
    public String getAid() {
        return getAppId();
    }

    @Override
    public <T> T getHeaderValue(String key, T fallbackValue, Class<T> tClass) {
        if (breakIfDeviceIsNull("getHeaderValue")) {
            return null;
        }
        return mDevice.getHeaderValue(key, fallbackValue, tClass);
    }

    @Override
    public void setTracerData(JSONObject tracerData) {
        if (breakIfDeviceIsNull("setTracerData")) {
            return;
        }
        mDevice.setTracerData(tracerData);
    }

    @Override
    public void setUserAgent(@NonNull String ua) {
        if (breakIfDeviceIsNull("setUserAgent")) {
            return;
        }
        mDevice.setUserAgent(ua);
    }

    @Override
    public void onEventV3(@NonNull String event) {
        onEventV3(event, (JSONObject) null);
    }

    @Override
    public void onEventV3(@NonNull String event, @Nullable JSONObject params) {
        onEventV3(event, params, DEFAULT_EVENT);
    }

    @Override
    public void onEventV3(
            @NonNull final String event, @Nullable final JSONObject params, int eventType) {
        if (TextUtils.isEmpty(event)) {
            getLogger().error("event name is empty");
            return;
        }

        getLogger()
                .debug(
                        Arrays.asList("customEvent", "eventV3"),
                        "event:{} type:{} params:{} ",
                        event,
                        eventType,
                        null != params ? params.toString() : null);

        // 参数校验
        Validator.testEvent(getLogger(), event, params);

        receive(
                new EventV3(
                        appId, event, false, params != null ? params.toString() : null, eventType));
    }

    @Override
    public void onEventV3(@NonNull String event, @Nullable Bundle params, int eventType) {
        JSONObject jsonParams = null;
        try {
            if (params != null && !params.isEmpty()) {
                jsonParams = new JSONObject();
                Set<String> keys = params.keySet();
                for (String key : keys) {
                    jsonParams.put(key, params.get(key));
                }
            }
        } catch (Throwable t) {
            getLogger().error("Parse event params failed", t);
        }
        onEventV3(event, jsonParams, eventType);
    }

    @Override
    public void onEventV3(@NonNull String event, @Nullable Bundle params) {
        onEventV3(event, params, DEFAULT_EVENT);
    }

    @Override
    public EventBuilder newEvent(@NonNull String event) {
        return new EventBuilder(this).setEvent(event);
    }

    @Override
    public void onMiscEvent(@NonNull final String logType, @Nullable final JSONObject params) {
        if (TextUtils.isEmpty(logType) || params == null || params.length() <= 0) {
            getLogger().warn("call onMiscEvent with invalid params");
            return;
        }

        getLogger()
                .debug(
                        Arrays.asList("customEvent", "miscEvent"),
                        "logType:{} params:{} ",
                        logType,
                        params.toString());

        try {
            params.put("log_type", logType);
            CustomEvent cv = new CustomEvent(Api.KEY_LOG_DATA, params);
            receive(cv);
        } catch (Throwable e) {
            getLogger().error("call onMiscEvent error", e);
        }
    }

    @Override
    public void setEncryptAndCompress(final boolean enable) {
        sEncryptAndCompress = enable;

        if (Utils.isNotEmpty(appId) && !LogUtils.isDisabled()) {
            LogUtils.sendJsonFetcher(
                    "update_config",
                    new EventBus.DataFetcher() {
                        @Override
                        public Object fetch() {
                            JSONObject data = new JSONObject();
                            JSONObject config = new JSONObject();
                            try {
                                data.put("appId", appId);
                                config.put("接口加密开关", enable);
                                data.put("config", config);
                            } catch (Throwable ignored) {
                            }
                            return data;
                        }
                    });
        }
    }

    @Override
    public boolean getEncryptAndCompress() {
        return sEncryptAndCompress;
    }

    @NonNull
    @Override
    public String getDid() {
        if (breakIfDeviceIsNull("getDid")) {
            return "";
        }
        String bdDid = mDevice.getBdDid();
        if (!TextUtils.isEmpty(bdDid)) {
            return bdDid;
        } else {
            return mDevice.getDid();
        }
    }

    @NonNull
    @Override
    public String getUdid() {
        if (breakIfDeviceIsNull("getUdid")) {
            return "";
        }
        return mDevice.getUdid();
    }

    @Override
    public void addSessionHook(ISessionObserver hook) {
        sessionObserverHolder.addSessionHook(hook);
    }

    @Override
    public void removeSessionHook(ISessionObserver hook) {
        sessionObserverHolder.removeSessionHook(hook);
    }

    @Override
    public void addEventObserver(IEventObserver iEventObserver) {
        eventObserverHolder.addEventObserver(EventObserverImpl.EventFactory
                .creteEventObserver(iEventObserver, null));
    }

    @Override
    public void removeEventObserver(IEventObserver iEventObserver) {
        eventObserverHolder.removeEventObserver(EventObserverImpl.EventFactory
                .creteEventObserver(iEventObserver, null));
    }

    @Override
    public void addEventObserver(IEventObserver iEventObserver,
                                 IPresetEventObserver iPresetEventObserver) {
        eventObserverHolder.addEventObserver(EventObserverImpl.EventFactory
                .creteEventObserver(iEventObserver, iPresetEventObserver));
    }

    @Override
    public void removeEventObserver(IEventObserver iEventObserver,
                                    IPresetEventObserver iPresetEventObserver) {
        eventObserverHolder.removeEventObserver(EventObserverImpl.EventFactory
                .creteEventObserver(iEventObserver, iPresetEventObserver));
    }

    @Override
    public void setAccount(Account account) {
        if (breakIfDeviceIsNull("setAccount")) {
            return;
        }
        mDevice.setAccount(account);
    }

    @NonNull
    @Override
    public String getIid() {
        if (breakIfDeviceIsNull("getIid")) {
            return "";
        }
        return mDevice.getIid();
    }

    @NonNull
    @Override
    public String getSsid() {
        if (breakIfDeviceIsNull("getSsid")) {
            return "";
        }
        return mDevice.getSsid();
    }

    @NonNull
    @Override
    public String getUserUniqueID() {
        if (breakIfDeviceIsNull("getUserUniqueID")) {
            return "";
        }
        return mDevice.getUserUniqueId();
    }

    @Nullable
    @Override
    public String getUserID() {
        if (breakIfEngineIsNull("getUserID")) {
            return null;
        }
        return String.valueOf(mEngine.getSession().getUserId());
    }

    @NonNull
    @Override
    public String getClientUdid() {
        if (breakIfDeviceIsNull("getClientUdid")) {
            return "";
        }
        return mDevice.getClientUdid();
    }

    @NonNull
    @Override
    public String getOpenUdid() {
        if (breakIfDeviceIsNull("getOpenUdid")) {
            return "";
        }
        return mDevice.getOpenUdid();
    }

    @Override
    public void setUriRuntime(UriConfig config) {
        if (breakIfEngineIsNull("setUriRuntime")) {
            return;
        }
        mEngine.setUriConfig(config);
    }

    @Nullable
    @Override
    public UriConfig getUriRuntime() {
        if (breakIfEngineIsNull("getUriRuntime")) {
            return null;
        }
        return mEngine.getUriConfig();
    }

    @Override
    public void getSsidGroup(Map<String, String> map) {
        String did = getDid();
        if (!TextUtils.isEmpty(did)) {
            map.put(Api.KEY_DEVICE_ID, did);
        }
        String iid = getIid();
        if (!TextUtils.isEmpty(iid)) {
            map.put(Api.KEY_INSTALL_ID, iid);
        }
        String openUdId = getOpenUdid();
        if (!TextUtils.isEmpty(openUdId)) {
            map.put(Api.KEY_OPEN_UDID, openUdId);
        }
        String clientUdid = getClientUdid();
        if (!TextUtils.isEmpty(clientUdid)) {
            map.put(Api.KEY_C_UDID, clientUdid);
        }
    }

    @Override
    public synchronized void addDataObserver(IDataObserver listener) {
        if (null == dataObserverHolder) {
            dataObserverHolder = new DataObserverHolder();
        }
        dataObserverHolder.addDataObserver(listener);
    }

    @Override
    public void removeDataObserver(IDataObserver listener) {
        if (null != dataObserverHolder) {
            dataObserverHolder.removeDataObserver(listener);
        }
    }

    @Override
    public void removeAllDataObserver() {
        if (null != dataObserverHolder) {
            dataObserverHolder.removeAllDataObserver();
        }
    }

    @NonNull
    @Override
    public INetworkClient getNetClient() {
        // 如果有defaultNetworkClient，直接返回
        if (null != defaultNetworkClient) {
            return defaultNetworkClient;
        }

        if (null != getInitConfig() && null != getInitConfig().getNetworkClient()) {
            return getInitConfig().getNetworkClient();
        }

        // 只有initConfig中没有配置networkClient后才会初始化defaultNetworkClient
        // 加锁防止初始化多次
        synchronized (this) {
            if (null == defaultNetworkClient) {
                defaultNetworkClient = new DefaultClient(api);
            }
        }
        return defaultNetworkClient;
    }

    @Nullable
    @Override
    public JSONObject getHeader() {
        if (breakIfDeviceIsNull("getHeader")) {
            return null;
        }
        return mDevice.getHeader();
    }

    @Override
    public void setAppTrack(JSONObject appTrackJson) {
        if (appTrackJson == null) {
            // 对齐内部版，只判定null，估计""可以清理之前的内容
            return;
        }
        if (breakIfDeviceIsNull("setAppTrack")) {
            return;
        }
        mDevice.setAppTrackJson(appTrackJson);
    }

    @Override
    public boolean isNewUser() {
        if (breakIfDeviceIsNull("isNewUser")) {
            return false;
        }
        return mDevice.isNewUser();
    }

    @Override
    public void onResume(@NonNull Context context) {
        if (context instanceof Activity) {
            onActivityResumed((Activity) context, context.hashCode());
        }
    }

    @Override
    public void onPause(@NonNull Context context) {
        if (context instanceof Activity) {
            onActivityPause();
        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity, int hashCode) {
        if (mNav != null) {
            mNav.onActivityResumed(activity, hashCode);
        }
    }

    @Override
    public void onActivityPause() {
        if (mNav != null) {
            mNav.onActivityPaused(null);
        }
    }

    @Override
    public void userProfileSetOnce(JSONObject jsonObject, UserProfileCallback callback) {
        if (breakIfEngineIsNull("userProfileSetOnce")) {
            return;
        }
        mEngine.userProfileExec(UserProfileHelper.METHOD_SET, jsonObject, callback);
    }

    @Override
    public void userProfileSync(JSONObject jsonObject, UserProfileCallback callback) {
        if (breakIfEngineIsNull("userProfileSync")) {
            return;
        }
        mEngine.userProfileExec(UserProfileHelper.METHOD_SYNC, jsonObject, callback);
    }

    @Override
    public void startSimulator(@NonNull String cookie) {
        if (breakIfEngineIsNull("startSimulator")) {
            return;
        }
        mEngine.startSimulator(cookie);
    }

    @Override
    public void setRangersEventVerifyEnable(boolean enable, String cookie) {
        if (breakIfEngineIsNull("setRangersEventVerifyEnable")) {
            return;
        }
        mEngine.setRangersEventVerifyEnable(enable, cookie);
    }

    @Override
    public void profileSet(JSONObject jsonObject) {
        if (breakIfEngineIsNull("profileSet")) {
            return;
        }
        if (jsonObject == null || jsonObject.length() == 0) {
            return;
        }
        Validator.testProfileParams(getLogger(), jsonObject);
        mEngine.profileSet(jsonObject);
    }

    @Override
    public void profileSetOnce(JSONObject jsonObject) {
        if (breakIfEngineIsNull("profileSetOnce")) {
            return;
        }
        if (jsonObject == null || jsonObject.length() == 0) {
            return;
        }
        Validator.testProfileParams(getLogger(), jsonObject);
        mEngine.profileSetOnce(jsonObject);
    }

    @Override
    public void profileUnset(String key) {
        if (breakIfEngineIsNull("profileUnset")) {
            return;
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(key, "");
        } catch (Throwable e) {
            getLogger().error("JSON handle failed", e);
        }
        Validator.testProfileParams(getLogger(), jsonObject);
        mEngine.profileUnset(jsonObject);
    }

    @Override
    public void profileIncrement(JSONObject jsonObject) {
        if (breakIfEngineIsNull("profileIncrement")) {
            return;
        }
        if (jsonObject == null || jsonObject.length() == 0) {
            return;
        }
        try {
            if (!JsonUtils.paramValueCheck(jsonObject, new Class[]{Integer.class}, null)) {
                getLogger().warn("only support Int param");
                return;
            }
        } catch (Throwable e) {
            getLogger().error("JSON handle failed", e);
        }
        Validator.testProfileParams(getLogger(), jsonObject);
        mEngine.profileIncrement(jsonObject);
    }

    @Override
    public void profileAppend(JSONObject jsonObject) {
        if (breakIfEngineIsNull("profileAppend")) {
            return;
        }
        if (jsonObject == null || jsonObject.length() == 0) {
            return;
        }
        try {
            if (!JsonUtils.paramValueCheck(
                    jsonObject,
                    new Class[]{String.class, Integer.class},
                    new Class[]{String.class})) {
                getLogger().warn("only support String、Int、String Array！");
                return;
            }
        } catch (Throwable e) {
            getLogger().error("JSON handle failed", e);
        }
        Validator.testProfileParams(getLogger(), jsonObject);
        mEngine.profileAppend(jsonObject);
    }

    /**
     * 用户多口径，绑定 ID
     *
     * @param identities
     * @param callback
     */
    @Override
    public void bind(Map<String, String> identities, IDBindCallback callback) {
        if (breakIfEngineIsNull("bind")) {
            return;
        }
        mEngine.bind(identities, callback);
    }

    @Override
    public void setEventFilterByClient(List<String> eventList, boolean isBlock) {
        sEventFilterFromClient = AbstractEventFilter.parseFilterFromClient(eventList, isBlock);
    }

    @Override
    public AbstractEventFilter getEventFilterByClient() {
        return sEventFilterFromClient;
    }

    @Override
    public Map<String, String> getRequestHeader() {
        if (mConfig == null) {
            return Collections.emptyMap();
        }
        SharedPreferences sp = mConfig.getStatSp();
        String deviceToken = sp.getString(Api.KEY_DEVICE_TOKEN, "");
        Map<String, String> headerExtra = new HashMap<>();
        if (deviceToken == null) {
            deviceToken = "";
        }
        headerExtra.put("x-tt-dt", deviceToken);
        return headerExtra;
    }

    @NonNull
    @Override
    public String getSessionId() {
        return null != mEngine ? mEngine.getSessionId() : "";
    }

    @Override
    public void setALinkListener(IALinkListener linkListener) {
        aLinkListener = linkListener;
    }

    @Override
    public IALinkListener getALinkListener() {
        return aLinkListener;
    }

    @Override
    public void setClipboardEnabled(final boolean enabled) {
        if (breakIfEngineIsNull("setClipboardEnabled")) {
            return;
        }
        mEngine.setClipboardEnabled(enabled);
        if (!LogUtils.isDisabled()) {
            LogUtils.sendJsonFetcher(
                    "update_config",
                    new EventBus.DataFetcher() {
                        @Override
                        public Object fetch() {
                            JSONObject data = new JSONObject();
                            JSONObject config = new JSONObject();
                            try {
                                data.put("appId", appId);
                                config.put("剪切板开关", enabled);
                                data.put("config", config);
                            } catch (Throwable ignored) {
                            }
                            return data;
                        }
                    });
        }
    }

    @Override
    public void activateALink(Uri uri) {
        if (breakIfEngineIsNull("activateALink")) {
            return;
        }
        mEngine.onDeepLinked(uri);
    }

    @NonNull
    @Override
    public String getSdkVersion() {
        return TLog.SDK_VERSION_NAME;
    }

    @NonNull
    @Override
    public JSONObject getAllAbTestConfigs() {
        return mEngine == null ? new JSONObject() : mEngine.getConfig().getAbConfig();
    }

    @Override
    public void setPrivacyMode(final boolean privacyMode) {
        sPrivacyMode = privacyMode;

        if (Utils.isNotEmpty(appId)) {
            LogUtils.sendJsonFetcher(
                    "update_config",
                    new EventBus.DataFetcher() {
                        @Override
                        public Object fetch() {
                            JSONObject data = new JSONObject();
                            JSONObject config = new JSONObject();
                            try {
                                data.put("appId", appId);
                                config.put("隐私模式开关", privacyMode);
                                data.put("config", config);
                            } catch (Throwable ignored) {
                            }
                            return data;
                        }
                    });
        }
    }

    @Override
    public boolean isPrivacyMode() {
        return sPrivacyMode;
    }

    @Override
    public void setViewId(View view, String id) {
        if (null == view) {
            return;
        }

        // 多实例下共享一个tag
        view.setTag(R.id.applog_tag_view_id, id);
    }

    @Override
    public void setViewId(Dialog dialog, String id) {
        if (null == dialog || null == dialog.getWindow()) {
            return;
        }

        // 多实例下共享一个tag
        dialog.getWindow().getDecorView().setTag(R.id.applog_tag_view_id, id);
    }

    @Override
    public void setViewId(Object alertDialog, String id) {
        if (null == alertDialog) {
            return;
        }
        if (!ReflectUtils.isInstance(
                alertDialog,
                "android.support.v7.app.AlertDialog",
                "androidx.appcompat.app.AlertDialog")) {
            getLogger().warn("Only support AlertDialog view");
            return;
        }
        try {
            Method getWindowMethod = alertDialog.getClass().getMethod("getWindow");
            Window window = (Window) getWindowMethod.invoke(alertDialog);
            if (null != window) {
                // 多实例下共享一个tag
                window.getDecorView().setTag(R.id.applog_tag_view_id, id);
            }
        } catch (NoSuchMethodException e) {
            getLogger().error("Not found getWindow method in alertDialog", e);
        } catch (Throwable e) {
            getLogger().error("Cannot set viewId for alertDialog", e);
        }
    }

    @Override
    public int getLaunchFrom() {
        return sLaunchFrom;
    }

    @Override
    public void setLaunchFrom(int sLaunchFrom) {
        this.sLaunchFrom = sLaunchFrom;
    }

    @Override
    public String getDeepLinkUrl() {
        return null != mEngine ? mEngine.getDeepLinkUrl() : null;
    }

    @Override
    public void setViewProperties(View view, JSONObject properties) {
        if (null != view && null != properties) {
            viewPropertiesMap.put(WidgetUtils.getHashCode(view), properties);
        }
    }

    @Override
    public JSONObject getViewProperties(View view) {
        return null != view ? viewPropertiesMap.get(WidgetUtils.getHashCode(view)) : null;
    }

    @Override
    public void ignoreAutoTrackPage(Class<?>... pages) {
        if (null == pages) {
            return;
        }
        for (Class<?> page : pages) {
            if (null == page) {
                continue;
            }
            if (!PageUtils.isPageClass(page)) {
                getLogger().warn("{} is not a page class", page);
                continue;
            }
            String canonicalName = page.getCanonicalName();
            if (TextUtils.isEmpty(canonicalName)) {
                continue;
            }
            ignoredPageHashCodes.add(canonicalName.hashCode());
        }
    }

    @Override
    public boolean isAutoTrackPageIgnored(Class<?> clazz) {
        if (null == clazz) {
            return false;
        }
        String canonicalName = clazz.getCanonicalName();
        if (TextUtils.isEmpty(canonicalName)) {
            return false;
        }
        return this.ignoredPageHashCodes.contains(canonicalName.hashCode());
    }

    @Override
    public void ignoreAutoTrackClick(View view) {
        if (null == view) {
            return;
        }
        ignoredViewHasCodes.add(WidgetUtils.getHashCode(view));
    }

    @Override
    public void ignoreAutoTrackClickByViewType(Class<?>... types) {
        if (null == types) {
            return;
        }
        ignoredViewTypes.addAll(Arrays.asList(types));
    }

    @Override
    public boolean isAutoTrackClickIgnored(View view) {
        if (null == view) {
            return false;
        }
        if (this.ignoredViewHasCodes.contains(WidgetUtils.getHashCode(view))) {
            return true;
        }
        for (Class<?> type : ignoredViewTypes) {
            if (type.isInstance(view)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void trackPage(Object fragment) {
        trackPage(fragment, null);
    }

    @Override
    public void trackPage(Object fragment, JSONObject properties) {
        trackPageEvent(fragment, properties);
    }

    @Override
    public void trackPage(Activity activity) {
        trackPage(activity, null);
    }

    @Override
    public void trackPage(Activity activity, JSONObject properties) {
        trackPageEvent(activity, properties);
    }

    @Override
    public void trackClick(View view) {
        trackClick(view, null);
    }

    @Override
    public void trackClick(View view, JSONObject properties) {
        // 不受ignore控制
        Click click = ViewHelper.getClickViewInfo(view, false);
        if (null != click && null != properties) {
            click.properties = properties;
        }
        receive(click);
    }

    @Override
    public void setEventHandler(IEventHandler handler) {
        this.eventHandler = handler;
    }

    @Override
    public IEventHandler getEventHandler() {
        return this.eventHandler;
    }

    @Override
    public void initH5Bridge(@NonNull View view, @NonNull String url) {
        Class<?> webViewUtilClz =
                ReflectUtils.getClassByName("com.bytedance.applog.tracker.WebViewUtil");
        if (null == webViewUtilClz) {
            getLogger().warn("No WebViewUtil class, and will not initialize h5 bridge");
            return;
        }
        try {
            Method method =
                    webViewUtilClz.getDeclaredMethod(
                            "injectWebViewBridges", View.class, String.class);
            method.setAccessible(true);
            method.invoke(null, view, url);
        } catch (Throwable e) {
            getLogger().error("Initialize h5 bridge failed", e);
        }
    }

    @Override
    public void setGPSLocation(float longitude, float latitude, String geoCoordinateSystem) {
        if (null == mDevice) {
            getLogger().warn("Please initialize first");
            return;
        }
        gpsLocation = new GpsLocationInfo(longitude, latitude, geoCoordinateSystem);
    }

    @Override
    public void startDurationEvent(String eventName) {
        long startTime = SystemClock.elapsedRealtime();
        if (Assert.notEmpty(eventName, "Event name must not empty!")) {
            return;
        }
        DurationEvent event = durationEventMap.get(eventName);
        if (null == event) {
            event = new DurationEvent(this.getLogger(), eventName);
            durationEventMap.put(eventName, event);
        }
        event.start(startTime);
    }

    @Override
    public void pauseDurationEvent(String eventName) {
        long time = SystemClock.elapsedRealtime();
        if (Assert.notEmpty(eventName, "Event name must not empty!")) {
            return;
        }
        DurationEvent event = durationEventMap.get(eventName);
        if (Assert.notNull(event, "No duration event with name: " + eventName)) {
            return;
        }
        event.pause(time);
    }

    @Override
    public void resumeDurationEvent(String eventName) {
        long time = SystemClock.elapsedRealtime();
        if (Assert.notEmpty(eventName, "Event name must not empty!")) {
            return;
        }
        DurationEvent event = durationEventMap.get(eventName);
        if (Assert.notNull(event, "No duration event with name: " + eventName)) {
            return;
        }
        event.resume(time);
    }

    @Override
    public void stopDurationEvent(String eventName, JSONObject properties) {
        long time = SystemClock.elapsedRealtime();
        if (Assert.notEmpty(eventName, "Event name must not empty!")) {
            return;
        }
        DurationEvent event = durationEventMap.get(eventName);
        if (Assert.notNull(event, "No duration event with name: " + eventName)) {
            return;
        }

        long duration = event.end(time);

        // 新的属性
        JSONObject props = new JSONObject();
        JsonUtils.mergeJsonObject(properties, props);
        try {
            props.put(Api.KEY_EVENT_DURATION, duration);
        } catch (Throwable e) {
            getLogger().error("JSON handle failed", e);
        }
        receive(new EventV3(eventName, props));

        // delete event
        durationEventMap.remove(eventName);
    }

    @Override
    public void clearDb() {
        // This line is a Aassert
        assert null != mEngine : "clearDb before init";

        long s = SystemClock.elapsedRealtime();
        getLogger().debug("Start to clear db data...");
        // 清理数据库的数据
        mEngine.getDbStoreV2().clear();
        getLogger().debug("Db data cleared");
    }

    @Override
    public void initWebViewBridge(@NonNull View view, @NonNull String url) {
        Class<?> webViewUtilClass =
                ReflectUtils.getClassByName("com.bytedance.applog.tracker.WebViewUtil");
        if (null != webViewUtilClass) {
            try {
                Method method =
                        webViewUtilClass.getMethod(
                                "injectWebViewBridges", View.class, String.class);
                method.invoke(null, view, url);
            } catch (Throwable e) {
                getLogger().error("Init webview bridge failed", e);
            }
        }
    }

    // ---------------实现类单独有的方法------------------------
    public DataObserverHolder getDataObserverHolder() {
        return dataObserverHolder;
    }

    public SessionObserverHolder getSessionObserverHolder() {
        return sessionObserverHolder;
    }

    public EventObserverHolder getEventObserverHolder() {
        return eventObserverHolder;
    }

    public DeviceRegisterParameterFactory getDeviceRegisterParameterFactory() {
        return deviceRegisterParameterFactory;
    }

    public ApiParamsUtil getApiParamsUtil() {
        return apiParamsUtil;
    }

    public Api getApi() {
        return api;
    }

    /**
     * 检查是否要设置uuid到device中
     */
    private void postSetUuidAfterDm() {
        if (postInitUserUniqueId.hasValue()
                && !Utils.equals(postInitUserUniqueId.getValue(), mConfig.getUserUniqueId())) {
            mDevice.setUserUniqueId(
                    postInitUserUniqueId.getValue());
            getLogger().debug("postSetUuidAfterDm uuid -> " + postInitUserUniqueId.getValue());
            mDevice.setSsid("");
        }
        if (postInitUserUniqueIdType.hasValue()
                && !Utils.equals(postInitUserUniqueIdType.getValue(), mConfig.getUserUniqueIdType())) {
            mDevice.setUserUniqueIdType(
                    postInitUserUniqueIdType.getValue());
            getLogger().debug("postSetUuidAfterDm uuid -> " + postInitUserUniqueIdType.getValue());
            mDevice.setSsid("");
        }
    }

    /**
     * 手动埋点页面事件
     *
     * @param obj        Activity|Fragment
     * @param properties 属性
     */
    private void trackPageEvent(Object obj, JSONObject properties) {
        if (null == mNav || null == obj) {
            return;
        }
        EventV3 pageEvent = new EventV3(EVENT_KEY, true);
        JSONObject pageJson = new JSONObject();
        String name = obj.getClass().getName();
        boolean isFragment = false;
        if (PageUtils.isFragment(obj)) {
            // Fragment单独处理name，添加activity的title
            Activity activity = null;
            try {
                Method getActivityMethod = obj.getClass().getMethod("getActivity");
                activity = (Activity) getActivityMethod.invoke(obj);
            } catch (Throwable ignored) {
            }
            if (null != activity) {
                name = activity.getClass().getName() + ":" + name;
            }
            isFragment = true;
        }
        try {
            pageJson.put(Page.COL_NAME, name);
            pageJson.put(Page.COL_IS_FRAGMENT, isFragment);
            pageJson.put(Page.COL_DURATION, 1000L);
            pageJson.put(Page.COL_TITLE, PageUtils.getTitle(obj));
            pageJson.put(Page.COL_PATH, PageUtils.getPath(obj));
            pageJson.put(Page.COL_IS_CUSTOM, true);
            JsonUtils.mergeJsonObject(properties, pageJson);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        pageEvent.setProperties(pageJson);
        receive(pageEvent);
    }

    @NonNull
    public ViewExposureManager getViewExposureManager() {
        return viewExposureManager;
    }

    /**
     * 判断是否为debug模式
     */
    public boolean isDebugMode() {
        return mDebugMode;
    }

    /**
     * 获取位置对象
     *
     * @return LocationInfo
     */
    public GpsLocationInfo getGpsLocation() {
        return gpsLocation;
    }

    /**
     * 如果mEngine为null，则断言
     *
     * @return true: mEngine == null
     */
    private boolean breakIfEngineIsNull(String method) {
        return Assert.notNull(mEngine, "Call " + method + " before please initialize first");
    }


    /**
     * 如果mDevice为null，则断言
     *
     * @return true: mDevice == null
     */
    private boolean breakIfDeviceIsNull(String method) {
        return Assert.notNull(mDevice, "Call " + method + " before please initialize first");
    }

    /**
     * 发送配置信息到devtools
     *
     * @param config InitConfig
     */
    private void sendConfig2DevTools(final InitConfig config) {
        if (LogUtils.isDisabled()) {
            return;
        }
        LogUtils.sendJsonFetcher(
                "init_begin",
                new EventBus.DataFetcher() {
                    @Override
                    public Object fetch() {
                        JSONObject params = new JSONObject();
                        try {
                            params.put("appId", config.getAid());
                            params.put("channel", config.getChannel());
                            JSONObject configParams = new JSONObject();
                            configParams.put("AppLog 版本号", TLog.SDK_VERSION_NAME);
                            configParams.put("AppLog 版本地区", BuildConfig.IS_I18N ? "海外" : "国内");
                            configParams.put("接口加密开关", sEncryptAndCompress);
                            if (sEncryptAndCompress) {
                                IEncryptor encryptor = config.getEncryptor();
                                configParams.put(
                                        "是否配置了自定义加密", encryptor == null ? "未配置" : "客户端已配置");
                                if (encryptor instanceof CustomEncryptor) {
                                    configParams.put(
                                            "自定义加密类型",
                                            IEncryptorType.DEFAULT_ENCRYPTOR.equals(
                                                    ((CustomEncryptor) encryptor)
                                                            .encryptorType())
                                                    ? "默认加密类型"
                                                    : (((CustomEncryptor) encryptor)
                                                    .encryptorType()));
                                } else {
                                    configParams.put("自定义加密类型", "默认加密类型");
                                }
                            }
                            configParams.put("日志开关", config.isLogEnable());
                            configParams.put("自定义日志打印", config.getLogger() != null);
                            configParams.put("AB实验开关", config.isAbEnable());
                            configParams.put("自动启动图开关", config.autoStart());
                            configParams.put("H5 打通开关", config.isH5BridgeEnable());
                            configParams.put("H5 全埋点注入", config.isH5CollectEnable());
                            if (null != config.getH5BridgeAllowlist()
                                    && !config.getH5BridgeAllowlist().isEmpty()) {
                                configParams.put(
                                        "H5 域名白名单",
                                        TextUtils.join("、", config.getH5BridgeAllowlist()));
                            }
                            configParams.put("不过滤 H5 域名开关", config.isH5BridgeAllowAll());
                            configParams.put("全埋点开关", config.isAutoTrackEnabled());
                            // 全埋点类型
                            List<String> autoTrackTypes = new ArrayList<>();
                            if (AutoTrackEventType.hasEventType(
                                    config.getAutoTrackEventType(), AutoTrackEventType.CLICK)) {
                                autoTrackTypes.add("点击事件");
                            }
                            if (AutoTrackEventType.hasEventType(
                                    config.getAutoTrackEventType(), AutoTrackEventType.PAGE)) {
                                autoTrackTypes.add("页面事件");
                            }
                            if (AutoTrackEventType.hasEventType(
                                    config.getAutoTrackEventType(),
                                    AutoTrackEventType.PAGE_LEAVE)) {
                                autoTrackTypes.add("页面离开事件");
                            }
                            if (!autoTrackTypes.isEmpty()) {
                                params.put("全埋点类型", TextUtils.join("、", autoTrackTypes));
                            }
                            configParams.put("视图曝光开关", config.isExposureEnabled());
                            configParams.put("采集屏幕方向开关", config.isScreenOrientationEnabled());
                            configParams.put("初始化 UUID", config.getUserUniqueId());
                            configParams.put("初始化 UUID 类型", config.getUserUniqueIdType());
                            configParams.put("采集 ANDROID ID 开关", config.isAndroidIdEnabled());
                            configParams.put("采集运营商信息开关", config.isOperatorInfoEnabled());
                            configParams.put("自动采集 FRAGMENT 开关", config.isAutoTrackFragmentEnabled());
                            configParams.put("后台静默开关", config.isSilenceInBackground());
                            configParams.put("鸿蒙设备采集开关", config.isHarmonyEnabled());
                            configParams.put("隐私模式开关", isPrivacyMode());
                            configParams.put(
                                    "采集 Crash",
                                    AppCrashType.hasCrashType(
                                            config.getTrackCrashType(), AppCrashType.JAVA)
                                            ? "JAVA"
                                            : "不采集");
                            configParams.put("ALINK 监听", aLinkListener != null);
                            configParams.put("延迟深度链接开关", config.isDeferredALinkEnabled());
                            configParams.put("缓存文件名称", config.getSpName());
                            configParams.put("数据库文件名称", config.getDbName());
                            configParams.put("监听生命周期", config.isHandleLifeCycle());
                            configParams.put("小版本号", config.getVersionMinor());
                            configParams.put("版本号编码", String.valueOf(config.getVersionCode()));
                            configParams.put("版本号", config.getVersion());
                            configParams.put("应用名称", config.getAppName());
                            configParams.put("圈选配置", null != config.getPicker());
                            configParams.put("当前进程", mConfig.isMainProcess() ? "主进程" : "子进程");
                            configParams.put("地区", config.getRegion());
                            configParams.put("语言", config.getLanguage());
                            configParams.put("PLAY 开关", config.isPlayEnable());
                            configParams.put("Gaid 开关", config.isGaidEnabled());
                            configParams.put("LaunchTerminate 开关", config.isLaunchTerminateEnabled());
                            if (config.isGaidEnabled()) {
                                configParams.put(
                                        "GAID 获取超时时间", config.getGaidTimeOutMilliSeconds());
                            }
                            configParams.put("PageMeta 接口注解开关", config.isPageMetaAnnotationEnable());

                            // uri
                            if (null != config.getUriConfig()) {
                                List<String> serverUrls = new ArrayList<>();
                                if (null != config.getUriConfig().getSendUris()) {
                                    serverUrls.addAll(
                                            Arrays.asList(config.getUriConfig().getSendUris()));
                                }
                                if (Utils.isNotEmpty(config.getUriConfig().getRegisterUri())) {
                                    serverUrls.add(config.getUriConfig().getRegisterUri());
                                }
                                if (Utils.isNotEmpty(config.getUriConfig().getSettingUri())) {
                                    serverUrls.add(config.getUriConfig().getSettingUri());
                                }
                                if (Utils.isNotEmpty(config.getUriConfig().getAbUri())) {
                                    serverUrls.add(config.getUriConfig().getAbUri());
                                }
                                if (Utils.isNotEmpty(config.getUriConfig().getSettingUri())) {
                                    serverUrls.add(config.getUriConfig().getSettingUri());
                                }
                                if (Utils.isNotEmpty(config.getUriConfig().getBusinessUri())) {
                                    serverUrls.add(config.getUriConfig().getBusinessUri());
                                }
                                if (Utils.isNotEmpty(config.getUriConfig().getProfileUri())) {
                                    serverUrls.add(config.getUriConfig().getProfileUri());
                                }
                                if (Utils.isNotEmpty(
                                        config.getUriConfig().getAlinkAttributionUri())) {
                                    serverUrls.add(config.getUriConfig().getAlinkAttributionUri());
                                }
                                if (Utils.isNotEmpty(config.getUriConfig().getAlinkQueryUri())) {
                                    serverUrls.add(config.getUriConfig().getAlinkQueryUri());
                                }
                                configParams.put("服务域名配置", TextUtils.join("、", serverUrls));
                            } else {
                                configParams.put("服务域名配置", "SaaS 默认");
                            }
                            params.put("config", configParams);
                        } catch (Throwable ignored) {

                        }
                        return params;
                    }
                });
    }

    /**
     * 初始化之后的处理
     *
     * @param startTime
     */
    private void handleAfterInit(long startTime) {
        // 实时埋点验证和圈选
        if (Utils.equals(SimulateLaunchActivity.entryAppId, getAppId())) {
            SimulateLoginTask.start(this);
        }

        // 发送缓存信息到devtools
        mConfig.sendOriginCachedConfig2DevTools();
    }

    /**
     * 获取 ConfigManager
     *
     * @return
     */
    public ConfigManager getConfig() {
        if (breakIfEngineIsNull("getConfig")) {
            return null;
        }
        return mEngine.getConfig();
    }

    public boolean isPageMetaAnnotationEnable() {
        return getInitConfig() != null && getInitConfig().isPageMetaAnnotationEnable();
    }
}
