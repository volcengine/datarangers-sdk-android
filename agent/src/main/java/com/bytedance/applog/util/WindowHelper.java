// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;

import com.bytedance.applog.collector.Navigator;
import com.bytedance.applog.log.LoggerImpl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * @author shiyanlong
 */
public class WindowHelper {
    private static final List<String> loggerTags = Collections.singletonList("WindowHelper");
    private static Object sWindowManger;

    private static Field viewsField;

    private static Class sPhoneWindowClazz;

    static Class sPopupWindowClazz;

    private static final String PREFIX_MAIN_WINDOW = "/MainWindow";

    private static final String PREFIX_DIALOG_WINDOW = "/DialogWindow";

    private static final String PREFIX_POPUP_WINDOW = "/PopupWindow";

    private static final String PREFIX_CUSTOM_WINDOW = "/CustomWindow";

    private static boolean sIsInitialized = false;

    private static boolean sArrayListWindowViews = false;

    private static boolean sViewArrayWindowViews = false;

    private WindowHelper() {}

    public static void init() {
        if (!sIsInitialized) {
            String windowManagerClassName;
            if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
                windowManagerClassName = "android.view.WindowManagerGlobal";
            } else {
                windowManagerClassName = "android.view.WindowManagerImpl";
            }

            Class windowManager;

            try {
                windowManager = Class.forName(windowManagerClassName);
                String windowManagerString;
                if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
                    windowManagerString = "sDefaultWindowManager";
                } else if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB_MR2) {
                    windowManagerString = "sWindowManager";
                } else {
                    windowManagerString = "mWindowManager";
                }

                viewsField = windowManager.getDeclaredField("mViews");
                Field instanceField = windowManager.getDeclaredField(windowManagerString);
                viewsField.setAccessible(true);
                if (viewsField.getType() == ArrayList.class) {
                    sArrayListWindowViews = true;
                } else if (viewsField.getType() == View[].class) {
                    sViewArrayWindowViews = true;
                }

                instanceField.setAccessible(true);
                sWindowManger = instanceField.get(null);
            } catch (Throwable e) {
                LoggerImpl.global().error(loggerTags, "Get window manager views failed", e);
            }

            try {
                if (VERSION.SDK_INT >= VERSION_CODES.M) {
                    try {
                        sPhoneWindowClazz =
                                Class.forName("com.android.internal.policy.PhoneWindow$DecorView");
                    } catch (ClassNotFoundException var5) {
                        sPhoneWindowClazz = Class.forName("com.android.internal.policy.DecorView");
                    }
                } else {
                    sPhoneWindowClazz =
                            Class.forName("com.android.internal.policy.impl.PhoneWindow$DecorView");
                }
            } catch (Throwable e) {
                LoggerImpl.global().error(loggerTags, "Get DecorView failed", e);
            }

            try {
                if (VERSION.SDK_INT >= VERSION_CODES.M) {
                    sPopupWindowClazz = Class.forName("android.widget.PopupWindow$PopupDecorView");
                } else {
                    sPopupWindowClazz =
                            Class.forName("android.widget.PopupWindow$PopupViewContainer");
                }
            } catch (Throwable e) {
                LoggerImpl.global().error(loggerTags, "Get popup view failed", e);
            }

            sIsInitialized = true;
        }
    }

    public static View[] getWindowViews() {
        View[] result = new View[0];
        if (sWindowManger == null) {
            Activity current = Navigator.getForegroundActivity();
            result = current != null ? new View[] {current.getWindow().getDecorView()} : result;
        } else {
            try {
                View[] views = null;
                if (sArrayListWindowViews) {
                    views = (View[]) ((ArrayList) viewsField.get(sWindowManger)).toArray(result);
                } else if (sViewArrayWindowViews) {
                    views = (View[]) viewsField.get(sWindowManger);
                }

                if (views != null) {
                    result = views;
                }
            } catch (Exception e) {
                LoggerImpl.global().error(loggerTags, "getWindowViews failed", e);
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

    static String getSubWindowPrefix(View root) {
        LayoutParams params = root.getLayoutParams();
        if (params instanceof WindowManager.LayoutParams) {
            WindowManager.LayoutParams windowParams =
                    (WindowManager.LayoutParams) params;
            int type = windowParams.type;
            if (type == WindowManager.LayoutParams.TYPE_BASE_APPLICATION) {
                return PREFIX_MAIN_WINDOW;
            } else if (type < WindowManager.LayoutParams.LAST_APPLICATION_WINDOW
                    && root.getClass() == sPhoneWindowClazz) {
                return PREFIX_DIALOG_WINDOW;
            } else if (type < WindowManager.LayoutParams.LAST_SUB_WINDOW
                    && root.getClass() == sPopupWindowClazz) {
                return PREFIX_POPUP_WINDOW;
            } else if (type < WindowManager.LayoutParams.LAST_SYSTEM_WINDOW) {
                return PREFIX_CUSTOM_WINDOW;
            }
        }

        // 被杀恢复时，view会恢复点击状态，此时root还没有添加到窗口上，默认是Activity吧。
        Class rootClazz = root.getClass();
        if (rootClazz == sPhoneWindowClazz) {
            return PREFIX_MAIN_WINDOW;
        } else {
            return rootClazz == sPopupWindowClazz ? PREFIX_POPUP_WINDOW : PREFIX_CUSTOM_WINDOW;
        }
    }

    public static boolean isDecorView(View rootView) {
        if (!sIsInitialized) {
            init();
        }

        Class rootClass = rootView.getClass();
        return rootClass == sPhoneWindowClazz || rootClass == sPopupWindowClazz;
    }

    @SuppressLint({"RestrictedApi"})
    static Object getMenuItemData(View view) {
        try {
            Method method = view.getClass().getMethod("getItemData");
            return method.invoke(view);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }
}
