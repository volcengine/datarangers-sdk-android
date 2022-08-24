// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import android.text.TextUtils;

import com.bytedance.applog.store.BaseData;

import java.util.ArrayList;
import java.util.List;

/**
 * AppLog帮助类
 *
 * @author luodong.seu
 */
public final class AppLogHelper {

    /** 是否开启了全埋点的匹配器 */
    public static AppLogInstanceMatcher isBavEnabledMatcher =
            new AppLogInstanceMatcher() {
                @Override
                public boolean match(AppLogInstance instance) {
                    return instance.isBavEnabled();
                }
            };

    /** 是否开启了H5桥接的匹配器 */
    public static AppLogInstanceMatcher isH5BridgeEnabledMatcher =
            new AppLogInstanceMatcher() {
                @Override
                public boolean match(AppLogInstance instance) {
                    return instance.isH5BridgeEnable();
                }
            };

    /** 是否开启了H5全埋点的匹配器 */
    public static AppLogInstanceMatcher isH5CollectEnabledMatcher =
            new AppLogInstanceMatcher() {
                @Override
                public boolean match(AppLogInstance instance) {
                    return instance.isH5CollectEnable();
                }
            };

    /** 是否开启了声明周期埋点的匹配器 */
    public static AppLogInstanceMatcher isHandleLifecycleMatcher =
            new AppLogInstanceMatcher() {
                @Override
                public boolean match(AppLogInstance instance) {
                    return null != instance.getInitConfig()
                            && instance.getInitConfig().isHandleLifeCycle();
                }
            };

    /**
     * 判断是否存在AppId实例
     *
     * @param appId appId
     * @return true 存在
     */
    public static boolean hasInstanceByAppId(final String appId) {
        return !TextUtils.isEmpty(appId)
                && matchInstance(
                        new AppLogInstanceMatcher() {
                            @Override
                            public boolean match(AppLogInstance instance) {
                                return appId.equals(instance.getAppId());
                            }
                        });
    }

    /**
     * 获取指定appId对应的实例
     *
     * @param appId appId
     * @return AppLogInstance
     */
    public static AppLogInstance getInstanceByAppId(final String appId) {
        if (TextUtils.isEmpty(appId)) {
            return null;
        }
        for (AppLogInstance instance : AppLogInstance.getAllInstances()) {
            if (appId.equals(instance.getAppId())) {
                return instance;
            }
        }
        return null;
    }

    /**
     * 获取指定appId对应的实例，如果不存在则返回全局的实例
     *
     * @param appId appId
     * @return IAppLogInstance
     */
    public static IAppLogInstance getInstanceByAppIdOrGlobalDefault(final String appId) {
        IAppLogInstance instance = getInstanceByAppId(appId);
        return null != instance ? instance : AppLog.getInstance();
    }

    /**
     * 采集基础事件
     *
     * @param baseData BaseData
     */
    public static void receive(BaseData baseData) {
        for (AppLogInstance instance : AppLogInstance.getAllInstances()) {
            instance.receive(baseData.clone());
        }
    }

    /**
     * 采集序列化后的事件数据
     *
     * @param data String[]
     */
    public static void receive(String[] data) {
        for (AppLogInstance instance : AppLogInstance.getAllInstances()) {
            instance.receive(data.clone());
        }
    }

    /**
     * 仅在指定match下采集数据
     *
     * @param baseData BaseData
     * @param matcher AppLogInstanceMatcher 过滤规则
     */
    public static void receiveIf(BaseData baseData, AppLogInstanceMatcher matcher) {
        for (AppLogInstance instance : AppLogInstance.getAllInstances()) {
            if (matcher.match(instance)) {
                instance.receive(baseData.clone());
            }
        }
    }

    /**
     * 仅在指定条件下采集数据
     *
     * @param baseDataLoader BaseLoader回调
     * @param matcher 过滤规则
     */
    public static void receiveIf(BaseDataLoader baseDataLoader, AppLogInstanceMatcher matcher) {
        BaseData baseData = null;
        for (AppLogInstance instance : AppLogInstance.getAllInstances()) {
            if (matcher.match(instance)) {
                if (null == baseData) {
                    baseData = baseDataLoader.load();
                }
                instance.receive(baseData.clone());
            }
        }
    }

    /**
     * 获取开启全埋点的实例列表
     *
     * @return List<AppLogInstance>
     */
    public static List<AppLogInstance> filterInstances(AppLogInstanceMatcher filter) {
        List<AppLogInstance> instances = new ArrayList<>();
        for (AppLogInstance instance : AppLogInstance.getAllInstances()) {
            if (filter.match(instance)) {
                instances.add(instance);
            }
        }
        return instances;
    }

    /**
     * 匹配实例
     *
     * @return true: 存在匹配成功的实例
     */
    public static boolean matchInstance(AppLogInstanceMatcher filter) {
        for (AppLogInstance instance : AppLogInstance.getAllInstances()) {
            if (filter.match(instance)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 遍历所有实例执行操作
     *
     * @param handler AppLogInstanceHandler
     */
    public static void handleAll(AppLogInstanceHandler handler) {
        for (AppLogInstance instance : AppLogInstance.getAllInstances()) {
            handler.handle(instance);
        }
    }

    /**
     * 是否为全局的实例
     *
     * @param instance IAppLogInstance
     * @return true 是
     */
    public static boolean isGlobalInstance(IAppLogInstance instance) {
        return AppLog.getInstance() == instance;
    }

    /**
     * 全局实例是否初始化了
     *
     * @return true 是
     */
    public static boolean isGlobalInstanceInitialized() {
        return !TextUtils.isEmpty(AppLog.getInstance().getAppId());
    }

    /**
     * 获取实例的SP文件名称
     *
     * @param instance 实例
     * @param defaultSpName 默认的SP文件名
     * @return 新的文件名
     */
    public static String getInstanceSpName(IAppLogInstance instance, String defaultSpName) {
        return isGlobalInstance(instance)
                ? defaultSpName
                : defaultSpName + "_" + instance.getAppId();
    }

    public interface AppLogInstanceMatcher {
        boolean match(AppLogInstance instance);
    }

    public interface AppLogInstanceHandler {
        void handle(AppLogInstance instance);
    }

    public interface BaseDataLoader {
        BaseData load();
    }
}
