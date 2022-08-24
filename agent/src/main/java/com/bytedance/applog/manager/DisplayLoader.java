// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.manager;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.bytedance.applog.server.Api;
import com.bytedance.applog.util.TLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;

/**
 * @author shiyanlong
 * @date 2019/2/2
 */
class DisplayLoader extends BaseLoader {

    private final Context mApp;

    DisplayLoader(Context ctx) {
        super(true, false);
        mApp = ctx;
    }

    @Override
    protected boolean doLoad(final JSONObject info) throws JSONException {
        DisplayMetrics dm = mApp.getResources().getDisplayMetrics();
        int density = dm.densityDpi;
        String dpi;
        switch (density) {
            case DisplayMetrics.DENSITY_LOW:
                dpi = "ldpi";
                break;
            case DisplayMetrics.DENSITY_HIGH:
                dpi = "hdpi";
                break;
            case DisplayMetrics.DENSITY_260:
            case DisplayMetrics.DENSITY_280:
            case DisplayMetrics.DENSITY_300:
            case DisplayMetrics.DENSITY_XHIGH:
                dpi = "xhdpi";
                break;
            case DisplayMetrics.DENSITY_340:
            case DisplayMetrics.DENSITY_360:
            case DisplayMetrics.DENSITY_400:
            case DisplayMetrics.DENSITY_420:
            case DisplayMetrics.DENSITY_440:
            case DisplayMetrics.DENSITY_XXHIGH:
                dpi = "xxhdpi";
                break;
            case DisplayMetrics.DENSITY_560:
            case DisplayMetrics.DENSITY_XXXHIGH:
                dpi = "xxxhdpi";
                break;
            default:
                dpi = "mdpi";
                break;
        }
        info.put(Api.KEY_DENSITY_DPI, density);
        info.put(Api.KEY_DISPLAY_DENSITY, dpi);
        int[] size = getScreenPixels();
        info.put(Api.KEY_RESOLUTION, size[1] + "x" + size[0]);
        return true;
    }

    public int[] getScreenPixels() {
        int width = 0, height = 0;
        WindowManager windowManager = (WindowManager) mApp.getSystemService(Context.WINDOW_SERVICE);
        final DisplayMetrics metrics = new DisplayMetrics();
        Display display = windowManager.getDefaultDisplay();
        try {
            // For JellyBean 4.2 (API 17) and onward
            if (null != display
                    && android.os.Build.VERSION.SDK_INT
                            >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                display.getRealMetrics(metrics);
                width = metrics.widthPixels;
                height = metrics.heightPixels;
            } else {
                Method mGetRawH = Display.class.getMethod("getRawHeight");
                Method mGetRawW = Display.class.getMethod("getRawWidth");
                if (null != mGetRawW) {
                    width = (Integer) mGetRawW.invoke(display);
                }
                if (null != mGetRawH) {
                    height = (Integer) mGetRawH.invoke(display);
                }
            }
        } catch (Throwable e) {
            TLog.e(e);
        }
        return new int[] {width, height};
    }
}
