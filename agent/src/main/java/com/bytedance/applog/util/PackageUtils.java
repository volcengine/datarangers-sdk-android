// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * pkg manager工具类，缓存
 *
 * @author luodong.seu@bytedance.com
 */
public class PackageUtils {

    private static final Map<String, Boolean> existsPackageMap = new ConcurrentHashMap<>();
    private static final Map<String, PackageInfo> packageInfoMap = new ConcurrentHashMap<>();

    /**
     * 判断是否存在某个package
     *
     * @param context     Context
     * @param packageName 包名
     * @return true 存在
     */
    public static boolean existsPackage(Context context, String packageName) {
        Context appContext = context.getApplicationContext();
        Context finalContext = null != appContext ? appContext : context;
        String key = finalContext.hashCode() + "@" + packageName;
        synchronized (existsPackageMap) {
            if (!existsPackageMap.containsKey(key)) {
                try {
                    PackageManager pm = finalContext.getPackageManager();
                    PackageInfo info =
                            pm.getPackageInfo(packageName, PackageManager.GET_CONFIGURATIONS);
                    existsPackageMap.put(key, info != null);
                } catch (Throwable ignore) {
                    existsPackageMap.put(key, false);
                }
            }
            return Boolean.TRUE.equals(existsPackageMap.get(key));
        }
    }

    /**
     * 获取应用的基本信息
     *
     * @param context     Context
     * @param packageName 包名
     * @return PackageInfo | null
     */
    public static @Nullable PackageInfo getPackageInfo(Context context, String packageName) {
        return getPackage(context, packageName, 0);
    }

    /**
     * 获取应用的签名信息
     *
     * @param context     Context
     * @param packageName 包名
     * @return PackageInfo | null
     */
    public static @Nullable PackageInfo getPackageSignature(Context context, String packageName) {
        return getPackage(context, packageName, PackageManager.GET_SIGNATURES);
    }

    /**
     * 获取package信息
     *
     * @param context     Context
     * @param packageName 包名
     * @param flag        flag值
     * @return PackageInfo | null
     */
    private static @Nullable PackageInfo getPackage(Context context, String packageName, int flag) {
        Context appContext = context.getApplicationContext();
        Context finalContext = null != appContext ? appContext : context;
        String key = flag + ":" + finalContext.hashCode() + "@" + packageName;
        synchronized (packageInfoMap) {
            if (!packageInfoMap.containsKey(key)) {

                try {
                    PackageManager pm = finalContext.getPackageManager();
                    PackageInfo info = pm.getPackageInfo(packageName, flag);
                    packageInfoMap.put(key, info);
                } catch (Throwable ignore) {
                }
            }
            return packageInfoMap.get(key);
        }
    }

    /**
     * 获取 pack 内的版本信息
     *
     * @param context     Context
     * @return String | null
     */
    public static @Nullable String getVersionName(Context context) {
        PackageInfo packageInfo = getPackageInfo(context, context.getPackageName());
        return null != packageInfo ? packageInfo.versionName : "";
    }

    /**
     * 获取 pack 内的版本信息
     *
     * @param context     Context
     * @return int | 0
     */
    public static int getVersionCode(Context context) {
        PackageInfo packageInfo = getPackageInfo(context, context.getPackageName());
        return null != packageInfo ? packageInfo.versionCode : 0;
    }
}
