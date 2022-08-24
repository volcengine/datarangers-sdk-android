// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import android.accounts.Account;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bytedance.applog.alink.IALinkListener;
import com.bytedance.applog.event.IEventHandler;
import com.bytedance.applog.exposure.ViewExposureManager;
import com.bytedance.applog.filter.AbstractEventFilter;
import com.bytedance.applog.monitor.IMonitor;
import com.bytedance.applog.network.INetworkClient;
import com.bytedance.applog.profile.UserProfileCallback;
import com.bytedance.applog.store.AccountCacheHelper;
import com.bytedance.applog.store.BaseData;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AppLog实例的抽象接口
 *
 * @author luodong.seu
 */
public interface IAppLogInstance {

    /**
     * 获取初始化配置的appId
     *
     * @return appid
     */
    String getAppId();

    /**
     * 采集事件
     *
     * @param data BaseData
     */
    void receive(BaseData data);

    /**
     * 采集字符串数组事件
     *
     * @param data 字符串数组
     */
    void receive(String[] data);

    /**
     * 设置APP信息
     *
     * @param appContext IAppContext
     */
    void setAppContext(IAppContext appContext);

    /**
     * 获取IAppContext实例
     *
     * @return IAppContext
     */
    IAppContext getAppContext();

    /**
     * 获取应用的上下文对象
     *
     * @return Context
     */
    Context getContext();

    /**
     * 初始化。必须在第一个Activity启动之前调用。 调用init之后，即可调用{@link #onActivityResumed(Activity, int)} (String,
     * int)} {@link #onActivityPause()} {@link #onEventV3(String)}
     *
     * @param context context
     * @param config 参考{@link InitConfig}
     */
    void init(@NonNull final Context context, @NonNull final InitConfig config);

    /**
     * 初始化，在Activity启动后调用。 调用init之后，即可调用{@link #onActivityResumed(Activity, int)} {@link
     * #onActivityPause()} {@link #onEventV3(String)}
     *
     * @param context context
     * @param config 参考{@link InitConfig}
     * @param activity 启动的activity
     */
    void init(@NonNull final Context context, @NonNull final InitConfig config, Activity activity);

    /** 初始化安全包 */
    void initMetaSec(final Context context);

    /** 启动。请在获得权限之后再调用start */
    void start();

    /**
     * 是否已经启动
     *
     * @return true 是
     */
    boolean hasStarted();

    InitConfig getInitConfig();

    /**
     * 是否开启全埋点
     *
     * @return true 开启
     */
    boolean isBavEnabled();

    /**
     * 强制将内存的事件存储到DB中，同步执行 <br>
     * 当发生crash时，请调用此接口。
     */
    void flush();

    boolean isH5BridgeEnable();

    boolean isH5CollectEnable();

    /**
     * 设置当前用户的唯一id； <br>
     * 每个事件，都会以'user_id'为键带上这个字段
     *
     * @param id 当前用户的唯一id
     */
    void setUserID(final long id);

    void setAppLanguageAndRegion(String language, String region);

    void setGoogleAid(String gaid);

    String addNetCommonParams(Context context, String url, boolean isApi, Level level);

    void putCommonParams(Context context, Map<String, String> params, boolean isApi, Level level);

    /** 设置uuid，立即生效 */
    void setUserUniqueID(final String id);

    /** 设置uuid，type:uuid类型 */
    void setUserUniqueID(final String id, final String type);

    /** 设置用于添加自定义url params的接口，能够覆盖默认header值 */
    void setExtraParams(IExtraParams iExtraParams);

    /**
     * 设置激活的自定义参数回调
     *
     * @param callback IActiveCustomParamsCallback
     */
    void setActiveCustomParams(IActiveCustomParamsCallback callback);

    /**
     * 查询自定义的激活参数回调
     *
     * @return IActiveCustomParamsCallback
     */
    IActiveCustomParamsCallback getActiveCustomParams();

    void setTouchPoint(String touchPoint);

    /**
     * 添加自定义上报信息； <br>
     * 设置一次即可，内部会存储。 <br>
     * 最终会以json格式放到AppLog的custom字段中。
     *
     * @param custom 用户设置的上报信息，Object需是JSON兼容的数据类型
     */
    void setHeaderInfo(final HashMap<String, Object> custom);

    void setHeaderInfo(final String key, final Object value);

    void removeHeaderInfo(final String key);

    /** set the ab_sdk_version */
    void setExternalAbVersion(final String version);

    /** 获取ab_sdk_version */
    String getAbSdkVersion();

    /**
     * 获取AB测试的配置 返回配置的同时，会记录该key对应的vid到已曝光区域 简而言之，调用后，就表明该key对应的实验已曝光
     *
     * @param key 配置项的健
     * @param defaultValue 默认值
     * @param <T> 类型，需是JSON兼容的数据类型
     * @return 配置项的值
     */
    @Nullable
    <T> T getAbConfig(String key, T defaultValue);

    /** 拉取AB实验的配置 */
    void pullAbTestConfigs();

    /**
     * 设置拉取ab实验间隔毫秒数
     *
     * @param mills 毫秒
     */
    void setPullAbTestConfigsThrottleMills(Long mills);

    /**
     * Use {@link IAppLogInstance#getAppId()} instead
     *
     * @return 应用ID
     */
    @Deprecated
    String getAid();

    <T> T getHeaderValue(String key, T fallbackValue, Class<T> tClass);

    void setTracerData(JSONObject tracerData);

    /**
     * 设置UA <br>
     * 设置一次即可，内部会存储。
     *
     * @param ua ua
     */
    void setUserAgent(final String ua);

    /** 上报v3事件。 */
    void onEventV3(@NonNull final String event);

    void onEventV3(@NonNull final String event, @Nullable final JSONObject params);

    void onEventV3(@NonNull final String event, @Nullable final JSONObject params, int eventType);

    void onEventV3(@NonNull final String event, @Nullable final Bundle params, int eventType);

    void onEventV3(@NonNull final String event, @Nullable final Bundle params);

    /** misc event 点播SDK */
    void onMiscEvent(@NonNull String logType, @Nullable JSONObject obj);

    /** 是否加密压缩上报数据，只在debug版可用，release版本不可设置 */
    void setEncryptAndCompress(final boolean enable);

    boolean getEncryptAndCompress();

    /** 返回后台生成的did，如果没有，返回"" */
    String getDid();

    /** 返回客户端生成的udid，如果没有，返回"" global版本可能为空 */
    String getUdid();

    /** 添加session变化回调 sdk内部通过weak reference持有，不影响业务接口回收 */
    void addSessionHook(ISessionObserver hook);

    /** 移除session变化回调 */
    void removeSessionHook(ISessionObserver hook);

    /** 设置埋点事件回调 sdk内部通过weak reference持有，不影响业务接口回收 */
    void addEventObserver(IEventObserver iEventObserver);

    void removeEventObserver(IEventObserver iEventObserver);

    /**
     * 为{@link AccountCacheHelper}设置新的Account，用于存储信息。 设置后，会将{@link
     * AccountCacheHelper}中原本存储在mCache的信息全部迁移到Account当中
     */
    void setAccount(final Account account);

    /** 返回iid，如果没有，返回"" */
    String getIid();

    /** 返回ssid，如果没有，返回"" */
    String getSsid();

    /** UserUniqueId(user_id)，如果没有，返回"" */
    String getUserUniqueID();

    /** user_id，如果没有，返回"" */
    String getUserID();

    /** clientUdid，如果没有，返回"" */
    String getClientUdid();

    /** openUdid，如果没有，返回"" */
    String getOpenUdid();

    /** applog运行时修改Uri 该接口只做以下几件事： 1. 设置新的uri 2. 重新register 3. 重新activate */
    void setUriRuntime(UriConfig config);

    /** 请确保已经调用了AppLog.init，初始化前无数据返回 */
    void getSsidGroup(Map<String, String> map);

    /**
     * 设置register，config，abConfig相关数据本地加载和server加载的回调通知接口 注意：sdk内部存weak reference，不影响接口的回收
     *
     * @param listener 回调接口
     */
    void addDataObserver(IDataObserver listener);

    void removeDataObserver(IDataObserver listener);

    void removeAllDataObserver();

    //    int getSuccRate();

    INetworkClient getNetClient();

    /** 获取header，header内容更新方式为引用替换，可以放心遍历获取字段 如需要修改header字段，请copy后再处理 */
    @Nullable
    JSONObject getHeader();

    void setAppTrack(JSONObject appTrackJson);

    /** 注册后返回的参数new_user,大于0为true,其余为false 不存sp,只存在内存中 */
    boolean isNewUser();

    void onResume(Context context);

    void onPause(Context context);

    void onActivityResumed(Activity activity, int hashCode);

    void onActivityPause();

    void registerHeaderCustomCallback(IHeaderCustomTimelyCallback customTimelyCallback);

    IHeaderCustomTimelyCallback getHeaderCustomCallback();

    /**
     * 设置profile信息，只能设置一次，后续设置无效，用于新用户
     *
     * @param jsonObject 用户profile信息
     */
    void userProfileSetOnce(JSONObject jsonObject, UserProfileCallback callback);

    /**
     * 同步profile信息，允许一次同步多个值
     *
     * @param jsonObject 用户profile信息
     */
    void userProfileSync(JSONObject jsonObject, UserProfileCallback callback);

    void startSimulator(final String cookie);

    void setRangersEventVerifyEnable(boolean enable, String cookie);

    void profileSet(JSONObject jsonObject);

    void profileSetOnce(JSONObject jsonObject);

    void profileUnset(String key);

    void profileIncrement(JSONObject jsonObject);

    void profileAppend(JSONObject jsonObject);

    void setEventFilterByClient(List<String> eventList, boolean isBlock);

    AbstractEventFilter getEventFilterByClient();

    Map<String, String> getRequestHeader();

    String getSessionId();

    /**
     * 设置ALink回调接口
     *
     * @param linkListener 接口
     */
    void setALinkListener(IALinkListener linkListener);

    /** 获取ALink回调接口类 */
    IALinkListener getALinkListener();

    /**
     * 剪贴板的开关，默认关闭
     *
     * @param enabled 开关
     */
    void setClipboardEnabled(boolean enabled);

    /**
     * 当深度链接激活时，用户主动调用
     *
     * @param uri 链接Uri
     */
    void activateALink(Uri uri);

    /** 获取sdk版本号 */
    String getSdkVersion();

    /** 获取所有ab配置 */
    JSONObject getAllAbTestConfigs();

    /** 隐私策略模式开关，设置后不存储上报事件 */
    void setPrivacyMode(boolean privacyMode);

    boolean isPrivacyMode();

    /**
     * 设置一个view的id
     *
     * @param view View
     * @param id string
     * @usage: AppLog.setViewId(View view, String viewId)
     */
    void setViewId(View view, String id);

    /**
     * 设置一个dialog view的id
     *
     * @param dialog Dialog
     * @param id string
     * @usage: AppLog.setViewId(Dialog dialog, String viewId)
     */
    void setViewId(Dialog dialog, String id);

    /**
     * 设置一个alert dialog view的id
     *
     * @param alertDialog Object
     * @param id string
     * @usage: AppLog.setViewId(AlertDialog dialog, String viewId)
     */
    void setViewId(Object alertDialog, String id);

    /** 启动标志 */
    int getLaunchFrom();

    void setLaunchFrom(int sLaunchFrom);

    /**
     * 获取深度链接地址
     *
     * @return url
     */
    String getDeepLinkUrl();

    /**
     * 设置view的属性
     *
     * @param view View
     * @param properties JSON
     */
    void setViewProperties(View view, JSONObject properties);

    /**
     * 查询view的属性
     *
     * @param view View
     * @return JSON
     */
    JSONObject getViewProperties(View view);

    /**
     * 忽略页面事件
     *
     * @param pages 页面类型
     */
    void ignoreAutoTrackPage(Class<?>... pages);

    /**
     * 是否忽略自动埋点页面事件
     *
     * @param page 页面Class
     * @return true:忽略
     */
    boolean isAutoTrackPageIgnored(Class<?> page);

    /**
     * 忽略View点击事件
     *
     * @param view View
     */
    void ignoreAutoTrackClick(View view);

    /**
     * 忽略指定类型的View的点击事件
     *
     * @param type 类型
     */
    void ignoreAutoTrackClickByViewType(Class<?>... type);

    /**
     * 检查点击事件是否被忽略
     *
     * @param view View
     * @return true: 忽略
     */
    boolean isAutoTrackClickIgnored(View view);

    /**
     * 手动埋点页面
     *
     * @param fragment androidx.Fragment
     */
    void trackPage(Object fragment);

    /**
     * 手动埋点页面
     *
     * @param fragment androidx.Fragment
     * @param properties 自定义属性
     */
    void trackPage(Object fragment, JSONObject properties);

    /**
     * 手动埋点页面
     *
     * @param activity Activity
     */
    void trackPage(Activity activity);

    /**
     * 手动埋点页面
     *
     * @param activity Activity
     * @param properties 自定义属性
     */
    void trackPage(Activity activity, JSONObject properties);

    /**
     * 手动埋点view点击事件
     *
     * @param view View
     */
    void trackClick(View view);

    /**
     * 手动埋点view点击事件
     *
     * @param view View
     * @param properties JSON
     */
    void trackClick(View view, JSONObject properties);

    /**
     * 设置事件的处理器
     *
     * @param handler IEventHandler
     */
    void setEventHandler(IEventHandler handler);

    /**
     * 获取事件的处理器
     *
     * @return IEventHandler
     */
    IEventHandler getEventHandler();

    /**
     * 初始化H5桥接器（备用）
     *
     * @param view WebView
     * @param url URL参数
     */
    void initH5Bridge(View view, String url);

    /**
     * 获取 View 曝光管理器
     *
     * @return {@link ViewExposureManager}
     */
    ViewExposureManager getViewExposureManager();

    /**
     * 设置GPS位置
     *
     * @param longitude 经度
     * @param latitude 纬度
     * @param geoCoordinateSystem 坐标系
     */
    void setGPSLocation(float longitude, float latitude, String geoCoordinateSystem);

    /**
     * 开始采集时长事件
     *
     * <p>多次调用 startTrackEvent("eventName") 时，事件 "eventName" 的开始时间以最后一次调用时为准
     *
     * @param eventName 事件名
     */
    void startDurationEvent(String eventName);

    /**
     * 暂停时长事件
     *
     * @param eventName 事件名
     */
    void pauseDurationEvent(String eventName);

    /**
     * 恢复时长事件
     *
     * @param eventName 事件名
     */
    void resumeDurationEvent(String eventName);

    /**
     * 结束采集时长事件
     *
     * @param eventName 事件名
     * @param properties 属性
     */
    void stopDurationEvent(String eventName, JSONObject properties);

    /** 清除数据库中的数据 */
    void clearDb();

    /**
     * 获取监控对象
     *
     * @return IMonitor
     */
    IMonitor getMonitor();
}
