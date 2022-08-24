// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.content.res.Resources;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.bytedance.applog.R;

/**
 * 组件有关的工具类
 *
 * @author luodong.seu
 */
public class WidgetUtils {

    /**
     * 获取组件的唯一ID
     *
     * @param view View
     * @return TypeName$$hashcode
     */
    public static String getHashCode(View view) {
        if (null == view) {
            return null;
        }
        return getType(view) + "$$" + view.hashCode();
    }

    /**
     * 获取组件的id
     *
     * @param view View
     * @return [package]:id/[xml-id]
     */
    public static String getId(View view) {
        if (null == view) {
            return "";
        }

        // 读取设置的id
        Object viewId = view.getTag(R.id.applog_tag_view_id);
        if (null != viewId && !TextUtils.isEmpty((String) viewId)) {
            return (String) viewId;
        }

        int id = view.getId();
        if (id != View.NO_ID) {
            try {
                return view.getResources().getResourceEntryName(view.getId());
            } catch (Resources.NotFoundException ignored) {
            } catch (Throwable e) {
                TLog.e(e);
            }
        }
        return "";
    }

    /**
     * 获取组件类型
     *
     * @param view View
     * @return 类型
     */
    public static String getType(View view) {
        if (null == view) {
            return "";
        }
        if (view instanceof CheckBox) { // CheckBox
            return "CheckBox";
        } else if (view instanceof RadioButton) { // RadioButton
            return "RadioButton";
        } else if (view instanceof ToggleButton) { // ToggleButton
            return "ToggleButton";
        } else if (view instanceof CompoundButton) { // Switch
            return getCompoundButtonViewType(view);
        } else if (view instanceof Button) { // Button
            return "Button";
        } else if (view instanceof CheckedTextView) { // CheckedTextView
            return "CheckedTextView";
        } else if (view instanceof TextView) { // TextView
            return "TextView";
        } else if (view instanceof ImageView) { // ImageView
            return "ImageView";
        } else if (view instanceof RatingBar) { // RatingBar
            return "RatingBar";
        } else if (view instanceof SeekBar) { // SeekBar
            return "SeekBar";
        } else if (view instanceof Spinner) { // SeekBar
            return "Spinner";
        } else if (isTabView(view)) { // TabView
            return "TabLayout";
        } else if (ReflectUtils.isInstance(
                view,
                "android.support.design.widget.NavigationView",
                "com.google.android.material.navigation.NavigationView")) { // NavigationView
            return "NavigationView";
        } else if (view instanceof ViewGroup) { // ViewGroup
            if (ReflectUtils.isInstance(
                    view,
                    "android.support.v7.widget.CardView",
                    "androidx.cardview.widget.CardView")) {
                return "CardView";
            }
            if (ReflectUtils.isInstance(
                    view,
                    "android.support.design.widget.NavigationView",
                    "com.google.android.material.navigation.NavigationView")) {
                return "NavigationView";
            }
        }
        try {
            return view.getClass().getCanonicalName();
        } catch (Throwable e) {
            TLog.e(e);
        }
        return "";
    }

    /**
     * 获取组合按钮的类型
     *
     * @param view 判断类型的 view
     * @return viewType
     */
    public static String getCompoundButtonViewType(View view) {
        if (ReflectUtils.isInstance(view, "android.widget.Switch")) {
            return "Switch";
        }
        if (ReflectUtils.isInstance(
                view,
                "android.support.v7.widget.SwitchCompat",
                "androidx.appcompat.widget.SwitchCompat")) {
            return "SwitchCompat";
        }
        return "";
    }

    /**
     * 判断是否为tabview
     *
     * @param tabView View
     * @return true:是TabView
     */
    private static boolean isTabView(View tabView) {
        try {
            Class<?> currentTabViewClass =
                    ReflectUtils.getCurrentClass(
                            "android.support.design.widget.TabLayout$TabView",
                            "com.google.android.material.tabs.TabLayout$TabView");
            return currentTabViewClass != null
                    && currentTabViewClass.isAssignableFrom(tabView.getClass());
        } catch (Throwable e) {
            TLog.e(e);
        }
        return false;
    }
}
