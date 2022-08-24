// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

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
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bytedance.applog.alink.IALinkListener;
import com.bytedance.applog.collector.Navigator;
import com.bytedance.applog.engine.Engine;
import com.bytedance.applog.event.DurationEvent;
import com.bytedance.applog.event.IEventHandler;
import com.bytedance.applog.exposure.ViewExposureManager;
import com.bytedance.applog.filter.AbstractEventFilter;
import com.bytedance.applog.holder.DataObserverHolder;
import com.bytedance.applog.holder.EventObserverHolder;
import com.bytedance.applog.holder.SessionObserverHolder;
import com.bytedance.applog.manager.AppLogCache;
import com.bytedance.applog.manager.ConfigManager;
import com.bytedance.applog.manager.DeviceManager;
import com.bytedance.applog.manager.DeviceRegisterParameterFactory;
import com.bytedance.applog.monitor.IMonitor;
import com.bytedance.applog.monitor.model.ApiCallTrace;
import com.bytedance.applog.network.DefaultClient;
import com.bytedance.applog.network.INetworkClient;
import com.bytedance.applog.profile.UserProfileCallback;
import com.bytedance.applog.profile.UserProfileHelper;
import com.bytedance.applog.server.Api;
import com.bytedance.applog.server.ApiParamsUtil;
import com.bytedance.applog.store.BaseData;
import com.bytedance.applog.store.Click;
import com.bytedance.applog.store.CustomEvent;
import com.bytedance.applog.store.EventV3;
import com.bytedance.applog.store.Page;
import com.bytedance.applog.util.Assert;
import com.bytedance.applog.util.JsonUtils;
import com.bytedance.applog.util.PageUtils;
import com.bytedance.applog.util.ReflectUtils;
import com.bytedance.applog.util.TLog;
import com.bytedance.applog.util.ViewHelper;
import com.bytedance.applog.util.WidgetUtils;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 统一的埋点实例实现类
 *
 * @author luodong.seu
 */
public final class AppLogInstance implements IAppLogInstance {
    private static final List<AppLogInstance> sInstances = new LinkedList<>();
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
    private volatile IHeaderCustomTimelyCallback sHeaderCustomTimelyCallback;
    private volatile AbstractEventFilter sEventFilterFromClient;
    private volatile boolean sPrivacyMode = false;
    private DataObserverHolder dataObserverHolder;
    private IALinkListener aLinkListener;
    private IActiveCustomParamsCallback sActiveCustomParamsCallback;

    /**
     * 埋点事件处理器
     */
    private IEventHandler eventHandler;

    /**
     * 是否压缩加密，仅debug版有效
     */
    private volatile boolean sEncryptAndCompress = true;

    /**
     * 最后一次拉取ab实验的时间
     */
    private long mLastPullAbTestConfigsTime = 0L;
    /**
     * 拉取ab实验配置的频控时间间隔
     */
    private long mPullAbTestConfigsThrottleMills = 10 * 1000L;

    public AppLogInstance() {
        sInstId.incrementAndGet();

        // 初始化对象
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
    public String toString() {
        return "AppLogInstance{id:" + sInstId.get() + ";appId:" + appId + "}@" + hashCode();
    }

    public static List<AppLogInstance> getAllInstances() {
        return sInstances;
    }

    @Override
    public String getAppId() {
        return appId;
    }

    @Override
    public void receive(BaseData data) {
        if (null == data) {
            return;
        }

        data.setAppId(getAppId());

        //        TLog.d("{} received data: {}", this, data);

        if (null == mEngine) {
            cache.cache(data);
        } else {
            mEngine.receive(data);
        }
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
    public void setAppContext(IAppContext appContext) {
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
    public void init(@NonNull Context context, @NonNull InitConfig config) {
        synchronized (AppLogInstance.class) {
            if (Assert.notEmpty(config.getAid(), "App id must not be empty!")) {
                return;
            }
            if (Assert.f(
                    AppLogHelper.hasInstanceByAppId(config.getAid()),
                    "The app id:" + config.getAid() + " has an instance already.")) {
                return;
            }

            // 仅主实例能设置logger
            if (AppLogHelper.isGlobalInstance(this)) {
                TLog.setLogger(context, config.getLogger(), config.isLogEnable());
            } else if (null != config.getLogger()) {
                TLog.w("Only static AppLog can set logger.");
            }

            TLog.i("AppLog init begin...");

            appId = config.getAid();
            mApp = (Application) context.getApplicationContext();

            // 多实例的SharedPreferenceName
            if (TextUtils.isEmpty(config.getSpName())) {
                config.setSpName(AppLogHelper.getInstanceSpName(this, ConfigManager.SP_FILE));
            }

            mConfig = new ConfigManager(this, mApp, config);
            mDevice = new DeviceManager(this, mApp, mConfig);
            mEngine = new Engine(this, mConfig, mDevice, cache);

            // 多实例下仅第一个生效
            mNav = Navigator.registerGlobalListener(mApp);

            // View 曝光事件管理器
            viewExposureManager = new ViewExposureManager(this);

            initMetaSec(context);

            // 对齐内部版，初始化的时候为1
            sLaunchFrom = 1;
            mStarted = config.autoStart();

            TLog.i("AppLog init end.");
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

    @Override
    public void initMetaSec(Context context) {
        Class<?> clazz =
                ReflectUtils.getClassByName("com.bytedance.applog.metasec.AppLogSecHelper");
        if (null == clazz) {
            TLog.d("No AppLogSecHelper class, and will not init.");
            return;
        }
        try {
            Method method = clazz.getDeclaredMethod("init", IAppLogInstance.class, Context.class);
            method.setAccessible(true);
            method.invoke(null, this, context);
        } catch (Throwable e) {
            TLog.e("Initialize AppLogSecHelper failed.", e);
        }
    }

    @Override
    public void start() {
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

    @Override
    public void flush() {
        if (breakIfEngineIsNull()) {
            return;
        }
        long s = SystemClock.elapsedRealtime();
        mEngine.process(null, true);
        traceApiCall("flush", s);
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
        mEngine.getSession().setUserId(id);
    }

    @Override
    public void setAppLanguageAndRegion(String language, String region) {
        if (breakIfEngineIsNull()) {
            return;
        }
        mEngine.setLanguageAndRegion(language, region);
    }

    @Override
    public void setGoogleAid(String gaid) {
        if (breakIfDeviceIsNull()) {
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
    public void setUserUniqueID(final String id) {
        if (breakIfDeviceIsNull()) {
            return;
        }
        setUserUniqueID(id, mDevice.getUserUniqueIdType());
    }

    @Override
    public void setUserUniqueID(final String id, final String type) {
        if (breakIfEngineIsNull()) {
            return;
        }
        long s = SystemClock.elapsedRealtime();
        mEngine.setUserUniqueId(id, type);
        traceApiCall("setUserUniqueID", s);
    }

    @Override
    public void setExtraParams(IExtraParams iExtraParams) {
        apiParamsUtil.setsExtraParams(iExtraParams);
    }

    @Override
    public void setActiveCustomParams(IActiveCustomParamsCallback callback) {
        sActiveCustomParamsCallback = callback;
    }

    @Override
    public IActiveCustomParamsCallback getActiveCustomParams() {
        return sActiveCustomParamsCallback;
    }

    @Override
    public void setTouchPoint(String touchPoint) {
        setHeaderInfo(Api.KEY_TOUCH_POINT, touchPoint);
    }

    @Override
    public void setHeaderInfo(HashMap<String, Object> custom) {
        if (breakIfDeviceIsNull()) {
            return;
        }
        mDevice.setCustom(custom);
    }

    @Override
    public void setHeaderInfo(String key, Object value) {
        if (breakIfDeviceIsNull()) {
            return;
        }
        if (!TextUtils.isEmpty(key)) {
            HashMap<String, Object> map = new HashMap<>();
            map.put(key, value);
            mDevice.setCustom(map);
        }
    }

    @Override
    public void removeHeaderInfo(String key) {
        if (breakIfDeviceIsNull()) {
            return;
        }
        mDevice.removeHeaderInfo(key);
    }

    @Override
    public void setExternalAbVersion(String version) {
        if (breakIfDeviceIsNull()) {
            return;
        }
        mDevice.setExternalAbVersion(version);
    }

    @Override
    public String getAbSdkVersion() {
        if (breakIfDeviceIsNull()) {
            return null;
        }
        return mDevice.getAbSdkVersion();
    }

    @Nullable
    @Override
    public <T> T getAbConfig(String key, T defaultValue) {
        if (breakIfDeviceIsNull()) {
            return null;
        }
        long s = SystemClock.elapsedRealtime();
        T value = mDevice.getAbConfig(key, defaultValue);
        traceApiCall("getAbConfig", s);
        return value;
    }

    @Override
    public void pullAbTestConfigs() {
        if (breakIfEngineIsNull()) {
            return;
        }
        long s = SystemClock.elapsedRealtime();
        long curTime = System.currentTimeMillis();
        if (Math.abs(curTime - mLastPullAbTestConfigsTime) > mPullAbTestConfigsThrottleMills) {
            mLastPullAbTestConfigsTime = curTime;
            mEngine.workAbConfiger();
        } else {
            TLog.w("Operation is too frequent, please try again later.");
        }
        traceApiCall("pullAbTestConfigs", s);
    }

    @Override
    public void setPullAbTestConfigsThrottleMills(Long mills) {
        mPullAbTestConfigsThrottleMills = null != mills && mills > 0L ? mills : 0L;
    }

    @Deprecated
    @Override
    public String getAid() {
        return getAppId();
    }

    @Override
    public <T> T getHeaderValue(String key, T fallbackValue, Class<T> tClass) {
        if (breakIfDeviceIsNull()) {
            return null;
        }
        return mDevice.getHeaderValue(key, fallbackValue, tClass);
    }

    @Override
    public void setTracerData(JSONObject tracerData) {
        if (breakIfDeviceIsNull()) {
            return;
        }
        mDevice.setTracerData(tracerData);
    }

    @Override
    public void setUserAgent(String ua) {
        if (breakIfDeviceIsNull()) {
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
    public void onEventV3(@NonNull String event, @Nullable JSONObject params, int eventType) {
        if (TextUtils.isEmpty(event)) {
            TLog.e("event name is empty", null);
            return;
        }
        long s = SystemClock.elapsedRealtime();
        receive(
                new EventV3(
                        appId, event, false, params != null ? params.toString() : null, eventType));
        traceApiCall("onEventV3", s);
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
            TLog.ysnp(t);
        }
        onEventV3(event, jsonParams, eventType);
    }

    @Override
    public void onEventV3(@NonNull String event, @Nullable Bundle params) {
        onEventV3(event, params, DEFAULT_EVENT);
    }

    @Override
    public void onMiscEvent(@NonNull String logType, @Nullable JSONObject obj) {
        if (TextUtils.isEmpty(logType) || obj == null || obj.length() <= 0) {
            TLog.w("call onMiscEvent with invalid params, return");
            return;
        }
        try {
            obj.put("log_type", logType);
            receive(new CustomEvent("log_data", obj));
        } catch (Exception e) {
            TLog.e("call onMiscEvent error: ", e);
        }
    }

    @Override
    public void setEncryptAndCompress(boolean enable) {
        sEncryptAndCompress = enable;
    }

    @Override
    public boolean getEncryptAndCompress() {
        return sEncryptAndCompress;
    }

    @Override
    public String getDid() {
        if (breakIfDeviceIsNull()) {
            return "";
        }
        return mDevice.getBdDid();
    }

    @Override
    public String getUdid() {
        if (breakIfDeviceIsNull()) {
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
        eventObserverHolder.addEventObserver(iEventObserver);
    }

    @Override
    public void removeEventObserver(IEventObserver iEventObserver) {
        eventObserverHolder.removeEventObserver(iEventObserver);
    }

    @Override
    public void setAccount(Account account) {
        if (breakIfDeviceIsNull()) {
            return;
        }
        mDevice.setAccount(account);
    }

    @Override
    public String getIid() {
        if (breakIfDeviceIsNull()) {
            return "";
        }
        return mDevice.getIid();
    }

    @Override
    public String getSsid() {
        if (breakIfDeviceIsNull()) {
            return "";
        }
        return mDevice.getSsid();
    }

    @Override
    public String getUserUniqueID() {
        if (breakIfDeviceIsNull()) {
            return "";
        }
        return mDevice.getUserUniqueId();
    }

    @Override
    public String getUserID() {
        if (breakIfEngineIsNull()) {
            return null;
        }
        return String.valueOf(mEngine.getSession().getUserId());
    }

    @Override
    public String getClientUdid() {
        if (breakIfDeviceIsNull()) {
            return "";
        }
        return mDevice.getClientUdid();
    }

    @Override
    public String getOpenUdid() {
        if (breakIfDeviceIsNull()) {
            return "";
        }
        return mDevice.getOpenUdid();
    }

    @Override
    public void setUriRuntime(UriConfig config) {
        if (breakIfEngineIsNull()) {
            return;
        }
        mEngine.setUriConfig(config);
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
        if (breakIfDeviceIsNull()) {
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
        if (breakIfDeviceIsNull()) {
            return;
        }
        mDevice.setAppTrackJson(appTrackJson);
    }

    @Override
    public boolean isNewUser() {
        if (breakIfDeviceIsNull()) {
            return false;
        }
        return mDevice.isNewUser();
    }

    @Override
    public void onResume(Context context) {
        if (context instanceof Activity) {
            onActivityResumed((Activity) context, context.hashCode());
        }
    }

    @Override
    public void onPause(Context context) {
        if (context instanceof Activity) {
            onActivityPause();
        }
    }

    @Override
    public void onActivityResumed(Activity activity, int hashCode) {
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
    public void registerHeaderCustomCallback(IHeaderCustomTimelyCallback customTimelyCallback) {
        sHeaderCustomTimelyCallback = customTimelyCallback;
    }

    @Override
    public IHeaderCustomTimelyCallback getHeaderCustomCallback() {
        return sHeaderCustomTimelyCallback;
    }

    @Override
    public void userProfileSetOnce(JSONObject jsonObject, UserProfileCallback callback) {
        if (breakIfEngineIsNull()) {
            return;
        }
        mEngine.userProfileExec(UserProfileHelper.METHOD_SET, jsonObject, callback);
    }

    @Override
    public void userProfileSync(JSONObject jsonObject, UserProfileCallback callback) {
        if (breakIfEngineIsNull()) {
            return;
        }
        mEngine.userProfileExec(UserProfileHelper.METHOD_SYNC, jsonObject, callback);
    }

    @Override
    public void startSimulator(String cookie) {
        if (breakIfEngineIsNull()) {
            return;
        }
        mEngine.startSimulator(cookie);
    }

    @Override
    public void setRangersEventVerifyEnable(boolean enable, String cookie) {
        if (breakIfEngineIsNull()) {
            return;
        }
        mEngine.setRangersEventVerifyEnable(enable, cookie);
    }

    @Override
    public void profileSet(JSONObject jsonObject) {
        if (breakIfEngineIsNull()) {
            return;
        }
        if (jsonObject == null || jsonObject.length() == 0) {
            return;
        }
        mEngine.profileSet(jsonObject);
    }

    @Override
    public void profileSetOnce(JSONObject jsonObject) {
        if (breakIfEngineIsNull()) {
            return;
        }
        if (jsonObject == null || jsonObject.length() == 0) {
            return;
        }
        mEngine.profileSetOnce(jsonObject);
    }

    @Override
    public void profileUnset(String key) {
        if (breakIfEngineIsNull()) {
            return;
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(key, "");
        } catch (Throwable e) {
            TLog.e(e);
        }
        mEngine.profileUnset(jsonObject);
    }

    @Override
    public void profileIncrement(JSONObject jsonObject) {
        if (breakIfEngineIsNull()) {
            return;
        }
        if (jsonObject == null || jsonObject.length() == 0) {
            return;
        }
        try {
            if (!JsonUtils.paramValueCheck(jsonObject, new Class[]{Integer.class}, null)) {
                TLog.e("only support Int", new Exception());
                return;
            }
        } catch (Throwable e) {
            TLog.e(e);
        }
        mEngine.profileIncrement(jsonObject);
    }

    @Override
    public void profileAppend(JSONObject jsonObject) {
        if (breakIfEngineIsNull()) {
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
                TLog.e("only support String、Int、String Array！", new Exception());
                return;
            }
        } catch (Throwable e) {
            TLog.e(e);
        }
        mEngine.profileAppend(jsonObject);
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
    public void setClipboardEnabled(boolean enabled) {
        if (breakIfEngineIsNull()) {
            return;
        }
        mEngine.setClipboardEnabled(enabled);
    }

    @Override
    public void activateALink(Uri uri) {
        if (breakIfEngineIsNull()) {
            return;
        }
        mEngine.onDeepLinked(uri);
    }

    @Override
    public String getSdkVersion() {
        return TLog.SDK_VERSION_NAME;
    }

    @Override
    public JSONObject getAllAbTestConfigs() {
        return mEngine == null ? new JSONObject() : mEngine.getConfig().getAbConfig();
    }

    @Override
    public void setPrivacyMode(boolean privacyMode) {
        sPrivacyMode = privacyMode;
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
            TLog.i("Only support AlertDialog view.");
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
            TLog.e("Not found getWindow method in alertDialog.", e);
        } catch (Exception e) {
            TLog.e("Cannot set viewId for alertDialog.", e);
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
                TLog.w(page + " is not a page class.");
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
    public void initH5Bridge(View view, String url) {
        Class<?> webViewUtilClz =
                ReflectUtils.getClassByName("com.bytedance.applog.tracker.WebViewUtil");
        if (null == webViewUtilClz) {
            TLog.d("No WebViewUtil class, and will not initialize h5 bridge.");
            return;
        }
        try {
            Method method =
                    webViewUtilClz.getDeclaredMethod(
                            "injectWebViewBridges", View.class, String.class);
            method.setAccessible(true);
            method.invoke(null, view, url);
        } catch (Throwable e) {
            TLog.e("Initialize h5 bridge failed.", e);
        }
    }

    @Override
    public void setGPSLocation(float longitude, float latitude, String geoCoordinateSystem) {
        if (null == mDevice) {
            TLog.w("Please initialize first.");
            return;
        }
        mDevice.setGPSLocation(longitude, latitude, geoCoordinateSystem);
    }

    @Override
    public void startDurationEvent(String eventName) {
        long startTime = SystemClock.elapsedRealtime();
        if (Assert.notEmpty(eventName, "Event name must not empty!")) {
            return;
        }
        DurationEvent event = durationEventMap.get(eventName);
        if (null == event) {
            event = new DurationEvent(eventName);
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
            TLog.e(e);
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
        TLog.d("Start to clear db data...");
        // 清理数据库的数据
        mEngine.getDbStoreV2().clear();
        TLog.d("Db data cleared.");
        traceApiCall("clearDb", s);
    }

    @Override
    public IMonitor getMonitor() {
        if (null == mEngine) {
            return null;
        }
        return mEngine.getMonitor();
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
     * 手动埋点页面事件
     *
     * @param obj        Activity|Fragment
     * @param properties 属性
     */
    private void trackPageEvent(Object obj, JSONObject properties) {
        if (null == mNav || null == obj) {
            return;
        }
        Page page = new Page();
        page.name = obj.getClass().getName();
        if (PageUtils.isFragment(obj)) {
            // Fragment单独处理name，添加activity的title
            Activity activity = null;
            try {
                Method getActivityMethod = obj.getClass().getMethod("getActivity");
                activity = (Activity) getActivityMethod.invoke(obj);
            } catch (Throwable ignored) {
            }
            if (null != activity) {
                page.name = activity.getClass().getName() + ":" + page.name;
            }
        }
        page.duration = 1000L;
        page.title = PageUtils.getTitle(obj);
        page.path = PageUtils.getPath(obj);
        if (null != properties) {
            page.properties = properties;
        }
        receive(page);
    }

    public ViewExposureManager getViewExposureManager() {
        return viewExposureManager;
    }

    /**
     * 如果mEngine为null，则断言
     *
     * @return true: mEngine == null
     */
    private boolean breakIfEngineIsNull() {
        return Assert.notNull(mEngine, "Please initialize first.");
    }

    /**
     * 如果mDevice为null，则断言
     *
     * @return true: mDevice == null
     */
    private boolean breakIfDeviceIsNull() {
        return Assert.notNull(mDevice, "Please initialize first.");
    }

    /**
     * 监控API调用
     *
     * @param apiName api名称
     */
    private void traceApiCall(String apiName, long startTime) {
        if (null == getMonitor()) {
            return;
        }
        long endTime = SystemClock.elapsedRealtime();
        ApiCallTrace trace = new ApiCallTrace();
        trace.setApiName(apiName);
        trace.setTime(endTime - startTime);
        getMonitor().trace(trace);
    }
}
