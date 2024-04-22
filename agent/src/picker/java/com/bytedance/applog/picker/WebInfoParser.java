// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.picker;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import android.webkit.WebView;

import com.bytedance.applog.IAppLogInstance;
import com.bytedance.applog.log.LoggerImpl;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用于接收WebViewJsUtil.getWebInfo返回的WebView中view信息 并解析成WebInfoModel
 *
 * @author wuzhijun
 */
public class WebInfoParser {

    private double mScale;
    private WebView mWebView;
    private int[] mLocation = new int[2];
    private final boolean mWebMarqueeMode;
    private final IAppLogInstance appLogInstance;
    private static int sScreenWidth;
    private static Map<String, Double> sScaleMap = new HashMap<>();

    public WebInfoParser(
            IAppLogInstance appLogInstance, WebView webView, boolean fixWebViewLocation) {
        this.appLogInstance = appLogInstance;
        this.mWebView = webView;
        this.mWebMarqueeMode = fixWebViewLocation;
    }

    WebInfoModel parse(String webInfoString) {
        if (!TextUtils.isEmpty(webInfoString)) {
            try {
                if (sScreenWidth == 0) {
                    sScreenWidth = getScreenWidth();
                }
                mWebView.getLocationInWindow(mLocation);
                String substring = webInfoString.substring(1, webInfoString.length() - 1);
                String str = substring.replace("\\\"", "\"").replace("\\\\", "\\");
                JSONObject webInfo = new JSONObject(str);
                WebInfoModel webInfoModel = new WebInfoModel();
                String page = webInfo.optString("page");
                webInfoModel.setPage(page);
                JSONArray infos = webInfo.optJSONArray("info");
                List<WebInfoModel.InfoModel> infoList = new ArrayList<>();

                if (sScaleMap.get(page) != null) {
                    mScale = sScaleMap.get(page);
                } else {
                    for (int i = 0; i < infos.length(); i++) {
                        JSONObject info = infos.optJSONObject(i);
                        JSONObject frame = info.optJSONObject("frame");
                        double scale = getScale(frame);
                        if (mScale == 0) {
                            mScale = scale;
                        } else {
                            mScale = Math.min(scale, mScale);
                        }
                    }
                    sScaleMap.put(page, mScale);
                }
                for (int i = 0; i < infos.length(); i++) {
                    JSONObject info = infos.optJSONObject(i);
                    WebInfoModel.InfoModel infoModel = getInfoModel(info);
                    infoList.add(infoModel);
                }
                webInfoModel.setInfo(infoList);
                return webInfoModel;
            } catch (JSONException e) {
                LoggerImpl.global()
                        .error(
                                Collections.singletonList("WebInfoParser"),
                                "WebInfoModel parse failed",
                                e);
            }
        }
        return null;
    }

    private WebInfoModel.InfoModel getInfoModel(JSONObject info) {
        String nodeName = info.optString("nodeName");
        JSONObject frame = info.optJSONObject("frame");

        WebInfoModel.FrameModel frameModel =
                new WebInfoModel.FrameModel(
                        (int) (frame.optInt("x") * mScale + (mWebMarqueeMode ? 0 : mLocation[0])),
                        (int) (frame.optInt("y") * mScale + (mWebMarqueeMode ? 0 : mLocation[1])),
                        (int) (frame.optInt("width") * mScale),
                        (int) (frame.optInt("height") * mScale));

        String elementPath = info.optString("_element_path");
        String elementPathV2 = info.optString("element_path");
        JSONArray positions = info.optJSONArray("positions");
        List<String> positionList = new ArrayList<>();
        if (positions != null) {
            for (int i = 0; i < positions.length(); i++) {
                positionList.add(positions.optString(i));
            }
        }
        int zIndex = info.optInt("zIndex");
        JSONArray texts = info.optJSONArray("texts");
        List<String> textList = new ArrayList<>();
        if (texts != null) {
            for (int i = 0; i < texts.length(); i++) {
                textList.add(texts.optString(i));
            }
        }

        String href = info.optString("href");
        boolean checkList = info.optBoolean("_checkList");

        JSONArray fuzzyPositions = info.optJSONArray("fuzzy_positions");
        List<String> fzPositionsList = new ArrayList<>();
        if (fuzzyPositions != null) {
            for (int i = 0; i < fuzzyPositions.length(); i++) {
                fzPositionsList.add(fuzzyPositions.optString(i));
            }
        }

        JSONArray children = info.optJSONArray("children");
        List<WebInfoModel.InfoModel> childrenList = new ArrayList<>();
        if (children != null && children.length() > 0) {
            for (int i = 0; i < children.length(); i++) {
                childrenList.add(getInfoModel(children.optJSONObject(i)));
            }
        }

        return new WebInfoModel.InfoModel(
                nodeName,
                frameModel,
                elementPath,
                elementPathV2,
                positionList,
                zIndex,
                textList,
                childrenList,
                href,
                checkList,
                fzPositionsList);
    }

    private double getScale(JSONObject frame) {
        int width = frame.optInt("width");
        return ((double) sScreenWidth) / width;
    }

    private int getScreenWidth() {
        Context context = appLogInstance.getContext();
        if (context != null) {
            try {
                Display display =
                        ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
                                .getDefaultDisplay();
                if (null == display) {
                    return 0;
                }
                DisplayMetrics displayMetrics = new DisplayMetrics();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    display.getRealMetrics(displayMetrics);
                } else {
                    display.getMetrics(displayMetrics);
                }
                return displayMetrics.widthPixels;
            } catch (Throwable e) {
                LoggerImpl.global()
                        .error(
                                Collections.singletonList("WebInfoParser"),
                                "getScreenWidth failed",
                                e);
            }
        }
        return 0;
    }
}
