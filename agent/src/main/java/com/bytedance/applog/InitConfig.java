// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import android.accounts.Account;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bytedance.applog.exposure.ViewExposureConfig;
import com.bytedance.applog.manager.ConfigManager;
import com.bytedance.applog.network.INetworkClient;
import com.bytedance.applog.util.Assert;
import com.bytedance.applog.util.Utils;

import java.util.List;
import java.util.Map;

/**
 * 此接口，都是初始化参数，在初始化时传入，运行时不再修改。 <br>
 * 另：运行时才能设置的参数，请调用{@link AppLog}的中的接口。 <br>
 * 如登录后才能获取的userUniqueId，请调用{@link AppLog#setUserUniqueID(String)}
 *
 * @author shiyanlong
 * @date 2019/1/10
 */
public class InitConfig {
    private static final String DEFAULT_DB_NAME = "bd_tea_agent.db";

    private String mAid;

    private boolean mAutoStart = true;

    private String mChannel;

    private String mClearKey;

    private IEncryptor mEncrytor;

    private String mGaid;

    private String mLanguage;

    private ILogger mLogger;

    private String mRegion;

    private String mAliyunUdid;

    private IPicker mPicker;

    private boolean mAnonymous;

    private boolean mLocalTest = false;

    private boolean mEnablePlay;

    private int mProcess = ConfigManager.PROCESS_UNKNOWN;

    private String mReleaseBuild;

    private boolean mNotRequestSender;

    private String mAppName;

    private UriConfig mUriConfig;

    private String mVersion;

    private String mTweakedChannel;

    private int mVersionCode;

    private int mUpdateVersionCode;

    private int mManifestVersionCode;

    private String mManifestVersion;

    private String mVersionMinor;

    private String mZiJieCloudPkg;

    private Map<String, Object> mCommonHeader;

    private Account mAccount;

    private boolean mClearDidAndIid;

    private INetworkClient mWrapperClient;

    /** 是否自动激活 */
    private boolean mAutoActive = true;

    /** 是否在后台时禁用部分worker */
    private boolean mSilenceInBackground;

    /** 开发者手动设置是否启动AB测试 */
    private boolean mAbEnable = false;

    /**
     * report the event of page opening or changing , default true for reporting only work when both
     * server switch(log setting) and mAutoTrackEnabled set to true
     */
    private boolean mAutoTrackEnabled = true;

    /** 是否自动处理activity的生命周期,自动产生page 对内重构版为 false，内部app多次调用生命周期方法，与自动处理相比，日活有差异 tob版为 true */
    private boolean mHandleLifeCycle = true;

    private boolean mCongestionControlEnable = true;

    private String mDbName;

    private String mSpName;

    private ISensitiveInfoProvider mSensitiveInfoProvider;

    private boolean mMacEnable = true;

    private boolean mImeiEnable = true;

    /** h5打通开关：默认关闭 */
    private boolean mH5BridgeEnable = false;

    /** h5的全埋点开关：默认关闭 */
    private boolean mH5CollectEnable = false;

    /** 日志开关：默认关闭 */
    private boolean mLogEnable = false;

    /** h5 bridge允许路由url的规则列表：'*'表示'.*'正则，'.'表示实际的'.'符号 */
    private List<String> mH5BridgeAllowlist;

    /** h5 bridge允许所有的路由url（优先级高于{@link mH5BridgeAllowlist}） */
    private boolean mH5BridgeAllowAll = false;

    private boolean mDeferredALinkEnabled = false;

    private boolean mClearABCacheOnUserChange = true;

    /** imei set by app, if mImeiEnable is false, use this value */
    private String mAppImei;

    private boolean mEventFilterEnable;

    private IpcDataChecker mIpcDataChecker = null;

    /** 初始化的UUID */
    private String mUserUniqueId = null;

    /** 初始化的UUID类型 */
    private String mUserUniqueIdType = null;

    /** 埋点采集开关，默认开 */
    private boolean trackEventEnabled = true;

    /** 是否开启鸿蒙设备采集 */
    private boolean harmonyEnabled = false;

    /** 采集Fragment事件，默认关闭 */
    private boolean autoTrackFragmentEnabled = false;

    /** 安全开关：反作弊 */
    private boolean metaSecEnabled = true;

    /** 采集OAID，默认开启 */
    private boolean oaidEnabled = true;

    /** 采集Android ID，默认开启 */
    private boolean androidIdEnabled = true;

    /** 曝光事件采集开关，默认关闭 */
    private boolean exposureEnabled = false;

    /** 监控开关，默认关闭 */
    private boolean monitorEnabled = false;

    /** 曝光事件采集的全局配置 */
    private ViewExposureConfig exposureConfig = null;

    /** 采集屏幕方向开关，默认关闭 */
    private boolean screenOrientationEnabled = false;

    /** 采集页面离开事件，默认关闭 */
    private boolean trackPageLeaveEnabled = false;

    /** 采集运营商信息开关，默认开启 */
    private boolean operatorInfoEnabled = true;

    public InitConfig(@NonNull final String aid, @NonNull final String channel) {
        Assert.notEmpty(aid, "App id must not be empty!");
        Assert.notEmpty(channel, "Channel must not be empty!");
        mAid = aid;
        mChannel = channel;
    }

    public InitConfig setLogger(ILogger logger) {
        mLogger = logger;
        return this;
    }

    public ILogger getLogger() {
        return mLogger;
    }

    public boolean autoStart() {
        return mAutoStart;
    }

    public InitConfig setAutoStart(final boolean autoStart) {
        mAutoStart = autoStart;
        return this;
    }

    public void setChannel(@NonNull String channel) {
        mChannel = channel;
    }

    /**
     * 设置一个参数
     *
     * @param releaseBuild 参数
     * @return this
     */
    @NonNull
    public InitConfig setReleaseBuild(final String releaseBuild) {
        mReleaseBuild = releaseBuild;
        return this;
    }

    public String getReleaseBuild() {
        return mReleaseBuild;
    }

    public InitConfig setNotRequestSender(boolean notRequestSender) {
        mNotRequestSender = notRequestSender;
        return this;
    }

    public boolean getNotReuqestSender() {
        return mNotRequestSender;
    }

    /** 对齐内部版,可在header添加自定义的字段 */
    public InitConfig putCommonHeader(Map<String, Object> commonHeader) {
        this.mCommonHeader = commonHeader;
        return this;
    }

    public Map<String, Object> getCommonHeader() {
        return mCommonHeader;
    }

    /**
     * 启用PlaySession：前台时，每分钟记录play_session事件。
     *
     * @param enable 是否启用
     * @return this
     */
    @NonNull
    public InitConfig setEnablePlay(final boolean enable) {
        mEnablePlay = enable;
        return this;
    }

    public boolean isPlayEnable() {
        return mEnablePlay;
    }

    public String getAid() {
        return mAid;
    }

    public String getChannel() {
        return mChannel;
    }

    /**
     * 设置google id。可选参数。 建议国际化App设置google ad. id。
     *
     * @param gaid google ad. id
     * @return this
     */
    @NonNull
    public InitConfig setGoogleAid(final String gaid) {
        mGaid = gaid;
        return this;
    }

    /**
     * {@link #setGoogleAid(String)}
     *
     * @return google ad. id
     * @hide
     */
    public String getGoogleAid() {
        return mGaid;
    }

    /**
     * 设置语言。可选参数。 建议国际化App设置语言。
     *
     * @param language 语言。（应该是"zh-cn"等标识符吧）
     * @return this
     */
    @NonNull
    public InitConfig setLanguage(final String language) {
        mLanguage = language;
        return this;
    }

    /**
     * {@link #setLanguage(String)}
     *
     * @return 语言
     * @hide
     */
    public String getLanguage() {
        return mLanguage;
    }

    /**
     * 设置区域。可选参数。 建议国际化App设置区域。
     *
     * @param region 区域。（应该是"cn"等标识符吧。
     * @return this
     */
    @NonNull
    public InitConfig setRegion(final String region) {
        mRegion = region;
        return this;
    }

    /**
     * {@link #setRegion(String)}
     *
     * @return 区域
     * @hide
     */
    public String getRegion() {
        return mRegion;
    }

    /**
     * 设置阿里云的udid
     *
     * @param aliyunUdid aliyunUdid
     */
    public InitConfig setAliyunUdid(final String aliyunUdid) {
        mAliyunUdid = aliyunUdid;
        return this;
    }

    /**
     * 获取阿里云的udid
     *
     * @return aliyunUdid
     */
    public String getAliyunUdid() {
        return mAliyunUdid;
    }

    /**
     * 设置当前进程类型
     *
     * <p>0：系统根据进程名判断是否为主进程：进程名包含":"为子进程；不含为主进程。
     *
     * <p>1：设置当前为主进程
     *
     * <p>2：设置当前未子进程
     *
     * <p>主进程负责记录和上报；子进程，仅负责记录，不负责上报。
     *
     * @param process 进程类型
     * @return 返回自己
     */
    @NonNull
    public InitConfig setProcess(int process) {
        mProcess = process;
        return this;
    }

    /**
     * 将当前进程设为主进程：所有采集和上报均来自当前进程
     *
     * @return InitConfig
     */
    public InitConfig setMainProcess() {
        mProcess = ConfigManager.PROCESS_MAIN;
        return this;
    }

    /**
     * 获取当前进程标识，是否为主进程。
     *
     * @return 参考{@link ConfigManager#PROCESS_MAIN}等
     * @hide 内部用，外部无需关注。
     */
    public int getProcess() {
        return mProcess;
    }

    /**
     * {@link #setPicker(IPicker)}
     *
     * @return 圈选器
     * @hide
     */
    public IPicker getPicker() {
        return mPicker;
    }

    /**
     * 设置圈选器。请查阅"圈选版SDK"的文档
     *
     * @param picker 圈选器
     * @hide
     */
    @NonNull
    public InitConfig setPicker(IPicker picker) {
        mPicker = picker;
        return this;
    }

    public InitConfig setUriConfig(UriConfig config) {
        mUriConfig = config;
        return this;
    }

    public UriConfig getUriConfig() {
        return mUriConfig;
    }

    public InitConfig setEncryptor(final IEncryptor encryptor) {
        mEncrytor = encryptor;
        return this;
    }

    public IEncryptor getEncryptor() {
        return mEncrytor;
    }

    /** 设置anonymous */
    public InitConfig setAnonymous(boolean anonymous) {
        mAnonymous = anonymous;
        return this;
    }

    public boolean getAnonymous() {
        return mAnonymous;
    }

    /** 设置是否为内测包，内测包会生成不同did（慎用！！！） */
    public InitConfig setLocalTest(boolean isLocalTest) {
        mLocalTest = isLocalTest;
        return this;
    }

    public boolean getLocalTest() {
        return mLocalTest;
    }

    /** 设置 */
    public InitConfig setAccount(Account account) {
        mAccount = account;
        return this;
    }

    public Account getAccount() {
        return mAccount;
    }

    /**
     * 清除设备 did 和 iid 信息，需要要{@link AppLog#init(Context, InitConfig)} 前调用。
     *
     * @param clearKey 一个key，不能是null,也不能是"",每个key会清除device_id和install_id一次，
     *     当用这个key清除过一次device_id和install_id之后，会用sp保存这个key，appLog就
     *     知道这个key已经被用过一次了，当下一次再用相同的key的时候调用这个方法的时候，就不会再有清除device_id和install_id的动作了。
     *     <p>注意：业务方不用当心这个key可能和我们的sp已经有的存储其它信息的key重复， 我们在底层会为这个key再加一个前缀作为真正的sp的key。
     */
    public void clearDidAndIid(String clearKey) {
        mClearDidAndIid = true;
        mClearKey = clearKey;
    }

    /** 判断当前是否需要清除手机的Did以及Iid信息 */
    public boolean isClearDidAndIid() {
        return mClearDidAndIid;
    }

    /**
     * 获得一个key，不能是null,也不能是"",每个key会清除device_id和install_id一次，
     * 当用这个key清除过一次device_id和install_id之后，会用sp保存这个key，appLog就
     * 知道这个key已经被用过一次了，当下一次再用相同的key的时候调用这个方法的时候，就不会再有清除device_id和install_id的动作了。
     */
    public String getClearKey() {
        return mClearKey;
    }

    /**
     * 使用者可通过该接口设置自定义的NetworkClient 已默认内置了一个实现，不调用该接口，sdk功能也是正常的
     * 注意：sdk内部是compileOnly的'com.bytedance.frameworks.baselib:utility:1.4.8' 使用该接口，请确保自己引入了对应的库
     *
     * @param client 自定义的NetworkClient
     */
    public InitConfig setNetworkClient(final INetworkClient client) {
        mWrapperClient = client;
        return this;
    }

    public INetworkClient getNetworkClient() {
        return mWrapperClient;
    }

    /**
     * 请设置为应用云平台注册的appname，否则存在命中后台风控风险
     *
     * @param appName 应用云平台注册的appname
     */
    public InitConfig setAppName(String appName) {
        mAppName = appName;
        return this;
    }

    public String getAppName() {
        return mAppName;
    }

    /**
     * 设置版本号，该字段会在header中出现，还会用在设备注册等多个API的请求url的param中
     *
     * @param version 版本
     */
    public InitConfig setVersion(String version) {
        mVersion = version;
        return this;
    }

    public String getVersion() {
        return mVersion;
    }

    /**
     * 设置tweaked channel，用在设备注册等多个API的请求url的param中 不出现在header中
     *
     * @param tweakedChannel 版本
     */
    public InitConfig setTweakedChannel(String tweakedChannel) {
        mTweakedChannel = tweakedChannel;
        return this;
    }

    public String getTweakedChannel() {
        return mTweakedChannel;
    }

    /**
     * 设置版本号Code，该字段会在header中出现 还会用在设备注册等多个API的请求url的param中
     *
     * @param versionCode 版本
     */
    public InitConfig setVersionCode(int versionCode) {
        mVersionCode = versionCode;
        return this;
    }

    public int getVersionCode() {
        return mVersionCode;
    }

    /**
     * 设置userUniqueId，用在设备注册等多个API的请求url的param(userUniqueId)中 不出现在header里
     *
     * @param userUniqueId
     * @return
     */
    public InitConfig setUserUniqueId(String userUniqueId) {
        mUserUniqueId = userUniqueId;
        return this;
    }

    public String getUserUniqueId() {
        return mUserUniqueId;
    }

    public InitConfig setUserUniqueIdType(String type) {
        mUserUniqueIdType = type;
        return this;
    }

    public String getUserUniqueIdType() {
        return mUserUniqueIdType;
    }

    /**
     * 设置update版本号Code，该字段会在header中出现 还会用在设备注册等多个API的请求url的param中
     *
     * @param updateVersionCode update版本
     */
    public InitConfig setUpdateVersionCode(int updateVersionCode) {
        mUpdateVersionCode = updateVersionCode;
        return this;
    }

    public int getUpdateVersionCode() {
        return mUpdateVersionCode;
    }

    /**
     * 设置manifest版本号Code，该字段会在header中出现 还会用在设备注册等多个API的请求url的param中
     *
     * @param manifestVersionCode manifest版本
     */
    public InitConfig setManifestVersionCode(int manifestVersionCode) {
        mManifestVersionCode = manifestVersionCode;
        return this;
    }

    public int getManifestVersionCode() {
        return mManifestVersionCode;
    }

    /**
     * 设置manifest版本号，该字段会在header中出现 还会用在设备注册等多个API的请求url的param中
     *
     * @param manifestVersion manifest版本
     */
    public InitConfig setManifestVersion(String manifestVersion) {
        mManifestVersion = manifestVersion;
        return this;
    }

    // FIXME: 2020-02-07 这个接口没有人调用...?
    public String getManifestVersion() {
        return mManifestVersion;
    }

    /**
     * @param versionMinor versionMinor
     */
    public InitConfig setVersionMinor(String versionMinor) {
        this.mVersionMinor = versionMinor;
        return this;
    }

    public String getVersionMinor() {
        return mVersionMinor;
    }

    /** 设置字节云注册的package */
    public InitConfig setZiJieCloudPkg(String ziJieCloudPkg) {
        this.mZiJieCloudPkg = ziJieCloudPkg;
        return this;
    }

    public String getZiJieCloudPkg() {
        return mZiJieCloudPkg;
    }

    public boolean isSilenceInBackground() {
        return mSilenceInBackground;
    }

    public void setSilenceInBackground(final boolean silenceInBackground) {
        mSilenceInBackground = silenceInBackground;
    }

    public boolean isAutoActive() {
        return mAutoActive;
    }

    public void setAutoActive(final boolean autoActive) {
        mAutoActive = autoActive;
    }

    public boolean isAbEnable() {
        return mAbEnable;
    }

    public void setAbEnable(final boolean enable) {
        mAbEnable = enable;
    }

    public void setAutoTrackEnabled(boolean autoTrackEnabled) {
        mAutoTrackEnabled = autoTrackEnabled;
    }

    public boolean isAutoTrackEnabled() {
        return mAutoTrackEnabled;
    }

    public boolean isHandleLifeCycle() {
        return mHandleLifeCycle;
    }

    public void setHandleLifeCycle(final boolean handle) {
        mHandleLifeCycle = handle;
    }

    public boolean isCongestionControlEnable() {
        return mCongestionControlEnable;
    }

    public void setCongestionControlEnable(final boolean congestEnable) {
        mCongestionControlEnable = congestEnable;
    }

    /**
     * 获取存储数据的数据库文件名
     *
     * @return String
     */
    public String getDbName() {
        if (TextUtils.isEmpty(mDbName)) {
            // 默认 {appId}@DEFAULT_DB_NAME
            return Utils.toString(mAid) + "@" + DEFAULT_DB_NAME;
        }
        return mDbName;
    }

    /** 设置存储DB名 */
    public InitConfig setDbName(String dbName) {
        if (!TextUtils.isEmpty(dbName)) {
            mDbName = dbName;
        }
        return this;
    }

    public String getSpName() {
        return mSpName;
    }

    /** 设置保存基本信息的SP名称 */
    public InitConfig setSpName(String spName) {
        if (!TextUtils.isEmpty(spName)) {
            mSpName = spName;
        }
        return this;
    }

    public ISensitiveInfoProvider getSensitiveInfoProvider() {
        return mSensitiveInfoProvider;
    }

    public void setSensitiveInfoProvider(final ISensitiveInfoProvider sensitiveInfoProvider) {
        mSensitiveInfoProvider = sensitiveInfoProvider;
    }

    public boolean isMacEnable() {
        return mMacEnable;
    }

    public boolean isDeferredALinkEnabled() {
        return mDeferredALinkEnabled;
    }

    public boolean isH5BridgeEnable() {
        return mH5BridgeEnable;
    }

    public boolean isH5CollectEnable() {
        return mH5CollectEnable;
    }

    public boolean isLogEnable() {
        return mLogEnable;
    }

    @Nullable
    public List<String> getH5BridgeAllowlist() {
        return mH5BridgeAllowlist;
    }

    public boolean isH5BridgeAllowAll() {
        return mH5BridgeAllowAll;
    }

    public void setMacEnable(final boolean macEnable) {
        mMacEnable = macEnable;
    }

    public InitConfig setH5BridgeEnable(boolean mH5BridgeEnable) {
        this.mH5BridgeEnable = mH5BridgeEnable;
        return this;
    }

    public InitConfig setH5CollectEnable(boolean mH5CollectEnable) {
        this.mH5CollectEnable = mH5CollectEnable;
        return this;
    }

    public InitConfig setLogEnable(boolean logEnable) {
        this.mLogEnable = logEnable;
        return this;
    }

    public InitConfig clearABCacheOnUserChange(boolean isClear) {
        this.mClearABCacheOnUserChange = isClear;
        return this;
    }

    public InitConfig setH5BridgeAllowlist(List<String> h5BridgeAllowlist) {
        this.mH5BridgeAllowlist = h5BridgeAllowlist;
        return this;
    }

    public InitConfig setH5BridgeAllowAll(boolean allow) {
        this.mH5BridgeAllowAll = allow;
        return this;
    }

    public boolean isClearABCacheOnUserChange() {
        return mClearABCacheOnUserChange;
    }

    public InitConfig disableDeferredALink() {
        this.mDeferredALinkEnabled = false;
        return this;
    }

    public InitConfig enableDeferredALink() {
        this.mDeferredALinkEnabled = true;
        return this;
    }

    public boolean isImeiEnable() {
        return mImeiEnable;
    }

    public InitConfig setImeiEnable(final boolean imeiEnable) {
        mImeiEnable = imeiEnable;
        return this;
    }

    public String getAppImei() {
        return mAppImei;
    }

    public void setAppImei(final String appImei) {
        mAppImei = appImei;
    }

    public boolean isEventFilterEnable() {
        return mEventFilterEnable;
    }

    public void setEventFilterEnable(boolean enable) {
        mEventFilterEnable = enable;
    }

    /**
     * callback the checker when the ipc-data's size is over 300k
     *
     * @param checker
     * @return
     */
    public InitConfig setIpcDataChecker(IpcDataChecker checker) {
        mIpcDataChecker = checker;
        return this;
    }

    public IpcDataChecker getIpcDataChecker() {
        return mIpcDataChecker;
    }

    public boolean isTrackEventEnabled() {
        return trackEventEnabled;
    }

    public void setTrackEventEnabled(boolean trackEventEnabled) {
        this.trackEventEnabled = trackEventEnabled;
    }

    public boolean isAutoTrackFragmentEnabled() {
        return this.autoTrackFragmentEnabled;
    }

    public void setAutoTrackFragmentEnabled(boolean enabled) {
        this.autoTrackFragmentEnabled = enabled;
    }

    /**
     * 设置是否开启鸿蒙支持
     *
     * @param enabled 开启
     */
    public void setHarmonyEnable(boolean enabled) {
        this.harmonyEnabled = enabled;
    }

    public boolean isHarmonyEnabled() {
        return this.harmonyEnabled;
    }

    /**
     * 设置反作弊安全开关
     *
     * @param enabled 默认开启
     */
    public void setMetaSecEnabled(boolean enabled) {
        this.metaSecEnabled = enabled;
    }

    public boolean isMetaSecEnabled() {
        return this.metaSecEnabled;
    }

    /**
     * 设置OAID采集开关
     *
     * @param enabled 打开
     */
    public void setOaidEnabled(boolean enabled) {
        this.oaidEnabled = enabled;
    }

    public boolean isOaidEnabled() {
        return this.oaidEnabled;
    }

    /**
     * 设置AndroidId采集开关
     *
     * @param enabled 打开
     */
    public void setAndroidIdEnabled(boolean enabled) {
        this.androidIdEnabled = enabled;
    }

    public boolean isAndroidIdEnabled() {
        return this.androidIdEnabled;
    }

    /** 设置屏幕方向采集开关 */
    public void setScreenOrientationEnabled(boolean enabled) {
        this.screenOrientationEnabled = enabled;
    }

    public boolean isScreenOrientationEnabled() {
        return this.screenOrientationEnabled;
    }

    /** 设置采集页面离开事件开关 */
    public void setTrackPageLeaveEnabled(boolean enabled) {
        this.trackPageLeaveEnabled = enabled;
    }

    public boolean isTrackPageLeaveEnabled() {
        return this.trackPageLeaveEnabled;
    }

    /**
     * 设置曝光事件采集开关
     *
     * @param exposureEnabled 是否开启
     */
    public void setExposureEnabled(boolean exposureEnabled) {
        this.exposureEnabled = exposureEnabled;
    }

    public boolean isExposureEnabled() {
        return exposureEnabled;
    }

    /**
     * 设置是否打开监控开关
     *
     * @param monitorEnabled 监控开关
     */
    public void setMonitorEnabled(boolean monitorEnabled) {
        this.monitorEnabled = monitorEnabled;
    }

    public boolean isMonitorEnabled() {
        return this.monitorEnabled;
    }

    /**
     * 设置是否采集运营商信息
     *
     * @param enabled 采集
     */
    public void setOperatorInfoEnabled(boolean enabled) {
        this.operatorInfoEnabled = enabled;
    }

    public boolean isOperatorInfoEnabled() {
        return this.operatorInfoEnabled;
    }

    public ViewExposureConfig getExposureConfig() {
        return exposureConfig;
    }

    /**
     * 曝光事件采集的全局配置
     *
     * @param exposureConfig
     */
    public void setExposureConfig(ViewExposureConfig exposureConfig) {
        this.exposureConfig = exposureConfig;
    }

    public interface IpcDataChecker {
        boolean checkIpcData(String[] strings);
    }
}
