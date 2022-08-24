// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.v7.view.menu.ListMenuItemView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;

import com.bytedance.applog.collector.Navigator;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * @author shiyanlong
 */
public class WindowUtils {

    private static final String PREFIX_MAIN_WINDOW = "/MainWindow";

    private static final String PREFIX_DIALOG_WINDOW = "/DialogWindow";

    private static final String PREFIX_POPUP_WINDOW = "/PopupWindow";

    private static final String PREFIX_CUSTOM_WINDOW = "/CustomWindow";

    private static Object sWindowManger;

    private static Field viewsField;

    private static Class<?> sPhoneWindowClass;

    static Class<?> sPopupWindowClass;

    private static Class<?> sListMenuItemViewClass;

    private static Method sItemViewGetDataMethod;

    private static boolean sWindowViewsIsList = false;

    private static boolean sWindowViewsIsArray = false;

    private static boolean sInitialized = false;

    private WindowUtils() {
    }

    public static void initialize() {
        if (!sInitialized) {
            String windowManagerClassName;
            if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
                windowManagerClassName = "android.view.WindowManagerGlobal";
            } else {
                windowManagerClassName = "android.view.WindowManagerImpl";
            }

            try {
                Class<?> windowManagerClass = Class.forName(windowManagerClassName);
                String windowManagerFieldName;
                if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
                    windowManagerFieldName = "sDefaultWindowManager";
                } else if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB_MR2) {
                    windowManagerFieldName = "sWindowManager";
                } else {
                    windowManagerFieldName = "mWindowManager";
                }
                viewsField = windowManagerClass.getDeclaredField("mViews");
                Field windowManagerField = windowManagerClass.getDeclaredField(windowManagerFieldName);
                viewsField.setAccessible(true);
                if (viewsField.getType() == ArrayList.class) {
                    sWindowViewsIsList = true;
                } else if (viewsField.getType() == View[].class) {
                    sWindowViewsIsArray = true;
                }
                windowManagerField.setAccessible(true);
                sWindowManger = windowManagerField.get(null);
            } catch (Throwable e) {
                TLog.ysnp(e);
            }

            try {
                if (VERSION.SDK_INT >= VERSION_CODES.M) {
                    try {
                        sPhoneWindowClass =
                                Class.forName("com.android.internal.policy.PhoneWindow$DecorView");
                    } catch (ClassNotFoundException e) {
                        sPhoneWindowClass = Class.forName("com.android.internal.policy.DecorView");
                    }
                } else {
                    sPhoneWindowClass =
                            Class.forName("com.android.internal.policy.impl.PhoneWindow$DecorView");
                }
            } catch (Throwable e) {
                TLog.ysnp(e);
            }

            try {
                sListMenuItemViewClass =
                        Class.forName("com.android.internal.view.menu.ListMenuItemView");
                sItemViewGetDataMethod = Class.forName("com.android.internal.view.menu.MenuView$ItemView").getDeclaredMethod("getItemData");
            } catch (Throwable e) {
                TLog.ysnp(e);
            }

            try {
                if (VERSION.SDK_INT >= VERSION_CODES.M) {
                    sPopupWindowClass = Class.forName("android.widget.PopupWindow$PopupDecorView");
                } else {
                    sPopupWindowClass =
                            Class.forName("android.widget.PopupWindow$PopupViewContainer");
                }
            } catch (Throwable e) {
                TLog.ysnp(e);
            }

            sInitialized = true;
        }
    }

    public static View[] getWindowViews() {
        View[] result = new View[0];
        if (sWindowManger == null) {
            Activity current = Navigator.getForegroundActivity();
            result = current != null ? new View[]{current.getWindow().getDecorView()} : result;
        } else {
            try {
                View[] views = null;
                if (sWindowViewsIsList) {
                    views = (View[]) ((ArrayList) viewsField.get(sWindowManger)).toArray(result);
                } else if (sWindowViewsIsArray) {
                    views = (View[]) viewsField.get(sWindowManger);
                }
                if (views != null) {
                    result = views;
                }
            } catch (Throwable e) {
                TLog.ysnp(e);
            }
            result = filterTouchableView(result);
        }
        return result;
    }

    private static View[] filterTouchableView(View[] array) {
        List<View> list = new ArrayList<>(array.length);
        HashSet<Integer> displayIds = new HashSet<>();
        View[] result = array;
        final int length = array.length;

        for (int i = 0; i < length; ++i) {
            View view = result[length - 1 - i];
            if (view != null) {
                if (view.getWindowVisibility() != View.VISIBLE
                        || view.getVisibility() != View.VISIBLE
                        || view.getWidth() == 0
                        || view.getHeight() == 0
                        || view instanceof FloatView) {
                    continue;
                }
                int displayId = ViewUtils.getDisplayId(view);
                if (!displayIds.contains(displayId)) {
                    list.add(0, view);
                    if (isDecorView(view)) {
                        displayIds.add(displayId);
                    }
                }
            }
        }

        result = new View[list.size()];
        list.toArray(result);
        return result;
    }

    public static boolean isDecorView(View view) {
        if (!sInitialized) {
            initialize();
        }

        Class<?> viewClass = view.getClass();
        return viewClass == sPhoneWindowClass || viewClass == sPopupWindowClass;
    }

    static String getWindowPrefix(View view) {
        LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams instanceof WindowManager.LayoutParams) {
            WindowManager.LayoutParams windowLayoutParams =
                    (WindowManager.LayoutParams) layoutParams;
            int windowType = windowLayoutParams.type;
            if (windowType == WindowManager.LayoutParams.TYPE_BASE_APPLICATION) {
                return PREFIX_MAIN_WINDOW;
            } else if (view.getClass() == sPopupWindowClass) {
                if (windowType < WindowManager.LayoutParams.LAST_APPLICATION_WINDOW) {
                    return PREFIX_DIALOG_WINDOW;
                } else if (windowType < WindowManager.LayoutParams.LAST_SUB_WINDOW) {
                    return PREFIX_POPUP_WINDOW;
                }
            } else if (windowType < WindowManager.LayoutParams.LAST_SYSTEM_WINDOW) {
                return PREFIX_CUSTOM_WINDOW;
            }
        }

        // 被杀恢复时，view会恢复点击状态，此时root还没有添加到窗口上，默认是Activity吧。
        Class<?> viewClass = view.getClass();
        if (viewClass == sPhoneWindowClass) {
            return PREFIX_MAIN_WINDOW;
        }
        return viewClass == sPopupWindowClass ? PREFIX_POPUP_WINDOW : PREFIX_CUSTOM_WINDOW;
    }

    @SuppressLint("RestrictedApi")
    static Object getMenuItemData(View view)
            throws InvocationTargetException, IllegalAccessException {
        if (view.getClass() == sListMenuItemViewClass) {
            return sItemViewGetDataMethod.invoke(view);
        } else if (ClassHelper.isSupportListMenuItemView(view)) {
            return ((android.support.v7.view.menu.ListMenuItemView) view).getItemData();
        } else if (ClassHelper.isAndroidXListMenuItemView(view)) {
            return ((ListMenuItemView) view).getItemData();
        }
        return null;
    }
}
