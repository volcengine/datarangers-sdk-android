// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import android.accounts.Account;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bytedance.applog.alink.IALinkListener;
import com.bytedance.applog.event.IEventHandler;
import com.bytedance.applog.exposure.ViewExposureManager;
import com.bytedance.applog.manager.ConfigManager;
import com.bytedance.applog.network.INetworkClient;
import com.bytedance.applog.profile.UserProfileCallback;
import com.bytedance.applog.store.AccountCacheHelper;
import com.bytedance.applog.util.Assert;
import com.bytedance.applog.util.TLog;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author shiyanlong
 * @date 2019/1/10
 */
public final class AppLog {

    /**
     * 全局的AppLog实例，兼容老版本接口
     */
    private static final IAppLogInstance gAppLogInstance = newInstance();

    /**
     * 全局实例是否初始化的标志位
     */
    private static volatile boolean gAppLogInstanceInitialized = false;

    public static IAppLogInstance getInstance() {
        return gAppLogInstance;
    }

    public static Context getContext() {
        return gAppLogInstance.getContext();
    }

    public static void setAppContext(IAppContext appContext) {
        gAppLogInstance.setAppContext(appContext);
    }

    public static IAppContext getAppContext() {
        return gAppLogInstance.getAppContext();
    }

    /**
     * 新建一个IAppLogInstance实例
     *
     * @return IAppLogInstance
     */
    public static IAppLogInstance newInstance() {
        return new AppLogInstance();
    }

    /**
     * 初始化，必须在第一个Activity启动之前调用。
     *
     * @param context context
     * @param config  参考{@link InitConfig}
     */
    public static void init(@NonNull final Context context, @NonNull final InitConfig config) {
        synchronized (AppLog.class) {
            if (Assert.f(
                    gAppLogInstanceInitialized,
                    "Default AppLog is initialized, please create another instance "
                            + "by `AppLog.newInstance()`"
                            + ".")) {
                return;
            }
            gAppLogInstanceInitialized = true;

            // 兼容
            if (TextUtils.isEmpty(config.getSpName())) {
                config.setSpName(ConfigManager.SP_FILE);
            }

            gAppLogInstance.init(context, config);
        }
    }

    /**
     * 初始化，在Activity启动后调用。
     *
     * @param context  context
     * @param config   参考{@link InitConfig}
     * @param activity 启动的activity
     */
    public static void init(
            @NonNull final Context context, @NonNull final InitConfig config, Activity activity) {
        synchronized (AppLog.class) {
            if (Assert.f(
                    gAppLogInstanceInitialized,
                    "Default AppLog is initialized, please create another instance "
                            + "by `new AppLogInstance()`"
                            + ".")) {
                return;
            }
            gAppLogInstanceInitialized = true;

            // 兼容
            if (TextUtils.isEmpty(config.getSpName())) {
                config.setSpName(ConfigManager.SP_FILE);
            }

            gAppLogInstance.init(context, config, activity);
        }
    }

    /**
     * 启动。请在获得权限之后再调用start
     */
    public static void start() {
        gAppLogInstance.start();
    }

    public static boolean hasStarted() {
        return gAppLogInstance.hasStarted();
    }

    public static InitConfig getInitConfig() {
        return gAppLogInstance.getInitConfig();
    }

    /**
     * 强制将内存的事件存储到DB中，同步执行 <br>
     * 当发生crash时，请调用此接口。
     */
    public static void flush() {
        gAppLogInstance.flush();
    }

    public static boolean isH5BridgeEnable() {
        return gAppLogInstance.isH5BridgeEnable();
    }

    public static boolean isH5CollectEnable() {
        return gAppLogInstance.isH5CollectEnable();
    }

    /**
     * 设置当前用户的唯一id； <br>
     * 每个事件，都会以'user_id'为键带上这个字段
     *
     * @param id 当前用户的唯一id
     */
    public static void setUserID(final long id) {
        gAppLogInstance.setUserID(id);
    }

    public static void setAppLanguageAndRegion(String language, String region) {
        gAppLogInstance.setAppLanguageAndRegion(language, region);
    }

    public static void setGoogleAid(String gaid) {
        gAppLogInstance.setGoogleAid(gaid);
    }

    public static String addNetCommonParams(
            Context context, String url, boolean isApi, Level level) {
        return gAppLogInstance.addNetCommonParams(context, url, isApi, level);
    }

    public static void putCommonParams(
            Context context, Map<String, String> params, boolean isApi, Level level) {
        gAppLogInstance.putCommonParams(context, params, isApi, level);
    }

    public static void setUserUniqueID(final String id) {
        gAppLogInstance.setUserUniqueID(id);
    }

    public static void setUserUniqueID(final String id, final String type) {
        gAppLogInstance.setUserUniqueID(id, type);
    }

    /**
     * 设置用于添加自定义url params的接口，能够覆盖默认header值
     */
    public static void setExtraParams(IExtraParams iExtraParams) {
        gAppLogInstance.setExtraParams(iExtraParams);
    }

    /**
     * 设置用于激活接口添加自定义字段的接口
     */
    public static void setActiveCustomParams(IActiveCustomParamsCallback callback) {
        gAppLogInstance.setActiveCustomParams(callback);
    }

    /**
     * 查询激活的自定义参数接口
     */
    public static IActiveCustomParamsCallback getActiveCustomParams() {
        return gAppLogInstance.getActiveCustomParams();
    }

    /**
     * add touch point header
     */
    public static void setTouchPoint(String touchPoint) {
        gAppLogInstance.setTouchPoint(touchPoint);
    }

    /**
     * 添加自定义上报信息； <br>
     * 设置一次即可，内部会存储。 <br>
     * 最终会以json格式放到AppLog的custom字段中。
     *
     * @param custom 用户设置的上报信息，Object需是JSON兼容的数据类型
     */
    public static void setHeaderInfo(final HashMap<String, Object> custom) {
        gAppLogInstance.setHeaderInfo(custom);
    }

    /**
     * add or update the single key-value in custom header
     */
    public static void setHeaderInfo(final String key, final Object value) {
        gAppLogInstance.setHeaderInfo(key, value);
    }

    /**
     * remove the single key-value in custom header
     */
    public static void removeHeaderInfo(final String key) {
        gAppLogInstance.removeHeaderInfo(key);
    }

    /**
     * set the ab_sdk_version
     */
    public static void setExternalAbVersion(final String version) {
        gAppLogInstance.setExternalAbVersion(version);
    }

    /**
     * 获取ab_sdk_version
     */
    public static String getAbSdkVersion() {
        return gAppLogInstance.getAbSdkVersion();
    }

    /**
     * 获取AB测试的配置 返回配置的同时，会记录该key对应的vid到已曝光区域 简而言之，调用后，就表明该key对应的实验已曝光
     *
     * @param key          配置项的健
     * @param defaultValue 默认值
     * @param <T>          类型，需是JSON兼容的数据类型
     * @return 配置项的值
     */
    @Nullable
    public static <T> T getAbConfig(String key, T defaultValue) {
        return gAppLogInstance.getAbConfig(key, defaultValue);
    }

    /**
     * 拉取AB实验的配置
     */
    public static void pullAbTestConfigs() {
        gAppLogInstance.pullAbTestConfigs();
    }

    /**
     * 设置拉取ab实验间隔毫秒数
     *
     * @param mills 毫秒
     */
    private static void setPullAbTestConfigsThrottleMills(Long mills) {
        gAppLogInstance.setPullAbTestConfigsThrottleMills(mills);
    }

    /**
     * Use {@link AppLog#getAppId()} instead
     */
    @Deprecated
    public static String getAid() {
        return gAppLogInstance.getAid();
    }

    public static String getAppId() {
        return gAppLogInstance.getAppId();
    }

    public static <T> T getHeaderValue(String key, T fallbackValue, Class<T> tClass) {
        return gAppLogInstance.getHeaderValue(key, fallbackValue, tClass);
    }

    public static void setTracerData(JSONObject tracerData) {
        gAppLogInstance.setTracerData(tracerData);
    }

    /**
     * 设置UA <br>
     * 设置一次即可，内部会存储。
     *
     * @param ua ua
     */
    public static void setUserAgent(final String ua) {
        gAppLogInstance.setUserAgent(ua);
    }

    public static void onEventV3(@NonNull final String event) {
        gAppLogInstance.onEventV3(event);
    }

    /**
     * 上报v3事件。
     *
     * @param event  事件名，应用内需唯一
     * @param params 事件参数
     */
    public static void onEventV3(@NonNull final String event, @Nullable final JSONObject params) {
        gAppLogInstance.onEventV3(event, params);
    }

    /**
     * 上报v3事件。
     *
     * @param event  事件名，应用内需唯一
     * @param params 事件参数
     */
    public static void onEventV3(
            @NonNull final String event, @Nullable final JSONObject params, int eventType) {
        gAppLogInstance.onEventV3(event, params, eventType);
    }

    /**
     * 上报v3事件
     *
     * @param event  事件名，应用内需唯一。
     * @param params 事件参数
     */
    public static void onEventV3(
            @NonNull final String event, @Nullable final Bundle params, int eventType) {
        gAppLogInstance.onEventV3(event, params, eventType);
    }

    /**
     * 上报v3事件
     *
     * @param event  事件名，应用内需唯一。
     * @param params 事件参数
     */
    public static void onEventV3(@NonNull final String event, @Nullable final Bundle params) {
        gAppLogInstance.onEventV3(event, params);
    }

    /**
     * 点播SDK的特殊event
     */
    public static void onMiscEvent(@NonNull String logType, @Nullable JSONObject obj) {
        gAppLogInstance.onMiscEvent(logType, obj);
    }

    /**
     * 是否加密压缩上报数据，只在debug版可用，release版本不可设置
     */
    public static void setEncryptAndCompress(final boolean enable) {
        gAppLogInstance.setEncryptAndCompress(enable);
    }

    /**
     * 返回是否加密压缩上报数据，只在debug版可用，release版固定返回true
     */
    public static boolean getEncryptAndCompress() {
        return gAppLogInstance.getEncryptAndCompress();
    }

    /**
     * 返回后台生成的did，如果没有，返回""
     */
    public static String getDid() {
        return gAppLogInstance.getDid();
    }

    /**
     * 返回客户端生成的udid，如果没有，返回"" global版本可能为空
     */
    public static String getUdid() {
        return gAppLogInstance.getUdid();
    }

    /**
     * 添加session变化回调 sdk内部通过weak reference持有，不影响业务接口回收
     */
    public static void addSessionHook(ISessionObserver hook) {
        gAppLogInstance.addSessionHook(hook);
    }

    /**
     * 移除session变化回调
     */
    public static void removeSessionHook(ISessionObserver hook) {
        gAppLogInstance.removeSessionHook(hook);
    }

    /**
     * 设置埋点事件回调 sdk内部通过weak reference持有，不影响业务接口回收
     */
    public static void addEventObserver(IEventObserver iEventObserver) {
        gAppLogInstance.addEventObserver(iEventObserver);
    }

    public static void removeEventObserver(IEventObserver iEventObserver) {
        gAppLogInstance.removeEventObserver(iEventObserver);
    }

    /**
     * 为{@link AccountCacheHelper}设置新的Account，用于存储信息。 设置后，会将{@link
     * AccountCacheHelper}中原本存储在mCache的信息全部迁移到Account当中
     */
    public static void setAccount(final Account account) {
        gAppLogInstance.setAccount(account);
    }

    /**
     * 返回iid，如果没有，返回""
     */
    public static String getIid() {
        return gAppLogInstance.getIid();
    }

    /**
     * 返回ssid，如果没有，返回""
     */
    public static String getSsid() {
        return gAppLogInstance.getSsid();
    }

    /**
     * UserUniqueId(user_id)，如果没有，返回""
     */
    public static String getUserUniqueID() {
        return gAppLogInstance.getUserUniqueID();
    }

    /**
     * user_id，如果没有，返回""
     */
    public static String getUserID() {
        return gAppLogInstance.getUserID();
    }

    /**
     * clientUdid，如果没有，返回""
     */
    public static String getClientUdid() {
        return gAppLogInstance.getClientUdid();
    }

    /**
     * openUdid，如果没有，返回""
     */
    public static String getOpenUdid() {
        return gAppLogInstance.getOpenUdid();
    }

    /**
     * applog运行时修改Uri 该接口只做以下几件事： 1. 设置新的uri 2. 重新register 3. 重新activate
     */
    public static void setUriRuntime(UriConfig config) {
        gAppLogInstance.setUriRuntime(config);
    }

    /**
     * 请确保已经调用了AppLog.init，初始化前无数据返回
     */
    public static void getSsidGroup(Map<String, String> map) {
        gAppLogInstance.getSsidGroup(map);
    }

    /**
     * 设置register，config，abConfig相关数据本地加载和server加载的回调通知接口 注意：sdk内部存weak reference，不影响接口的回收
     *
     * @param listener 回调接口
     */
    public static void addDataObserver(IDataObserver listener) {
        gAppLogInstance.addDataObserver(listener);
    }

    public static void removeDataObserver(IDataObserver listener) {
        gAppLogInstance.removeDataObserver(listener);
    }

    public static void removeAllDataObserver() {
        gAppLogInstance.removeAllDataObserver();
    }

    public static INetworkClient getNetClient() {
        return gAppLogInstance.getNetClient();
    }

    /**
     * 获取header，header内容更新方式为引用替换，可以放心遍历获取字段 如需要修改header字段，请copy后再处理
     */
    public static @Nullable
    JSONObject getHeader() {
        return gAppLogInstance.getHeader();
    }

    public static void setAppTrack(JSONObject appTrackJson) {
        gAppLogInstance.setAppTrack(appTrackJson);
    }

    /**
     * 注册后返回的参数new_user,大于0为true,其余为false 不存sp,只存在内存中
     */
    public static boolean isNewUser() {
        return gAppLogInstance.isNewUser();
    }

    public static void onResume(Context context) {
        gAppLogInstance.onResume(context);
    }

    public static void onPause(Context context) {
        gAppLogInstance.onPause(context);
    }

    /**
     * must call in main thread from Activity.onResume().
     */
    public static void onActivityResumed(Activity activity, int hashCode) {
        gAppLogInstance.onActivityResumed(activity, hashCode);
    }

    public static void onActivityPause() {
        gAppLogInstance.onActivityPause();
    }

    public static void registerHeaderCustomCallback(
            IHeaderCustomTimelyCallback customTimelyCallback) {
        gAppLogInstance.registerHeaderCustomCallback(customTimelyCallback);
    }

    public static IHeaderCustomTimelyCallback getHeaderCustomCallback() {
        return gAppLogInstance.getHeaderCustomCallback();
    }

    /**
     * 设置profile信息，只能设置一次，后续设置无效，用于新用户
     *
     * @param jsonObject 用户profile信息
     */
    public static void userProfileSetOnce(JSONObject jsonObject, UserProfileCallback callback) {
        gAppLogInstance.userProfileSetOnce(jsonObject, callback);
    }

    /**
     * 同步profile信息，允许一次同步多个值
     *
     * @param jsonObject 用户profile信息
     */
    public static void userProfileSync(JSONObject jsonObject, UserProfileCallback callback) {
        gAppLogInstance.userProfileSync(jsonObject, callback);
    }

    public static void startSimulator(final String cookie) {
        gAppLogInstance.startSimulator(cookie);
    }

    public static void setRangersEventVerifyEnable(boolean enable, String cookie) {
        gAppLogInstance.setRangersEventVerifyEnable(enable, cookie);
    }

    public static void profileSet(JSONObject jsonObject) {
        gAppLogInstance.profileSet(jsonObject);
    }

    public static void profileSetOnce(JSONObject jsonObject) {
        gAppLogInstance.profileSetOnce(jsonObject);
    }

    public static void profileUnset(String key) {
        gAppLogInstance.profileUnset(key);
    }

    public static void profileIncrement(JSONObject jsonObject) {
        gAppLogInstance.profileIncrement(jsonObject);
    }

    public static void profileAppend(JSONObject jsonObject) {
        gAppLogInstance.profileAppend(jsonObject);
    }

    public static void setEventFilterByClient(List<String> eventList, boolean isBlock) {
        gAppLogInstance.setEventFilterByClient(eventList, isBlock);
    }

    /**
     * add device_token to header
     *
     * @return cookie map
     */
    public static Map<String, String> getRequestHeader() {
        return gAppLogInstance.getRequestHeader();
    }

    public static String getSessionId() {
        return gAppLogInstance.getSessionId();
    }

    /**
     * 设置ALink回调接口
     *
     * @param linkListener 接口
     */
    public static void setALinkListener(IALinkListener linkListener) {
        gAppLogInstance.setALinkListener(linkListener);
    }

    /**
     * 剪贴板的开关，默认关闭
     *
     * @param enabled 开关
     */
    public static void setClipboardEnabled(boolean enabled) {
        gAppLogInstance.setClipboardEnabled(enabled);
    }

    /**
     * 当深度链接激活时，用户主动调用
     *
     * @param uri 链接Uri
     */
    public static void activateALink(Uri uri) {
        gAppLogInstance.activateALink(uri);
    }

    /**
     * 获取sdk版本号
     */
    public static String getSdkVersion() {
        return gAppLogInstance.getSdkVersion();
    }

    /**
     * 获取所有ab配置
     */
    public static JSONObject getAllAbTestConfigs() {
        return gAppLogInstance.getAllAbTestConfigs();
    }

    /**
     * 隐私策略模式开关，设置后不存储上报事件
     */
    public static void setPrivacyMode(boolean privacyMode) {
        gAppLogInstance.setPrivacyMode(privacyMode);
    }

    public static boolean isPrivacyMode() {
        return gAppLogInstance.isPrivacyMode();
    }

    /**
     * 设置一个view的id
     *
     * @param view View
     * @param id   string
     * @usage: AppLog.setViewId(View view, String viewId)
     */
    public static void setViewId(View view, String id) {
        gAppLogInstance.setViewId(view, id);
    }

    /**
     * 设置一个dialog view的id
     *
     * @param dialog Dialog
     * @param id     string
     * @usage: AppLog.setViewId(Dialog dialog, String viewId)
     */
    public static void setViewId(Dialog dialog, String id) {
        gAppLogInstance.setViewId(dialog, id);
    }

    /**
     * 设置一个alert dialog view的id
     *
     * @param alertDialog Object
     * @param id          string
     * @usage: AppLog.setViewId(AlertDialog dialog, String viewId)
     */
    public static void setViewId(Object alertDialog, String id) {
        gAppLogInstance.setViewId(alertDialog, id);
    }

    /**
     * 设置view的属性
     *
     * @param view       View
     * @param properties JSON
     */
    public static void setViewProperties(View view, JSONObject properties) {
        gAppLogInstance.setViewProperties(view, properties);
    }

    /**
     * 查询view的属性
     *
     * @param view View
     * @return JSON
     */
    public static JSONObject getViewProperties(View view) {
        return gAppLogInstance.getViewProperties(view);
    }

    /**
     * 忽略页面事件
     *
     * @param pages 页面类型
     */
    public static void ignoreAutoTrackPage(Class<?>... pages) {
        gAppLogInstance.ignoreAutoTrackPage(pages);
    }

    /**
     * 是否忽略自动埋点页面事件
     *
     * @param page 页面Class
     * @return true:忽略
     */
    public static boolean isAutoTrackPageIgnored(Class<?> page) {
        return gAppLogInstance.isAutoTrackPageIgnored(page);
    }

    /**
     * 忽略View点击事件
     *
     * @param view View
     */
    public static void ignoreAutoTrackClick(View view) {
        gAppLogInstance.ignoreAutoTrackClick(view);
    }

    /**
     * 忽略指定类型的View的点击事件
     *
     * @param type 类型
     */
    public static void ignoreAutoTrackClickByViewType(Class<?>... type) {
        gAppLogInstance.ignoreAutoTrackClickByViewType(type);
    }

    /**
     * 检查点击事件是否被忽略
     *
     * @param view View
     * @return true: 忽略
     */
    public static boolean isAutoTrackClickIgnored(View view) {
        return gAppLogInstance.isAutoTrackClickIgnored(view);
    }

    /**
     * 手动埋点页面
     *
     * @param fragment android.app.Fragment
     */
    public static void trackPage(Object fragment) {
        gAppLogInstance.trackPage(fragment);
    }

    /**
     * 手动埋点页面
     *
     * @param fragment   android.app.Fragment
     * @param properties 自定义属性
     */
    public static void trackPage(Object fragment, JSONObject properties) {
        gAppLogInstance.trackPage(fragment, properties);
    }

    /**
     * 手动埋点页面
     *
     * @param activity Activity
     */
    public static void trackPage(Activity activity) {
        gAppLogInstance.trackPage(activity);
    }

    /**
     * 手动埋点页面
     *
     * @param activity   Activity
     * @param properties 自定义属性
     */
    public static void trackPage(Activity activity, JSONObject properties) {
        gAppLogInstance.trackPage(activity, properties);
    }

    /**
     * 手动埋点view点击事件
     *
     * @param view View
     */
    public static void trackClick(View view) {
        gAppLogInstance.trackClick(view);
    }

    /**
     * 手动埋点view点击事件
     *
     * @param view       View
     * @param properties JSON
     */
    public static void trackClick(View view, JSONObject properties) {
        gAppLogInstance.trackClick(view, properties);
    }

    /**
     * 设置事件的处理器
     *
     * @param handler IEventHandler
     */
    public static void setEventHandler(IEventHandler handler) {
        gAppLogInstance.setEventHandler(handler);
    }

    /**
     * 初始化H5的桥接器（当无法自动注入时可以手动桥接）
     *
     * @param webView WebView
     * @param url     访问的url
     */
    public static void initH5Bridge(View webView, String url) {
        gAppLogInstance.initH5Bridge(webView, url);
    }

    /**
     * 设置GPS坐标
     *
     * @param longitude           经度
     * @param latitude            纬度
     * @param geoCoordinateSystem 坐标系
     */
    public static void setGPSLocation(float longitude, float latitude, String geoCoordinateSystem) {
        gAppLogInstance.setGPSLocation(longitude, latitude, geoCoordinateSystem);
    }

    /**
     * 开始采集时长事件
     *
     * <p>多次调用 startTrackEvent("eventName") 时，事件 "eventName" 的开始时间以最后一次调用时为准
     *
     * @param eventName 事件名
     */
    public static void startDurationEvent(String eventName) {
        gAppLogInstance.startDurationEvent(eventName);
    }

    /**
     * 暂停时长事件
     *
     * @param eventName 事件名
     */
    public static void pauseDurationEvent(String eventName) {
        gAppLogInstance.pauseDurationEvent(eventName);
    }

    /**
     * 恢复时长事件
     *
     * @param eventName 事件名
     */
    public static void resumeDurationEvent(String eventName) {
        gAppLogInstance.resumeDurationEvent(eventName);
    }

    /**
     * 结束采集时长事件
     *
     * @param eventName  事件名
     * @param properties 属性
     */
    public static void stopDurationEvent(String eventName, JSONObject properties) {
        gAppLogInstance.stopDurationEvent(eventName, properties);
    }

    /**
     * 获取 View 曝光管理器
     *
     * @return {@link ViewExposureManager}
     */
    public static ViewExposureManager getViewExposureManager() {
        return gAppLogInstance.getViewExposureManager();
    }

    /**
     * 清除DB中的所有数据
     */
    public static void clearDb() {
        gAppLogInstance.clearDb();
    }

    /**
     * 强制打印DEBUG日志，请在init之后调用
     */
    public static void forcePrintDebugLog() {
        TLog.DEBUG = true;
    }
}
