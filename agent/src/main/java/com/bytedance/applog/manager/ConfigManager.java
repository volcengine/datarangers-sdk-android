// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.manager;

import android.accounts.Account;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bytedance.applog.AppLogHelper;
import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.InitConfig;
import com.bytedance.applog.engine.Engine;
import com.bytedance.applog.log.EventBus;
import com.bytedance.applog.log.LogUtils;
import com.bytedance.applog.server.Api;
import com.bytedance.applog.store.BaseData;
import com.bytedance.applog.store.EventV3;
import com.bytedance.applog.store.SharedPreferenceCacheHelper;
import com.bytedance.applog.util.JsonUtils;
import com.bytedance.applog.util.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * @author shiyanlong 2019/1/20
 */
public class ConfigManager {

    /** for forward compatible, don't modify these fields, copy from old version */
    private static final String SP_SESSION = "last_sp_session";

    private static final String KEY_SESS_LAST_DAY = "session_last_day";

    private static final String KEY_SESS_ORDER = "session_order";

    public static final String SP_FILE = "applog_stats";

    private static final String SP_KEY_GOOGLE_AID = "google_aid";

    private static final String SP_KEY_APP_LANGUAGE = "app_language";

    private static final String SP_KEY_APP_REGION = "app_region";

    private static final String SP_KEY_IS_FIRST_TIME_LAUNCH = "is_first_time_launch";

    private static final String SP_KEY_IS_FIRST_APP_LAUNCH = "is_first_app_launch";

    private static final String SP_KEY_SESSION_INTERVAL = "session_interval";

    private static final String SP_KEY_EVENT_INTERVAL = "batch_event_interval";

    private static final String SP_KEY_EVENT_SIZE = "batch_event_size";

    private static final String SP_KEY_LAUNCH_TIMELY = "send_launch_timely";

    private static final String SP_KEY_ENTER_BACKGROUND_NOT_SEND = "enter_background_not_send";

    private static final String KEY_CONFIG_TS = "app_log_last_config_time";

    private static final String KEY_ABTEST_INTERVAL = "abtest_fetch_interval";

    private static final String KEY_BAV_ENABLE = "bav_log_collect";

    private static final String KEY_BAV_AB_ENABLE = "bav_ab_config";

    private static final String KEY_REAL_TIME_EVENTS = "real_time_events";

    private static final String KEY_SENSITIVE_FIELDS = "sensitive_fields";

    private static final String SP_CUSTOM_HEADER = "header_custom";

    private static final String CUSTOM_CUSTOM_INFO = "header_custom_info";

    private static final String CUSTOM_APP_TRACK = Api.KEY_APP_TRACK;

    private static final String SP_KEY_USER_AGENT = Api.KEY_USER_AGENT;

    private static final String SP_KEY_CONFIG_INTERVAL = "fetch_interval";

    private static final String CUSTOM_AB_CONFIG = "ab_configure";

    private static final String EXTERNAL_AB_VERSION = "external_ab_version";

    public static final int PROCESS_UNKNOWN = 0;

    public static final int PROCESS_MAIN = 1;

    public static final int PROCESS_OTHER = 2;

    private static final long INTERVAL_UPDATE_CONFIG_DEFAULT =
            6 * 60 * 60; // unit is second, is same with config

    private static final long EVENT_INTERVAL_DEFAULT = 60 * 1000L;

    private static final long MIN_EVENT_INTERVAL = 10 * 1000L;

    private static final long MAX_EVENT_INTERVAL = 5 * 60 * 1000L;

    private static final long MIN_EVENT_SIZE = 50;

    private static final long MAX_EVENT_SIZE = 9999;

    private static final String KEY_BACK_OFF_RATIO = "backoff_ratio";

    private static final String KEY_MAX_REQUEST_FREQUENCY = "max_request_frequency";

    private static final int BACK_OFF_DENOMINATOR = 10000;

    private static final int MAX_REQUEST_FREQUENCY = 27; // (LIMIT_SELECT_PACK + 1) * 3

    private static final int MIN_REQUEST_FREQUENCY = 1;

    private final Context mApp;

    private final AppLogInstance appLogInstance;

    private final InitConfig mInitConfig;

    private final SharedPreferences mCustomSp;

    private final SharedPreferences mSessionSp;

    private final SharedPreferences mSp;

    private volatile JSONObject mAbConfig;

    private volatile String mExternalAbVersion;

    private volatile JSONObject mConfig;

    private final Set<String> mBlockSetV1 = new HashSet<>();
    private final Set<String> mBlockSetV3 = new HashSet<>();

    private volatile HashSet<String> mRealTimeEvents;

    private int mBackoffRatio = 0; // 0-9999, the ratio of send log back off

    private int mMaxRequestFrequency = MAX_REQUEST_FREQUENCY;

    private long mBackoffWindowStartTime = 0L;

    private int mBackoffWindowSendCount = 0;

    private long mEventIntervalFromLogResp = 0L;

    // 进入后台立即上报开关，默认开启，可以通过 app_log 回包关闭
    private boolean mEnterBackgroundSendDisabled = false;

    /**
     * 0->off 1->open, auto upload pageNavigate/click event server version switch compare to local
     * switch {@link InitConfig}.mAutoTrackEnabled
     */
    private int mEnableBav = 1;
    private final AppLogEventFilterConfig blockAndWhiteListConfig;

    public ConfigManager(
            final AppLogInstance appLogInstance, final Context app, final InitConfig config) {
        this.appLogInstance = appLogInstance;
        mApp = app;
        mInitConfig = config;
        mSp =
                SharedPreferenceCacheHelper.getSafeSharedPreferences(
                        mApp, mInitConfig.getSpName(), Context.MODE_PRIVATE);
        mCustomSp =
                SharedPreferenceCacheHelper.getSafeSharedPreferences(
                        mApp,
                        AppLogHelper.getInstanceSpName(appLogInstance, SP_CUSTOM_HEADER),
                        Context.MODE_PRIVATE);
        mSessionSp =
                SharedPreferenceCacheHelper.getSafeSharedPreferences(
                        mApp,
                        AppLogHelper.getInstanceSpName(appLogInstance, SP_SESSION),
                        Context.MODE_PRIVATE);
        blockAndWhiteListConfig = new AppLogEventFilterConfig(mSp, appLogInstance.getLogger());
    }

    public boolean autoStart() {
        return mInitConfig.autoStart();
    }

    public InitConfig getInitConfig() {
        return mInitConfig;
    }

    public boolean isAnonymous() {
        return mInitConfig.getAnonymous();
    }

    public boolean isLocalTest() {
        return mInitConfig.getLocalTest();
    }

    public Account getAccount() {
        return mInitConfig.getAccount();
    }

    public boolean isClearDidAndIid() {
        return mInitConfig.isClearDidAndIid();
    }

    public String getClearKey() {
        return mInitConfig.getClearKey();
    }

    public String getLastDay() {
        return mSessionSp.getString(KEY_SESS_LAST_DAY, "");
    }

    public void setLastDay(final String day, final int order) {
        mSessionSp.edit().putString(KEY_SESS_LAST_DAY, day).putInt(KEY_SESS_ORDER, order).apply();
    }

    public int getIsFirstTimeLaunch() {
        return mSp.getInt(SP_KEY_IS_FIRST_TIME_LAUNCH, 1);
    }

    public void setIsFirstTimeLaunch(final int isFirstTime) {
        mSp.edit().putInt(SP_KEY_IS_FIRST_TIME_LAUNCH, isFirstTime).apply();
    }

    public void setFirstAppLaunch(final boolean isFirstAppLaunch) {
        mSp.edit().putBoolean(SP_KEY_IS_FIRST_APP_LAUNCH, isFirstAppLaunch).apply();
    }

    public boolean isFirstAppLaunch() {
        return mSp.getBoolean(SP_KEY_IS_FIRST_APP_LAUNCH, true);
    }

    public int getSessionOrder() {
        return mSessionSp.getInt(KEY_SESS_ORDER, 0);
    }

    public SharedPreferences getStatSp() {
        return mSp;
    }

    private static final long SEVEN_DAY_IN_SECONDS = 7 * 24 * 60 * 60;

    public boolean isPlayEnable() {
        return mInitConfig.isPlayEnable();
    }

    public JSONObject getConfig() {
        return mConfig;
    }

    /**
     * 从log_setting接口获取配置后保存到本地缓存
     *
     * @param config JSONObject
     */
    public void setConfig(final JSONObject config) {
        appLogInstance
                .getLogger()
                .debug(Collections.singletonList("ConfigManager"), "Set config:{}", config);

        mConfig = config;

        final long current = System.currentTimeMillis();

        Editor editor = mSp.edit();

        final long sessionLife = config.optInt(SP_KEY_SESSION_INTERVAL, 0);
        if (sessionLife > 0 && sessionLife <= SEVEN_DAY_IN_SECONDS) {
            editor.putLong(SP_KEY_SESSION_INTERVAL, sessionLife * 1000L);
        } else {
            editor.remove(SP_KEY_SESSION_INTERVAL);
        }

        final long eventInterval = config.optInt(SP_KEY_EVENT_INTERVAL, 60) * 1000L;
        if (isValidEventInterval(eventInterval)) {
            editor.putLong(SP_KEY_EVENT_INTERVAL, eventInterval);
        } else {
            editor.remove(SP_KEY_EVENT_INTERVAL);
        }

        final int eventSize = config.optInt(SP_KEY_EVENT_SIZE, -1);
        if (isValidEventSize(eventSize)) {
            editor.putInt(SP_KEY_EVENT_SIZE, eventSize);
        } else {
            editor.remove(SP_KEY_EVENT_SIZE);
        }

        final int sendLaunchTimely = config.optInt(SP_KEY_LAUNCH_TIMELY, 0);
        if (sendLaunchTimely > 0 && sendLaunchTimely <= SEVEN_DAY_IN_SECONDS) {
            editor.putInt(SP_KEY_LAUNCH_TIMELY, sendLaunchTimely);
        } else {
            editor.remove(SP_KEY_LAUNCH_TIMELY);
        }

        final long abInterval = config.optInt(KEY_ABTEST_INTERVAL, 0);
        if (abInterval > 20 && abInterval <= SEVEN_DAY_IN_SECONDS) {
            editor.putLong(KEY_ABTEST_INTERVAL, abInterval * 1000L);
        } else {
            editor.remove(KEY_ABTEST_INTERVAL);
        }

        final boolean bavCollect = config.optBoolean(KEY_BAV_ENABLE, isLocalBavEnable());
        editor.putBoolean(KEY_BAV_ENABLE, bavCollect);
        setEnableBav(bavCollect);

        final boolean bavAbConfig = config.optBoolean(KEY_BAV_AB_ENABLE, isLocalABEnable());
        editor.putBoolean(KEY_BAV_AB_ENABLE, bavAbConfig);

        JSONArray realTimeEvents = config.optJSONArray(KEY_REAL_TIME_EVENTS);
        if (realTimeEvents != null && realTimeEvents.length() > 0) {
            editor.putString(KEY_REAL_TIME_EVENTS, realTimeEvents.toString());
        } else {
            editor.remove(KEY_REAL_TIME_EVENTS);
        }
        mRealTimeEvents = null;

        JSONArray sensitiveFields = config.optJSONArray(KEY_SENSITIVE_FIELDS);
        if (sensitiveFields != null && sensitiveFields.length() > 0) {
            editor.putString(KEY_SENSITIVE_FIELDS, sensitiveFields.toString());
        } else {
            editor.remove(KEY_SENSITIVE_FIELDS);
        }

        editor.putLong(KEY_CONFIG_TS, current);

        long configInterval =
                config.optLong(SP_KEY_CONFIG_INTERVAL, INTERVAL_UPDATE_CONFIG_DEFAULT) * 1000L;
        if (configInterval < 30 * 60 * 1000L || configInterval > 48 * 60 * 60 * 1000L) {
            configInterval = INTERVAL_UPDATE_CONFIG_DEFAULT * 1000L;
        }
        editor.putLong(SP_KEY_CONFIG_INTERVAL, configInterval);

        if (config.has(SP_KEY_ENTER_BACKGROUND_NOT_SEND)) {
            boolean enterBackgroundNotSend = config.optInt(SP_KEY_ENTER_BACKGROUND_NOT_SEND) == 1;
            editor.putBoolean(SP_KEY_ENTER_BACKGROUND_NOT_SEND, enterBackgroundNotSend);
        }

        editor.apply();

        sendOriginCachedConfig2DevTools();
    }

    /** 发送缓存的配置信息到devtools */
    public void sendOriginCachedConfig2DevTools() {
        if (getConfigTs() <= 0) {
            return;
        }
        if (!LogUtils.isDisabled()) {
            LogUtils.sendJsonFetcher(
                    "remote_settings",
                    new EventBus.DataFetcher() {
                        @Override
                        public Object fetch() {
                            JSONObject json = new JSONObject();
                            JSONObject cacheConfig = new JSONObject();
                            try {
                                json.put("appId", appLogInstance.getAppId());

                                long V_SP_KEY_SESSION_INTERVAL =
                                        mSp.getLong(SP_KEY_SESSION_INTERVAL, 0);
                                cacheConfig.put(
                                        "后台会话时长",
                                        V_SP_KEY_SESSION_INTERVAL > 0
                                                ? (V_SP_KEY_SESSION_INTERVAL + "ms")
                                                : "--");
                                long V_SP_KEY_EVENT_INTERVAL = mSp.getLong(SP_KEY_EVENT_INTERVAL, 0);
                                cacheConfig.put(
                                        "事件上报周期",
                                        V_SP_KEY_EVENT_INTERVAL > 0
                                                ? (V_SP_KEY_EVENT_INTERVAL + "ms")
                                                : "--");
                                long V_KEY_ABTEST_INTERVAL = mSp.getLong(KEY_ABTEST_INTERVAL, 0);
                                cacheConfig.put(
                                        "AB实验更新周期",
                                        V_KEY_ABTEST_INTERVAL > 0
                                                ? (V_KEY_ABTEST_INTERVAL + "ms")
                                                : "--");
                                cacheConfig.put("全埋点开关", mSp.getBoolean(KEY_BAV_ENABLE, false));
                                cacheConfig.put("AB实验开关", mSp.getBoolean(KEY_BAV_AB_ENABLE, false));
                                cacheConfig.put("实时埋点事件", mSp.getString(KEY_REAL_TIME_EVENTS, "[]"));
                                long V_SP_KEY_CONFIG_INTERVAL = mSp.getLong(SP_KEY_CONFIG_INTERVAL, 0);
                                cacheConfig.put(
                                        "服务端配置更新周期",
                                        V_SP_KEY_CONFIG_INTERVAL > 0
                                                ? (V_SP_KEY_CONFIG_INTERVAL + "ms")
                                                : "--");
                                int eventSize = mSp.getInt(SP_KEY_EVENT_SIZE, -1);
                                cacheConfig.put("事件累计上报数量", (eventSize >= 0 ? eventSize : "--") + "条");
                                cacheConfig.put("禁止采集的敏感字段", mSp.getString(KEY_SENSITIVE_FIELDS, "--"));
                                cacheConfig.put("服务端黑名单事件", mBlockSetV3);
                                json.put("config", cacheConfig);
                            } catch (Throwable ignored) {
                            }
                            return json;
                        }
                    });
        }
    }

    public long getConfigTs() {
        return mSp.getLong(KEY_CONFIG_TS, 0L);
    }

    /**
     * TODO: support realtime event
     *
     * @return Set
     */
    private HashSet<String> getRealTimeEvents() {
        HashSet<String> realTimeEvents = mRealTimeEvents;
        if (realTimeEvents == null) {
            try {
                JSONArray array = new JSONArray(mSp.getString(KEY_REAL_TIME_EVENTS, "[]"));
                final int length = array.length();
                realTimeEvents = new HashSet<>();
                for (int i = 0; i < length; i++) {
                    String event = array.getString(i);
                    if (!TextUtils.isEmpty(event)) {
                        realTimeEvents.add(event);
                    }
                }
            } catch (Throwable t) {
                appLogInstance
                        .getLogger()
                        .error(
                                Collections.singletonList("ConfigManager"),
                                "getRealTimeEvents failed",
                                t);
                realTimeEvents = new HashSet<>();
            }
        }
        mRealTimeEvents = realTimeEvents;
        return realTimeEvents;
    }

    String getAid() {
        return mInitConfig.getAid();
    }

    public String getChannel() {
        String channel = mInitConfig.getChannel();
        if (TextUtils.isEmpty(channel)) {
            channel = getTweakedChannel();
        }
        if (TextUtils.isEmpty(channel)) {
            try {
                ApplicationInfo ai =
                        mApp.getPackageManager()
                                .getApplicationInfo(
                                        mApp.getPackageName(), PackageManager.GET_META_DATA);
                Bundle bundle = ai.metaData;
                channel = bundle.getString("UMENG_CHANNEL");
            } catch (Throwable e) {
                appLogInstance
                        .getLogger()
                        .error(Collections.singletonList("ConfigManager"), "getChannel failed", e);
            }
        }
        return channel;
    }

    public String getLastChannel() {
        return mSp.getString(Api.KEY_CHANNEL, "");
    }

    String getGoogleAID() {
        return mInitConfig.getGoogleAid();
    }

    String getAppLanguageFromInitConfig() {
        return mInitConfig.getLanguage();
    }

    String getAppRegionFromInitConfig() {
        return mInitConfig.getRegion();
    }

    void setCustomInfo(final JSONObject newCustom) {
        mCustomSp
                .edit()
                .putString(CUSTOM_CUSTOM_INFO, newCustom != null ? newCustom.toString() : "")
                .apply();
    }

    String getCustomInfo() {
        return mCustomSp.getString(CUSTOM_CUSTOM_INFO, null);
    }

    void setAbSdkVersion(final String version) {
        mCustomSp.edit().putString(Api.KEY_AB_SDK_VERSION, version).apply();
    }

    String getAbSdkVersion() {
        return mCustomSp.getString(Api.KEY_AB_SDK_VERSION, "");
    }

    void setAppTrack(final String appTrackJson) {
        mCustomSp.edit().putString(CUSTOM_APP_TRACK, appTrackJson).apply();
    }

    void setUserUniqueId(final String id) {
        mCustomSp.edit().putString(Api.KEY_USER_UNIQUE_ID, Utils.toString(id)).apply();
    }

    void setUserUniqueIdType(final String type) {
        mCustomSp.edit().putString(Api.KEY_USER_UNIQUE_ID_TYPE, type).apply();
    }

    public String getUserUniqueId() {
        return mCustomSp.getString(Api.KEY_USER_UNIQUE_ID, "");
    }

    public String getUserUniqueIdType() {
        return mCustomSp.getString(Api.KEY_USER_UNIQUE_ID_TYPE, null);
    }

    public boolean isMainProcess() {
        if (mInitConfig.getProcess() == PROCESS_UNKNOWN) {
            String processName = Utils.getProcessName();
            if (!TextUtils.isEmpty(processName)) {
                mInitConfig.setProcess(
                        processName.contains(":")
                                ? ConfigManager.PROCESS_OTHER
                                : ConfigManager.PROCESS_MAIN);
            } else {
                mInitConfig.setProcess(ConfigManager.PROCESS_UNKNOWN);
            }
        }
        return mInitConfig.getProcess() == PROCESS_MAIN;
    }

    public long getAbInterval() {
        return mSp.getLong(KEY_ABTEST_INTERVAL, 0L);
    }

    void setAbConfig(final JSONObject config) {
        appLogInstance
                .getLogger()
                .debug(Collections.singletonList("ConfigManager"), "setAbConfig:{}", config);

        String abConfigStr = config == null ? "" : config.toString();
        mCustomSp.edit().putString(CUSTOM_AB_CONFIG, abConfigStr).apply();
        mAbConfig = null;

        if (!LogUtils.isDisabled()) {
            // 发送配置
            LogUtils.sendJsonFetcher(
                    "set_abconfig",
                    new EventBus.DataFetcher() {
                        @Override
                        public Object fetch() {
                            JSONObject data = new JSONObject();
                            JSONObject c = new JSONObject();
                            JsonUtils.mergeJsonObject(config, c);
                            try {
                                data.put("appId", appLogInstance.getAppId());
                                data.put("config", c);
                            } catch (Throwable ignored) {

                            }
                            return data;
                        }
                    });
        }
    }

    public JSONObject getAbConfig() {
        JSONObject config = mAbConfig;
        if (config == null) {
            synchronized (this) {
                try {
                    config = new JSONObject(mCustomSp.getString(CUSTOM_AB_CONFIG, ""));
                } catch (Throwable ignore) {
                    // ignore
                }
                if (config == null) {
                    config = new JSONObject();
                }
                mAbConfig = config;
            }
        }
        return config;
    }

    void setExternalAbVersion(final String version) {
        appLogInstance
                .getLogger()
                .debug(
                        Collections.singletonList("ConfigManager"),
                        "setExternalAbVersion:{}",
                        version);

        mCustomSp.edit().putString(EXTERNAL_AB_VERSION, version).apply();
        mExternalAbVersion = null;
    }

    public String getExternalAbVersion() {
        String externalAbVersion = mExternalAbVersion;
        if (TextUtils.isEmpty(externalAbVersion)) {
            synchronized (this) {
                externalAbVersion = mCustomSp.getString(EXTERNAL_AB_VERSION, "");
                mExternalAbVersion = externalAbVersion;
            }
        }
        return externalAbVersion;
    }

    public boolean isAbEnable() {
        return isLocalABEnable() && mSp.getBoolean(KEY_BAV_AB_ENABLE, isLocalABEnable());
    }

    private boolean isLocalABEnable() {
        return mInitConfig.isAbEnable();
    }

    public boolean isBavEnable() {
        return mSp.getBoolean(KEY_BAV_ENABLE, isLocalBavEnable());
    }

    private boolean isLocalBavEnable() {
        return mInitConfig.isAutoTrackEnabled();
    }

    public void setEnableBav(final boolean enableBav) {
        mEnableBav = enableBav ? 1 : 0;
    }

    /**
     * @return And relationship
     */
    public boolean isEnableBavToB() {
        return mEnableBav == 1 && isLocalBavEnable();
    }

    /**
     * when app switch to background, the Session will last for a period of time
     *
     * @return background session life time
     */
    public long getSessionLife() {
        return mSp.getLong(SP_KEY_SESSION_INTERVAL, 30 * 1000);
    }

    public long getEventInterval() {
        if (isValidEventInterval(mEventIntervalFromLogResp)) {
            return mEventIntervalFromLogResp;
        } else {
            return mSp.getLong(SP_KEY_EVENT_INTERVAL, EVENT_INTERVAL_DEFAULT);
        }
    }

    public int getEventSize() {
        return mSp.getInt(SP_KEY_EVENT_SIZE, -1);
    }

    String getAppTrack() {
        return mCustomSp.getString(CUSTOM_APP_TRACK, null);
    }

    String getReleaseBuild() {
        return mInitConfig.getReleaseBuild();
    }

    boolean getNotRequestSender() {
        return mInitConfig.getNotReuqestSender();
    }

    @Nullable
    String getUserAgent() {
        return mSp.getString(SP_KEY_USER_AGENT, null);
    }

    void setUserAgent(final String ua) {
        mSp.edit().putString(SP_KEY_USER_AGENT, ua).apply();
    }

    @Nullable
    String getAppLanguageFromSp() {
        return mSp.getString(SP_KEY_APP_LANGUAGE, null);
    }

    void setAppLanguage(final String language) {
        mSp.edit().putString(SP_KEY_APP_LANGUAGE, language).apply();
    }

    @Nullable
    String getAppRegionFromSp() {
        return mSp.getString(SP_KEY_APP_REGION, null);
    }

    void setAppRegion(final String appRegion) {
        mSp.edit().putString(SP_KEY_APP_REGION, appRegion).apply();
    }

    void setGoogleAid(String googleAid) {
        mSp.edit().putString(SP_KEY_GOOGLE_AID, googleAid).apply();
    }

    /**
     * filter block list events
     *
     * @return whether filter finished
     */
    public boolean filterBlockAndWhite(final List<BaseData> datas, Engine engine) {
        blockAndWhiteListConfig.filterBlockAndWhite(datas, engine);
        return true;
    }

    public void updateLogRespConfig(@NonNull JSONObject resp) {
        mBackoffRatio = resp.optInt(KEY_BACK_OFF_RATIO, 0);
        if (mBackoffRatio < 0 || mBackoffRatio > BACK_OFF_DENOMINATOR) {
            mBackoffRatio = 0;
        }
        int defaultRequestFrequency =
                mBackoffRatio > 0 ? MIN_REQUEST_FREQUENCY : MAX_REQUEST_FREQUENCY;
        mMaxRequestFrequency = resp.optInt(KEY_MAX_REQUEST_FREQUENCY, defaultRequestFrequency);
        if (mMaxRequestFrequency < MIN_REQUEST_FREQUENCY
                || mMaxRequestFrequency > MAX_REQUEST_FREQUENCY) {
            mMaxRequestFrequency = defaultRequestFrequency;
        }
        if (mBackoffRatio > 0 && mBackoffWindowStartTime == 0) {
            mBackoffWindowStartTime = System.currentTimeMillis();
            mBackoffWindowSendCount = 1;
        } else if (mBackoffRatio == 0) {
            mBackoffWindowStartTime = 0;
            mBackoffWindowSendCount = 0;
        }
        mEventIntervalFromLogResp = resp.optLong(SP_KEY_EVENT_INTERVAL, 0) * 1000L;
        mEnterBackgroundSendDisabled = resp.optInt(SP_KEY_ENTER_BACKGROUND_NOT_SEND) == 1;
        appLogInstance
                .getLogger()
                .debug(
                        Collections.singletonList("ConfigManager"),
                        "updateLogRespConfig mBackoffRatio: "
                                + mBackoffRatio
                                + ", mMaxRequestFrequency: "
                                + mMaxRequestFrequency
                                + ", mBackoffWindowStartTime: "
                                + mBackoffWindowStartTime
                                + ", mBackoffWindowSendCount: "
                                + mBackoffWindowSendCount
                                + ", mEventIntervalFromLogResp: "
                                + mEventIntervalFromLogResp);
    }

    String getAppName() {
        return mInitConfig.getAppName();
    }

    int getVersionCode() {
        return mInitConfig.getVersionCode();
    }

    int getUpdateVersionCode() {
        return mInitConfig.getUpdateVersionCode();
    }

    int getManifestVersionCode() {
        return mInitConfig.getManifestVersionCode();
    }

    public String getVersion() {
        return mInitConfig.getVersion();
    }

    String getTweakedChannel() {
        return mInitConfig.getTweakedChannel();
    }

    String getVersionMinor() {
        return mInitConfig.getVersionMinor();
    }

    CharSequence getZiJiePkg() {
        return mInitConfig.getZiJieCloudPkg();
    }

    public boolean isDeferredALinkEnable() {
        return mInitConfig.isDeferredALinkEnabled();
    }

    public boolean isImeiEnable() {
        return mInitConfig.isImeiEnable() && !isInSensitiveFieldsConfig("IMEI");
    }

    public String getAppImei() {
        return mInitConfig.getAppImei();
    }

    public long getConfigInterval() {
        return mSp.getLong(SP_KEY_CONFIG_INTERVAL, INTERVAL_UPDATE_CONFIG_DEFAULT * 1000L);
    }

    public String getSsidSpKey() {
        return Api.KEY_SSID + "_" + mInitConfig.getAid();
    }

    public void updateRegisterTime(long time) {
        mSp.edit().putLong(Api.KEY_REGISTER_TIME, time).apply();
    }

    private boolean isValidEventInterval(long interval) {
        return interval >= MIN_EVENT_INTERVAL && interval <= MAX_EVENT_INTERVAL;
    }

    public boolean isValidEventSize(int size) {
        return size >= MIN_EVENT_SIZE && size <= MAX_EVENT_SIZE;
    }

    public boolean backoffLogRequestAsRatio() {
        if (mBackoffRatio > 0) {
            long curEventInterval = getEventInterval();
            long curTime = System.currentTimeMillis();
            if (curTime < mBackoffWindowStartTime + curEventInterval) {
                if (mBackoffWindowSendCount >= mMaxRequestFrequency) {
                    return true;
                } else {
                    mBackoffWindowSendCount++;
                }
            } else {
                mBackoffWindowStartTime +=
                        (curTime - mBackoffWindowStartTime) / curEventInterval * curEventInterval;
                mBackoffWindowSendCount = 1;
            }
        }
        if (mBackoffRatio >= BACK_OFF_DENOMINATOR) {
            return true;
        } else return mBackoffRatio > 0
                && new Random().nextInt(BACK_OFF_DENOMINATOR) < mBackoffRatio;
    }

    public boolean isClearABCacheOnUserChange() {
        return getInitConfig().isClearABCacheOnUserChange();
    }

    /** 是否开启采集屏幕方向 */
    public boolean isScreenOrientationEnabled() {
        return getInitConfig().isScreenOrientationEnabled();
    }

    /**
     * 是否采集运营商信息，默认运行采集
     *
     * @return true：允许
     */
    public boolean isOperatorInfoEnabled() {
        return getInitConfig().isOperatorInfoEnabled() && !isInSensitiveFieldsConfig(Api.KEY_CARRIER);
    }

    /** 是否开启设备迁移 */
    public boolean isMigrateEnabled() {
        return getInitConfig().isMigrateEnabled();
    }

    boolean isHarmonyEnabled() {
        return getInitConfig().isHarmonyEnabled();
    }

    /**
     * 是否关闭进入后台是否立即上报，默认开启，可以通过 log_settings、app_log 接口返回配置关闭
     * @return 是否关闭进入后台是否立即上报
     */
    public boolean isEnterBackgroundSendDisabled() {
        return mEnterBackgroundSendDisabled || mSp.getBoolean(SP_KEY_ENTER_BACKGROUND_NOT_SEND, false);
    }

    private boolean isInSensitiveFieldsConfig(String sensitiveField) {
        String sensitiveFieldsStr = mSp.getString(KEY_SENSITIVE_FIELDS, "");
        return !TextUtils.isEmpty(sensitiveFieldsStr) && sensitiveFieldsStr.contains(sensitiveField);
    }

    public List<BaseData> filterRealTime(final List<BaseData> datas) {
        List<BaseData> realData = null;
        Iterator<BaseData> iterator = datas.iterator();
        while (iterator.hasNext()) {
            BaseData data = iterator.next();
            if (data instanceof EventV3) {
                EventV3 eventV3 = (EventV3) data;
                if (getRealTimeEvents().contains(eventV3.getEvent())) {
                    iterator.remove();
                    if (realData == null) {
                        realData = new ArrayList<>();
                    }
                    realData.add(data);
                }
            }
        }
        return realData;
    }

    public boolean isAndroidIdEnabled() {
        return mInitConfig.isAndroidIdEnabled() && !isInSensitiveFieldsConfig(Api.KEY_OPEN_UDID);
    }

    public int getGaidTimeOutMilliSeconds() {
        return getInitConfig().getGaidTimeOutMilliSeconds();
    }

    public void parseEventConfigList(JSONObject o) {
        blockAndWhiteListConfig.parseEventConfigList(o);
    }
}
