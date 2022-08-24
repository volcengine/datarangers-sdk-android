// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

import com.bytedance.applog.IPageMeta;
import com.bytedance.applog.annotation.PageMeta;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 页面有关的工具类
 *
 * @author luodong.seu
 */
public class PageUtils {
    /** Activity页面类型名称集合 */
    private static final List<String> PAGE_ACTIVITY_CLASSES_NAMES =
            Collections.singletonList("android.app.Activity");
    /** Fragment页面类型名称集合 */
    private static final List<String> PAGE_FRAGMENT_CLASSES_NAMES =
            Arrays.asList(
                    "android.app.Fragment",
                    "androidx.fragment.app.Fragment",
                    "android.support.v4.app.Fragment");
    /** 支持的Activity页面类型 */
    private static final List<Class<?>> SUPPORT_ACTIVITY_PAGE_CLASSES = new ArrayList<>();
    /** 支持的Fragment页面类型 */
    private static final List<Class<?>> SUPPORT_FRAGMENT_PAGE_CLASSES = new ArrayList<>();

    /** 初始化页面类型Class */
    static {
        for (String clzName : PAGE_ACTIVITY_CLASSES_NAMES) {
            Class<?> clz = ReflectUtils.getClassByName(clzName);
            if (null != clz) {
                SUPPORT_ACTIVITY_PAGE_CLASSES.add(clz);
            }
        }
        for (String clzName : PAGE_FRAGMENT_CLASSES_NAMES) {
            Class<?> clz = ReflectUtils.getClassByName(clzName);
            if (null != clz) {
                SUPPORT_FRAGMENT_PAGE_CLASSES.add(clz);
            }
        }
    }

    /**
     * 获取Activity|Fragment的标题
     *
     * @param page Activity|Fragment
     * @return 标题 Annotation > getTitle > toolbar > label
     */
    public static String getTitle(Object page) {
        if (null == page) {
            return "";
        }
        if (page instanceof IPageMeta) {
            try {
                return ((IPageMeta) page).title();
            } catch (Throwable e) {
                TLog.e("Cannot get title from IPageMeta.", e);
            }
        }
        if (page.getClass().isAnnotationPresent(PageMeta.class)) {
            PageMeta meta = page.getClass().getAnnotation(PageMeta.class);
            if (null != meta && !TextUtils.isEmpty(meta.title())) {
                return meta.title();
            }
        }
        if (page instanceof Activity) {
            if (!TextUtils.isEmpty(((Activity) page).getTitle())) {
                return ((Activity) page).getTitle().toString();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                String toolbarTitle = getToolbarTitle(((Activity) page));
                if (!TextUtils.isEmpty(toolbarTitle)) {
                    return toolbarTitle;
                }
            }
            try {
                PackageManager packageManager = ((Activity) page).getPackageManager();
                if (null != packageManager) {
                    ActivityInfo activityInfo =
                            packageManager.getActivityInfo(((Activity) page).getComponentName(), 0);
                    CharSequence label = activityInfo.loadLabel(packageManager);
                    if (!TextUtils.isEmpty(label)) {
                        return label.toString();
                    }
                }
            } catch (Exception e) {
                TLog.e("Cannot get title from activity label.", e);
            }
        }
        return page.getClass().getName();
    }

    /**
     * 获取页面路径
     *
     * @param clz Object
     * @return 标题 Annotation > CanonicalName
     */
    public static String getPath(Object clz) {
        if (null == clz) {
            return "";
        }
        if (clz instanceof IPageMeta) {
            try {
                return ((IPageMeta) clz).path();
            } catch (Throwable e) {
                TLog.e("Cannot get path from IPageMeta.", e);
            }
        }
        if (clz.getClass().isAnnotationPresent(PageMeta.class)) {
            PageMeta meta = clz.getClass().getAnnotation(PageMeta.class);
            if (null != meta && !TextUtils.isEmpty(meta.path())) {
                return meta.path();
            }
        }
        return clz.getClass().getCanonicalName();
    }

    /**
     * 获取Toolbar的标题
     *
     * @param activity Activity
     * @return 标题
     */
    @TargetApi(11)
    public static String getToolbarTitle(Activity activity) {
        // 读取actionBar
        ActionBar actionBar = activity.getActionBar();
        if (null != actionBar) {
            if (!TextUtils.isEmpty(actionBar.getTitle())) {
                return actionBar.getTitle().toString();
            }
            return null;
        }

        // 读取getSupportActionBar -> getTitle
        try {
            Class<?> appCompatActivityClass =
                    ReflectUtils.getCurrentClass(
                            "android.support.v7.app.AppCompatActivity",
                            "androidx.appcompat.app.AppCompatActivity");
            if (null != appCompatActivityClass && appCompatActivityClass.isInstance(activity)) {
                Method method = activity.getClass().getMethod("getSupportActionBar");
                Object supportActionBar = method.invoke(activity);
                if (null != supportActionBar) {
                    method = supportActionBar.getClass().getMethod("getTitle");
                    CharSequence charSequence = (CharSequence) method.invoke(supportActionBar);
                    if (null != charSequence) {
                        return charSequence.toString();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 读取Activity|Fragment的自定义属性
     *
     * @param page Activity|Fragment
     * @return JSONObject
     */
    public static JSONObject getTrackProperties(Object page) {
        if (page instanceof IPageMeta) {
            try {
                return ((IPageMeta) page).pageProperties();
            } catch (Throwable e) {
                TLog.e("Cannot get track properties from activity.", e);
            }
        }
        return null;
    }

    /**
     * 是否为页面类型
     *
     * @param pageClass Class
     * @return true:是
     */
    public static boolean isPageClass(Class<?> pageClass) {
        for (Class<?> clz : SUPPORT_ACTIVITY_PAGE_CLASSES) {
            if (pageClass.isAssignableFrom(clz)) {
                return true;
            }
        }
        for (Class<?> clz : SUPPORT_FRAGMENT_PAGE_CLASSES) {
            if (pageClass.isAssignableFrom(clz)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为Activity对象
     *
     * @param obj Object
     * @return true:是
     */
    public static boolean isActivity(Object obj) {
        for (Class<?> clz : SUPPORT_ACTIVITY_PAGE_CLASSES) {
            if (clz.isInstance(obj)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为Fragment对象
     *
     * @param obj Object
     * @return true:是
     */
    public static boolean isFragment(Object obj) {
        for (Class<?> clz : SUPPORT_FRAGMENT_PAGE_CLASSES) {
            if (clz.isInstance(obj)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取Fragment的Activity对象
     *
     * @param fragment Object
     * @return Activity|null
     */
    public static Activity getFragmentActivity(Object fragment) {
        if (isFragment(fragment)) {
            // Fragment单独处理name，添加activity的title
            try {
                Method getActivityMethod = fragment.getClass().getMethod("getActivity");
                return (Activity) getActivityMethod.invoke(fragment);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    /**
     * 获取Fragment的Activity对象的名称
     *
     * @param fragment Object
     * @return String
     */
    public static String getFragmentActivityName(Object fragment) {
        Activity activity = getFragmentActivity(fragment);
        return null != activity ? activity.getClass().getName() : "";
    }
}
