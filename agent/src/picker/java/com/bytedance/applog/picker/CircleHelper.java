// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.picker;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.IPickerCallback;
import com.bytedance.applog.collector.Navigator;
import com.bytedance.applog.store.Click;
import com.bytedance.applog.util.ViewHelper;
import com.bytedance.applog.util.ViewUtils;
import com.bytedance.applog.util.WebViewJsUtil;
import com.bytedance.applog.util.WindowHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用于获取circle信息
 *
 * @author wuzhijun
 */
public class CircleHelper implements Handler.Callback {
    private final Rect mRect = new Rect();
    private final Map<Integer, CircleInfo> mCircleInfos = new HashMap<>();
    private final Handler mHandler;

    private boolean mNativeViewFinish = false;
    private int mWaitingWebView = 0;

    private final CircleInfoListener mListener;

    /** 是否是用于电脑圈选模式,用于电脑圈选的,会找出所有的circle，包括不可点击的,且WebView的circle的y不算上WebView的高度 */
    private boolean mWebMarqueeMode = true;

    private Class rangersAppLogPickerClass;
    private Class reactRootViewClass;

    private final AppLogInstance appLogInstance;

    static class CircleInfo {
        Circle rootCircle;
        List<WebInfoModel> webInfoList = new ArrayList<>(2);

        public void addWebInfo(WebInfoModel webInfoModel) {
            webInfoList.add(webInfoModel);
        }

        public List<WebInfoModel> getWebInfoList() {
            List<WebInfoModel> list = new ArrayList<>(webInfoList);
            webInfoList.clear();
            return list;
        }
    }

    public CircleHelper(
            AppLogInstance appLogInstance,
            CircleInfoListener circleInfoListener,
            Looper callbackThreadLooper) {
        this.appLogInstance = appLogInstance;
        mListener = circleInfoListener;
        mHandler = new Handler(callbackThreadLooper, this);
        loadClasses();
    }

    private void loadClasses() {
        try {
            if (rangersAppLogPickerClass == null) {
                rangersAppLogPickerClass = Class.forName("com.reactnativerangersapplogreactnativeplugin.RangersAppLogPicker");
            }
            if (reactRootViewClass == null) {
                reactRootViewClass = Class.forName("com.facebook.react.ReactRootView");
            }
        } catch (Exception e) {}

    }

    public void getCircleInfo() {
        View reactRootView = getReactRootView();

        /** 仅当当前页是 React Native 且引用了 rn 插件，才会走 rn 圈选 */
        if (reactRootView != null && rangersAppLogPickerClass != null) {
            getReactNativeInfo(reactRootView);
        } else {
            getNativeInfo();
        }
    }
    private View getReactRootView() {
        Activity current = Navigator.getForegroundActivity();
        if (current == null) {
            appLogInstance.getLogger()
                    .warn(Collections.singletonList("CircleHelper"), "could not get current activity in getForegroundActivity");
            return null;
        }
        View view = current.findViewById(android.R.id.content);
        if (reactRootViewClass == null) {
            return null;
        }

        if (reactRootViewClass.isInstance(view)) {
            return view;
        }

        if (view instanceof FrameLayout) {
            FrameLayout layout = (FrameLayout) view;
            View rootView = layout.getRootView();
            if (reactRootViewClass.isInstance(rootView)) {
                return rootView;
            }

            View child = layout.getChildAt(0);
            if (child != null && reactRootViewClass.isInstance(child)) {
                return child;
            }
        }
        return null;
    }
    private void getNativeInfo() {
        WindowHelper.init();
        // todo 是否直接取decorView就可以了
        View[] views = WindowHelper.getWindowViews();
        for (final View view : views) {
            CircleInfo circleInfo;
            int displayId = ViewUtils.getDisplayId(view);
            if (!mCircleInfos.containsKey(displayId)) {
                circleInfo = new CircleInfo();
                mCircleInfos.put(displayId, circleInfo);
            } else {
                circleInfo = mCircleInfos.get(displayId);
            }
            findCircle(view, null, circleInfo);
        }
        mNativeViewFinish = true;
        hasAllFinish();
    }
    private void getReactNativeInfo(View reactRootView) {
        try {
            Method instanceMethod = rangersAppLogPickerClass.getMethod("getInstance");
            Object instance = instanceMethod.invoke(null);

            final int displayId = ViewUtils.getDisplayId(reactRootView);
            Method loadInfoeMethod = rangersAppLogPickerClass.getMethod("loadPageInfoFromJS", reactRootViewClass, Object.class, Long.class);
            loadInfoeMethod.invoke(instance, reactRootView, new IPickerCallback() {
                @Override
                public void success(JSONObject pageInfo) {
                    JSONArray domPagerArray = new JSONArray();
                    domPagerArray.put(pageInfo);
                    hasAllFinish(displayId, domPagerArray);
                }

                @Override
                public void failed(String message) {
                    JSONArray domPagerArray = new JSONArray();
                    hasAllFinish(displayId, domPagerArray);
                    appLogInstance.getLogger()
                            .warn(Collections.singletonList("PickerApi"), "load reactNative pageInfo timeout");
                }
            }, 500L);
        } catch (Exception e) {
            appLogInstance.getLogger()
                    .error(Collections.singletonList("PickerApi"), "load reactNative pageInfo failed: " + e.getMessage(), e);
        }
    }

    /** 判断原生view和WebView的信息是否都获取完毕 都获取完毕则回调并将mWebViewFinish和mNativeViewFinish恢复初始化 */
    private void hasAllFinish() {
        if (mNativeViewFinish && mWaitingWebView == 0) {
            if (mListener != null) {
                mListener.onGetCircleInfoFinish(mCircleInfos);
            }
            mNativeViewFinish = false;
        }
    }
    /** 判断 React Native 页面信息是否获取完毕 都获取完毕则回调 */
    private void hasAllFinish(int displayId, JSONArray domPagerArray) {
        if (mListener != null) {
            mListener.onGetCircleInfoFinish(displayId, domPagerArray);
        }
    }

    private void findCircle(final View view, Circle parentCircle, CircleInfo circleInfo) {
        if (ViewHelper.isViewSelfVisible(view)) {
            Circle curCircle = null;
            if (mWebMarqueeMode || ViewUtils.isViewClickable(view)) {
                Click info = ViewHelper.getClickViewInfo(view, !mWebMarqueeMode);
                if (info != null) {
                    Circle ci = new Circle(info);
                    ci.level = isViewCovered(view) ? 0 : Integer.MAX_VALUE;
                    if (!ViewUtils.isViewClickable(view)) {
                        ci.ignore = true;
                        ci.level = 0;
                    }
                    ci.location = new int[2];
                    view.getLocationOnScreen(ci.location);
                    ci.width = view.getWidth();
                    ci.height = view.getHeight();
                    ci.isHtml = false;

                    curCircle = ci;
                    if (parentCircle == null) {
                        circleInfo.rootCircle = ci;
                    }
                    if (parentCircle != null) {
                        parentCircle.childrenCircle.add(ci);
                    }
                    if (view instanceof WebView) {
                        mWaitingWebView++;
                        getWebCircleInfos((WebView) view);
                        ci.ignore = false;
                        ci.isHtml = true;
                    }
                }
            }
            if (view instanceof ViewParent) {
                ViewGroup group = (ViewGroup) view;
                final int count = group.getChildCount();
                for (int i = 0; i < count; ++i) {
                    View child = group.getChildAt(i);
                    if (null == curCircle && !isValidSubView(child)) {
                        // 如果是decorView的子View，则需要校验是否有效，防止出现其他View替换了主View
                        continue;
                    }
                    findCircle(child, curCircle, circleInfo);
                }
            }
        }
    }

    /**
     * 是否为合法的子View：检测DecorView子View是否有效
     *
     * @param view View
     */
    private boolean isValidSubView(View view) {
        if (null == view) {
            return false;
        }
        if (view instanceof ViewParent) {
            return true;
        }
        if (!ViewUtils.isViewClickable(view)) {
            return false;
        }
        return ViewHelper.isViewSelfVisible(view);
    }

    private void getWebCircleInfos(final WebView webView) {
        webView.post(
                new Runnable() {
                    @Override
                    public void run() {
                        WebViewJsUtil.getWebInfo(webView, mHandler);
                    }
                });
    }

    @Override
    public boolean handleMessage(Message msg) {
        mWaitingWebView--;
        WebView webView = (WebView) msg.obj;
        Bundle data = msg.getData();
        String webInfoStr = data.getString(WebViewJsUtil.BUNDLE_WEB_INFO);
        WebInfoModel webInfoModel =
                new WebInfoParser(appLogInstance, webView, mWebMarqueeMode).parse(webInfoStr);
        if (webInfoModel != null) {
            int[] webViewLocation = new int[2];
            webView.getLocationInWindow(webViewLocation);
            WebInfoModel.FrameModel frame =
                    new WebInfoModel.FrameModel(
                            webViewLocation[0],
                            webViewLocation[1],
                            webView.getWidth(),
                            webView.getHeight());
            webInfoModel.setFrame(frame);

            // pc端圈选需要在带上WebView的element_path
            Click clickViewInfo = ViewHelper.getClickViewInfo(webView, !mWebMarqueeMode);
            webInfoModel.setWebViewElementPath(clickViewInfo != null ? clickViewInfo.path : "");

            CircleInfo circleInfo = mCircleInfos.get(ViewUtils.getDisplayId(webView));
            if (circleInfo != null) {
                circleInfo.addWebInfo(webInfoModel);
            }
        }
        hasAllFinish();
        return true;
    }

    public interface CircleInfoListener {
        /**
         * 获取到circle的回调 web圈选忽略第三个参数,移动端圈选请忽略前两个参数
         *
         * @param circleInfos 屏幕的displayId以及对应原生页面view的dom树的根节点信息
         */
        void onGetCircleInfoFinish(Map<Integer, CircleInfo> circleInfos);

        /**
         * React Native 页面走该回调
         *
         * @param domPagerArray React Native 的页面结构信息，可直接用于上传
         */
        void onGetCircleInfoFinish(int displayId, JSONArray domPagerArray);
    }

    /**
     * 判断view是否被遮挡
     *
     * @param view 需要判断的view
     * @return 被遮挡了返回true，否则false
     */
    private boolean isViewCovered(final View view) {
        View currentView = view;

        boolean partVisible = currentView.getGlobalVisibleRect(mRect);
        boolean totalHeightVisible = (mRect.bottom - mRect.top) >= view.getMeasuredHeight();
        boolean totalWidthVisible = (mRect.right - mRect.left) >= view.getMeasuredWidth();
        boolean totalViewVisible = partVisible && totalHeightVisible && totalWidthVisible;
        if (!totalViewVisible) {
            return true;
        }
        while (currentView.getParent() instanceof ViewGroup) {
            ViewGroup currentParent = (ViewGroup) currentView.getParent();
            if (currentParent.getVisibility() != View.VISIBLE) {
                return true;
            }

            int start = indexOfViewInParent(currentView, currentParent);
            for (int i = start + 1; i < currentParent.getChildCount(); i++) {
                Rect viewRect = new Rect();
                view.getGlobalVisibleRect(viewRect);
                View otherView = currentParent.getChildAt(i);
                Rect otherViewRect = new Rect();
                otherView.getGlobalVisibleRect(otherViewRect);
                if (Rect.intersects(viewRect, otherViewRect)) {
                    return true;
                }
            }
            currentView = currentParent;
        }
        return false;
    }

    private int indexOfViewInParent(View view, ViewGroup parent) {
        int index;
        for (index = 0; index < parent.getChildCount(); index++) {
            if (parent.getChildAt(index) == view) {
                break;
            }
        }
        return index;
    }
}
