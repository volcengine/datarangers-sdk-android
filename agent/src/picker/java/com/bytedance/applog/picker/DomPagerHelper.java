// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.picker;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Base64;
import android.view.View;

import com.bytedance.applog.log.LoggerImpl;
import com.bytedance.applog.util.ViewUtils;
import com.bytedance.applog.util.WindowHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 用于获取模拟器圈选时上传的dom及截图的base64
 *
 * @author wzj
 */
public class DomPagerHelper {
    private static final List<String> loggerTags = Collections.singletonList("DomPagerHelper");

    public static String getShotBase64(int displayId) {
        Bitmap bitmap = captureScreen(displayId);
        String bitmapBase64Str = bitmapToBase64(bitmap);
        if (bitmap != null) {
            bitmap.recycle();
        }
        return bitmapBase64Str;
    }

    public static JSONArray getDomPagerArray(
            Circle rootCircle, List<WebInfoModel> webInfoModelList) {
        try {
            JSONArray pageArray = new JSONArray();
            // 原生页面dom
            JSONObject nativePage = new JSONObject();
            nativePage.put("pageKey", rootCircle.page);
            nativePage.put("is_html", false);
            nativePage.put("frame", rootCircle.getFrameJson());
            JSONArray domArray = new JSONArray();
            domArray.put(getNativeDom(rootCircle));
            nativePage.put("dom", domArray);
            pageArray.put(nativePage);
            // webView的dom
            for (WebInfoModel webInfoModel : webInfoModelList) {
                JSONObject webViewPage = new JSONObject();
                webViewPage.put("pageKey", webInfoModel.getPage());
                webViewPage.put("is_html", true);
                webViewPage.put("frame", webInfoModel.getFrame().toJson());
                webViewPage.put("element_path", webInfoModel.getWebViewElementPath());
                List<WebInfoModel.InfoModel> infos = webInfoModel.getInfo();
                domArray = new JSONArray();
                for (WebInfoModel.InfoModel info : infos) {
                    domArray.put(getWebViewDom(info));
                }
                webViewPage.put("dom", domArray);
                pageArray.put(webViewPage);
            }
            return pageArray;
        } catch (Throwable e) {
            LoggerImpl.global().error(loggerTags, "getDomPagerArray failed", e);
        }
        return null;
    }

    private static Bitmap captureScreen(int displayId) {
        Bitmap screenShot = null;
        WindowHelper.init();
        View[] views = WindowHelper.getWindowViews();
        View decorView = null;

        for (View view : views) {
            if (WindowHelper.isDecorView(view) && ViewUtils.getDisplayId(view) == displayId) {
                decorView = view;
                break;
            }
        }

        if (decorView != null) {
            decorView.setDrawingCacheEnabled(true);
            try {
                Bitmap cacheBitmap = decorView.getDrawingCache();
                screenShot = cacheBitmap.copy(Bitmap.Config.RGB_565, true);
                decorView.setDrawingCacheEnabled(false);
            } catch (Throwable e) {
                LoggerImpl.global().error("Cannot get decor view screen shot", e);
                decorView.setDrawingCacheEnabled(false);
            } finally {
                decorView.destroyDrawingCache();
            }
        } else {
            LoggerImpl.global()
                    .warn(
                            "Cannot find decor view when captureScreen:{} in {} views",
                            displayId,
                            views.length);
        }
        if (screenShot == null) {
            LoggerImpl.global().warn("Cannot build decor view screenShot:{}", displayId);
            return null;
        }
        int[] decorViewLoc = new int[2];
        decorView.getLocationOnScreen(decorViewLoc);
        Canvas canvas = new Canvas(screenShot);

        for (View view : views) {
            if (view != null && ViewUtils.getDisplayId(view) == displayId) {
                int[] viewLoc = new int[2];
                view.getLocationOnScreen(viewLoc);
                view.setDrawingCacheEnabled(true);
                try {
                    Bitmap cacheBitmap = view.getDrawingCache();
                    Bitmap bitmap = cacheBitmap.copy(Bitmap.Config.RGB_565, true);
                    canvas.drawBitmap(
                            bitmap,
                            viewLoc[0] - decorViewLoc[0],
                            viewLoc[1] - decorViewLoc[1],
                            null);
                    view.setDrawingCacheEnabled(false);
                } catch (Throwable e) {
                    LoggerImpl.global().error("Cannot get view:{} screen shot", e, view.getId());
                    view.setDrawingCacheEnabled(false);
                } finally {
                    view.destroyDrawingCache();
                }
            }
        }
        return screenShot;
    }

    /**
     * bitmap转为base64
     *
     * @param bitmap 截图
     * @return base64字符串
     */
    static String bitmapToBase64(Bitmap bitmap) {
        String result = null;
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                // 新版本调整到 Webp 格式 并且压缩比调整 0%，数据比例能减少 50% 以上
                // 已验证过 chrome / safari
                bitmap.compress(Bitmap.CompressFormat.WEBP, 0, baos);

                baos.flush();
                baos.close();

                byte[] bitmapBytes = baos.toByteArray();
                result = Base64.encodeToString(bitmapBytes, Base64.NO_WRAP);
            } else {
                LoggerImpl.global().debug(loggerTags, "shot is null!");
            }
        } catch (IOException e) {
            LoggerImpl.global().error(loggerTags, "bitmapToBase64 failed", e);
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException ignored) {
            }
        }
        return result;
    }

    private static JSONObject getWebViewDom(WebInfoModel.InfoModel info) {
        try {
            JSONObject dom = new JSONObject();
            dom.put("frame", info.getFrameModel().toJson());
            dom.put("_element_path", info.getElementPath());
            dom.put("element_path", info.getElementPathV2());
            if (info.positions != null && info.positions.size() > 0) {
                dom.put("positions", new JSONArray(info.positions));
            }
            if (info.getTexts() != null && info.getTexts().size() > 0) {
                dom.put("texts", new JSONArray(info.getTexts()));
            }
            if (info.fuzzyPositions != null && info.fuzzyPositions.size() > 0) {
                dom.put("fuzzy_positions", new JSONArray(info.fuzzyPositions));
            }
            dom.put("zIndex", info.getzIndex());

            if (info.children != null && !info.children.isEmpty()) {
                JSONArray childrenArray = new JSONArray();
                for (WebInfoModel.InfoModel child : info.children) {
                    childrenArray.put(getWebViewDom(child));
                }
                dom.put("children", childrenArray);
            }
            return dom;
        } catch (Throwable e) {
            LoggerImpl.global().error(loggerTags, "getWebViewDom failed", e);
        }
        return null;
    }

    private static JSONObject getNativeDom(Circle circle) {
        try {
            JSONObject dom = new JSONObject();

            dom.put("frame", circle.getFrameJson());
            dom.put("element_path", circle.path);
            if (circle.positions != null && circle.positions.size() > 0) {
                circle.fuzzyPositions = new ArrayList<>();
                for (int i = 0; i < circle.positions.size(); i++) {
                    circle.fuzzyPositions.add("*");
                }
                dom.put("positions", new JSONArray(circle.positions));
                dom.put("fuzzy_positions", new JSONArray(circle.fuzzyPositions));
            }
            if (circle.contents != null && circle.contents.size() > 0) {
                dom.put("texts", new JSONArray(circle.contents));
            }
            dom.put("zIndex", circle.level);
            dom.put("ignore", circle.ignore);
            dom.put("is_html", circle.isHtml);

            JSONArray children = new JSONArray();
            for (Circle childCircle : circle.childrenCircle) {
                JSONObject childDom = getNativeDom(childCircle);
                children.put(childDom);
            }
            dom.put("children", children);
            return dom;
        } catch (Throwable e) {
            LoggerImpl.global().error(loggerTags, "getNativeDom failed", e);
        }
        return null;
    }
}
