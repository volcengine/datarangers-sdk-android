// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import com.bytedance.applog.AppLogHelper;
import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.log.LoggerImpl;
import com.bytedance.applog.store.BaseData;
import com.bytedance.applog.store.EventV3;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author linguoqing auto版 注意：auto版和picker版处理不同 auto版本是给到外部用户用，会生成web点击信息回调native做上报
 * picker版本是给到开发/产品用，可以呈现web页外部用户点击数据
 */
public class WebViewJsUtil {
    private static final List<String> loggerTags = Collections.singletonList("WebViewJsUtil");
    public static final String BUNDLE_WEB_INFO = "web_info";
    public static final String EMPTY_PAGE = "about:blank";
    private static final String JS_URL_PREFIX = "javascript:";

    /**
     * js回调入口，拿到js采集的点击事件，直接上报
     */
    private static class TeaNativeReport {

        @JavascriptInterface
        public void postMessage(String paramFromJS) {
            LoggerImpl.global()
                    .debug(loggerTags, "WebViewJsUtil postMessage to native: " + paramFromJS);
            // auto版本，这里回调的是web的点击触摸事件，直接上报
            sendWebClick(paramFromJS);
        }
    }

    /**
     * picker版本注入的js支持这个js接口，可以拿到对应webview的信息，后续用于展示 evaluateJavascript的调用必须在主线程调用
     *
     * @param webView WebView
     */
    public static void getWebInfo(final WebView webView, final Handler handler) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(
                    getJsCodeUrl("TEAWebviewInfo();"),
                    new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String s) {
                            if (handler != null) {
                                if (TextUtils.isEmpty(s)) {
                                    LoggerImpl.global()
                                            .debug(loggerTags, "WebViewJsUtil getWebInfo:null!");
                                }
                                // s是否为空都要进行发送消息,通知该webView已经处理完毕
                                Message message = handler.obtainMessage();
                                message.obj = webView;
                                Bundle data = message.getData();
                                data.putString(BUNDLE_WEB_INFO, s);
                                handler.sendMessage(message);
                            }
                        }
                    });
        } else {
            // 圈选的sdk可以做一些限制，不支持就忽略，不返回数据
            LoggerImpl.global()
                    .warn(
                            loggerTags,
                            "WebViewJsUtil Doesn't support evaluateJavascript, can't get return value ");
        }
    }

    /**
     * 注入web事件采集后的回调接口
     *
     * @param webView WebView
     */
    @SuppressLint("AddJavascriptInterface")
    public static void injectNativeReportCallback(View webView) {
        LoggerImpl.global()
                .debug(loggerTags, "WebViewJsUtil add interface:TEANativeReport to: {}", webView);
        JavaScriptUtils.addJavascriptInterface(webView, new TeaNativeReport(), "TEANativeReport");
    }

    /**
     * 注入js代码 如果未设置picker，则为实现自动采集功能的js代码段 如有注入picker，则为可以返回对应webview全部数据的js代码段
     *
     * @param webView WebView
     */
    public static void injectCollectJs(View webView, String url) {
        LoggerImpl.global().debug(loggerTags, "WebViewJsUtil collect js to: {}", webView);

        // picker没有cookie,说明通过模拟器登陆,注入自动采集的js,否则注入圈选的js
        // 只要有一个实例有cookie则注入圈选 TODO: 该方案存在异常case，需要讨论方案优化
        boolean hasCookie =
                AppLogHelper.matchInstance(
                        new AppLogHelper.AppLogInstanceMatcher() {
                            @Override
                            public boolean match(AppLogInstance instance) {
                                return instance.isH5CollectEnable()
                                        && instance.getInitConfig() != null
                                        && instance.getInitConfig().getPicker() != null
                                        && !TextUtils.isEmpty(
                                        instance.getInitConfig()
                                                .getPicker()
                                                .getMarqueeCookie());
                            }
                        });
        String jsCode;
        if (hasCookie) {
            jsCode = JS_CODE_MARQUEE;
            LoggerImpl.global().debug(loggerTags, "WebViewJsUtil 圈选");
        } else {
            jsCode = JS_CODE_COLLECT;
            LoggerImpl.global().debug(loggerTags, "WebViewJsUtil 全埋点");
        }

        if (getJsCodeUrl(jsCode).equals(url)) {
            // 执行代码本身，则跳过
            return;
        }

        evaluateJavascript(webView, jsCode);
    }

    /**
     * 执行JS代码
     *
     * @param view WebView对象
     * @param code 代码
     */
    public static void evaluateJavascript(View view, String code) {
        evaluateJavascript(
                view,
                code,
                new JsValueCallback() {
                    @Override
                    public void cb(String result) {
                    }
                });
    }

    /**
     * 执行JS代码
     *
     * @param view WebView对象
     * @param code 代码
     */
    public static void evaluateJavascript(View view, String code, final JsValueCallback callback) {
        if (!ClassHelper.isWebView(view)) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ((WebView) view)
                    .evaluateJavascript(
                            code,
                            new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String s) {
                                    if (null != callback) callback.cb(s);
                                }
                            });

        } else {
            loadUrl(view, getJsCodeUrl(code), null);
        }
    }

    public interface JsValueCallback {
        void cb(String result);
    }

    /**
     * 加载网页
     *
     * @param view WebView对象
     * @param url  链接
     */
    public static void loadUrl(View view, String url, Map<String, String> headers) {
        if (!ClassHelper.isWebView(view)) {
            return;
        }
        try {
            Class<?> clazz = view.getClass();
            Method method = clazz.getMethod("loadUrl", String.class, Map.class);
            method.invoke(view, url, headers);
        } catch (Throwable e) {
            LoggerImpl.global().debug(loggerTags, "Reflect loadUrl failed", e);
        }
    }

    /**
     * 加载网页 loadData
     */
    public static void loadData(View webView, String data, String mimeType, String encoding) {
        if (!ClassHelper.isWebView(webView)) {
            return;
        }
        try {
            Class<?> clazz = webView.getClass();
            Method method = clazz.getMethod("loadData", String.class, String.class, String.class);
            method.invoke(webView, data, mimeType, encoding);
        } catch (Throwable e) {
            LoggerImpl.global().debug(loggerTags, "Reflect loadData failed", e);
        }
    }

    /**
     * 加载网页 loadDataWithBaseURL
     */
    public static void loadDataWithBaseURL(
            View webView,
            String baseUrl,
            String data,
            String mimeType,
            String encoding,
            String failUrl) {
        if (!ClassHelper.isWebView(webView)) {
            return;
        }
        try {
            Class<?> clazz = webView.getClass();
            Method method =
                    clazz.getMethod(
                            "loadDataWithBaseURL",
                            String.class,
                            String.class,
                            String.class,
                            String.class,
                            String.class);
            method.invoke(webView, baseUrl, data, mimeType, encoding, failUrl);
        } catch (Throwable e) {
            LoggerImpl.global().debug(loggerTags, "Reflect loadDataWithBaseURL failed", e);
        }
    }

    /**
     * JS代码转URL
     *
     * @param code 代码
     * @return URL
     */
    private static String getJsCodeUrl(String code) {
        return JS_URL_PREFIX + (TextUtils.isEmpty(code) ? "" : code);
    }

    /**
     * 传入web端采集的event字符串，转eventV3事件做上报
     *
     * @param message web端采集的event字符串
     */
    private static void sendWebClick(String message) {
        try {
            JSONArray jsonArray = new JSONArray(message);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                final String event = jsonObject.optString("event");
                final String time = jsonObject.optString("local_time_ms");
                final boolean isBav = jsonObject.optInt("is_bav") == 1;
                final JSONObject params = jsonObject.optJSONObject("params");
                final String pagePath = params != null ? params.optString("page_path") : "";
                if (EMPTY_PAGE.equals(pagePath)) return;
                if (!TextUtils.isEmpty(event)) {
                    AppLogHelper.receiveIf(
                            new AppLogHelper.BaseDataLoader() {
                                @Override
                                public BaseData load() {
                                    return create(event, time, isBav, params);
                                }
                            },
                            AppLogHelper.isH5CollectEnabledMatcher);
                }
            }
        } catch (Throwable e) {
            LoggerImpl.global().debug(loggerTags, "sendWebClick failed", e);
        }
    }

    /**
     * 如果event名为空，丢弃，其他事件上报
     */
    private static EventV3 create(String event, String time, boolean isBav, JSONObject params) {
        EventV3 eventV3 =
                new EventV3(null, event, isBav, params != null ? params.toString() : null);
        if (!TextUtils.isEmpty(event)) {
            try {
                eventV3.setTs(Long.parseLong(time));
            } catch (NumberFormatException e) {
                LoggerImpl.global().debug(loggerTags, "Create eventV3 failed", e);
            }
        }
        return eventV3;
    }

    /**
     * JS预置
     */
    private static final String JS_CODE_COLLECT =
            "!function(){\"use strict\";function t(t,e){if(!(t instanceof e))throw new TypeError(\"Cannot call a class as a function\")}function e(t,e){for(var n=0;n<e.length;n++){var r=e[n];r.enumerable=r.enumerable||!1,r.configurable=!0,\"value\"in r&&(r.writable=!0),Object.defineProperty(t,r.key,r)}}function n(t,n,r){return n&&e(t.prototype,n),r&&e(t,r),Object.defineProperty(t,\"prototype\",{writable:!1}),t}function r(t,e){if(\"function\"!=typeof e&&null!==e)throw new TypeError(\"Super expression must either be null or a function\");t.prototype=Object.create(e&&e.prototype,{constructor:{value:t,writable:!0,configurable:!0}}),Object.defineProperty(t,\"prototype\",{writable:!1}),e&&o(t,e)}function i(t){return i=Object.setPrototypeOf?Object.getPrototypeOf.bind():function(t){return t.__proto__||Object.getPrototypeOf(t)},i(t)}function o(t,e){return o=Object.setPrototypeOf?Object.setPrototypeOf.bind():function(t,e){return t.__proto__=e,t},o(t,e)}function a(t,e){if(e&&(\"object\"==typeof e||\"function\"==typeof e))return e;if(void 0!==e)throw new TypeError(\"Derived constructors may only return object or undefined\");return function(t){if(void 0===t)throw new ReferenceError(\"this hasn't been initialised - super() hasn't been called\");return t}(t)}function u(t){var e=function(){if(\"undefined\"==typeof Reflect||!Reflect.construct)return!1;if(Reflect.construct.sham)return!1;if(\"function\"==typeof Proxy)return!0;try{return Boolean.prototype.valueOf.call(Reflect.construct(Boolean,[],(function(){}))),!0}catch(t){return!1}}();return function(){var n,r=i(t);if(e){var o=i(this).constructor;n=Reflect.construct(r,arguments,o)}else n=r.apply(this,arguments);return a(this,n)}}function c(t,e){for(;!Object.prototype.hasOwnProperty.call(t,e)&&null!==(t=i(t)););return t}function s(){return s=\"undefined\"!=typeof Reflect&&Reflect.get?Reflect.get.bind():function(t,e,n){var r=c(t,e);if(!r)return;var i=Object.getOwnPropertyDescriptor(r,e);if(i.get)return i.get.call(arguments.length<3?t:n);return i.value},s.apply(this,arguments)}function l(t){if([\"LI\",\"TR\",\"DL\"].includes(t.nodeName))return!0;if(t.dataset&&t.dataset.hasOwnProperty(\"teaIdx\"))return!0;if(t.hasAttribute&&t.hasAttribute(\"data-tea-idx\"))return!0;return!1}function h(t){if(!t)return!1;if([\"A\",\"BUTTON\"].includes(t.nodeName))return!0;if(t.dataset&&t.dataset.hasOwnProperty(\"teaContainer\"))return!0;if(t.hasAttribute&&t.hasAttribute(\"data-tea-container\"))return!0;return!1}function f(t){for(var e=t;e&&!h(e);){if(\"HTML\"===e.nodeName||\"BODY\"===e.nodeName)return t;e=e.parentElement}return e||t}function d(t){for(var e=[];null!==t.parentElement;)e.push(t),t=t.parentElement;var n=[],r=[];return e.forEach((function(t){var e=function(t){if(null===t)return{str:\"\",index:0};var e=0,n=t.parentElement;if(n)for(var r=0;r<n.children.length&&n.children[r]!==t;r++)n.children[r].nodeName===t.nodeName&&e++;return{str:[t.nodeName.toLowerCase(),l(t)?\"[]\":\"\"].join(\"\"),index:e}}(t),i=e.str,o=e.index;n.unshift(i),r.unshift(o)})),{element_path:\"/\".concat(n.join(\"/\")),positions:r}}function p(t){var e={element_path:\"\",positions:[],texts:[]},n=d(t),r=n.element_path,i=n.positions,o=function(t){var e=f(t),n=[];return!function t(e){var r=function(t){var e=\"\";if(3===t.nodeType?e=t.textContent.trim():t.dataset&&t.dataset.hasOwnProperty(\"teaTitle\")||t.hasAttribute(\"data-tea-title\")?e=t.getAttribute(\"data-tea-title\"):t.hasAttribute(\"title\")?e=t.getAttribute(\"title\"):\"INPUT\"===t.nodeName&&[\"button\",\"submit\"].includes(t.getAttribute(\"type\"))?e=t.getAttribute(\"value\"):\"IMG\"===t.nodeName&&t.getAttribute(\"alt\")&&(e=t.getAttribute(\"alt\")),!e)return\"\";return e.slice(0,200)}(e);if(r&&-1===n.indexOf(r)&&n.push(r),e.childNodes.length>0)for(var i=e.childNodes,o=0;o<i.length;o++)8!==i[o].nodeType&&t(i[o])}(e),n}(t);e.element_path=r,e.positions=i.map((function(t){return\"\".concat(t)})),e.texts=o;var a=f(t);if(\"A\"===a.tagName&&(e.href=a.getAttribute(\"href\")),\"IMG\"===t.tagName){var u=t.getAttribute(\"src\");u&&0===u.trim().indexOf(\"data:\")&&(u=\"\"),e.src=u}return e.page_title=document.title,e.element_id=t.id,e.element_type=t.tagName,e}var v=function(){function e(n,r){var i=this;t(this,e),this.handler=function(t){var e=(t=i.getEvent(t)).target;if(!i.checkShouldTrackElement(e)||i.checkShouldIgnore(e))return;var n=i.getPositionData(e),r=i.getEventData(t,n),o=i.getElementData(e),a=i.getAllData(r,o,{element_width:Math.floor(n.element_width),element_height:Math.floor(n.element_height)});i.report(a)},this.info=n,this.autoTrack=r,this.listen()}return n(e,[{key:\"listen\",value:function(){this.autoTrack.root.addEventListener(this.info.eventType,this.handler,!0)}},{key:\"_checkShouldTrackElement\",value:function(t){return function(t){var e=window.innerHeight,n=window.innerWidth;if(1!==t.nodeType)return!1;if(function(t){for(var e=t.parentElement,n=!1;e;)\"svg\"===e.tagName.toLowerCase()?(e=null,n=!0):e=e.parentElement;return n}(t))return!1;if([\"HTML\",\"BODY\"].includes(t.tagName.toUpperCase()))return!1;var r=t;if(\"none\"===r.style.display)return!1;if(h(r))return!0;if(!function(t){if(t.children.length>0){var e=t.children;if([].slice.call(e).some((function(t){return t.children.length>0})))return!1;return!0}return!0}(r))return!1;if(r.clientHeight*r.clientWidth>.5*e*n)return!1;return!0}(t)}},{key:\"checkShouldTrackElement\",value:function(t){return!0}},{key:\"checkShouldIgnore\",value:function(t){return function(t){for(var e=t;e&&e.parentNode;){if(e.hasAttribute(\"data-tea-ignore\"))return!0;if(\"HTML\"===e.nodeName||\"body\"===e.nodeName)return!1;e=e.parentNode}return!1}(t)}},{key:\"getEvent\",value:function(t){return t}},{key:\"getEventData\",value:function(){var t=arguments.length>0&&void 0!==arguments[0]?arguments[0]:{},e=arguments.length>1&&void 0!==arguments[1]?arguments[1]:{},n=t.clientX,r=t.clientY,i=e.left,o=e.top;return{touch_x:Math.floor(n-i),touch_y:Math.floor(r-o)}}},{key:\"getElementData\",value:function(t){return p(t)}},{key:\"getPositionData\",value:function(t){if(!t)return;var e=t.getBoundingClientRect(),n=e.width,r=e.height;return{left:e.left,top:e.top,element_width:n,element_height:r}}},{key:\"getAllData\",value:function(){var t=arguments.length>0&&void 0!==arguments[0]?arguments[0]:{},e=arguments.length>1&&void 0!==arguments[1]?arguments[1]:{},n=arguments.length>2&&void 0!==arguments[2]?arguments[2]:{};return Object.assign(Object.assign(Object.assign(Object.assign({},t),e),n),{is_html:1,page_key:window.location.href})}},{key:\"report\",value:function(t){this.autoTrack.report(this.info.eventName,t)}},{key:\"destroy\",value:function(){this.autoTrack.root.removeEventListener(this.info.eventName,this.handler,!0),this.autoTrack=null}}]),e}(),y=function(e){r(a,e);var o=u(a);function a(e){var n;t(this,a),(n=o.call(this,{eventName:\"bav2b_page\"},e)).handler=function(t){if(n.checkHref())return;n._handler()},n._handler=function(){n.setHref();var t=n.getAllData();n.report(t)},n._listen();try{\"loading\"===document.readyState?document.addEventListener(\"DOMContentLoaded\",n._handler):setTimeout(n._handler)}catch(t){}return n}return n(a,[{key:\"listen\",value:function(){}},{key:\"_listen\",value:function(){var t=this;this._oldPushState=history.pushState,this._oldReplaceState=history.replaceState,history.pushState=function(){try{for(var e,n=arguments.length,r=new Array(n),i=0;i<n;i++)r[i]=arguments[i];return(e=t._oldPushState).call.apply(e,[history].concat(r))}finally{t._handler()}},history.replaceState=function(){try{for(var e,n=arguments.length,r=new Array(n),i=0;i<n;i++)r[i]=arguments[i];return(e=t._oldReplaceState).call.apply(e,[history].concat(r))}finally{t._handler()}},window.addEventListener(\"hashchange\",this.handler,!0),window.addEventListener(\"popstate\",this.handler,!0)}},{key:\"setHref\",value:function(t){this._currentHref=t||window.location.href}},{key:\"checkHref\",value:function(){return this._currentHref===window.location.href}},{key:\"getAllData\",value:function(){return Object.assign(Object.assign({},s(i(a.prototype),\"getAllData\",this).call(this)),{is_bav:1,page_key:this._currentHref,refer_page_key:document.referrer,page_title:document.title,page_path:this._currentHref,referrer_page_path:document.referrer})}},{key:\"destroy\",value:function(){window.removeEventListener(\"popstate\",this.handler,!0),window.removeEventListener(\"hashchange\",this.handler,!0),history.pushState=this._oldPushState,history.replaceState=this._oldReplaceState,this._oldPushState=null,this._oldReplaceState=null,this.autoTrack=null}}]),a}(v),g=function(e){r(a,e);var o=u(a);function a(e){return t(this,a),o.call(this,{eventType:\"click\",eventName:\"bav2b_click\"},e)}return n(a,[{key:\"checkShouldTrackElement\",value:function(t){return s(i(a.prototype),\"_checkShouldTrackElement\",this).call(this,t)}}]),a}(v),m=function(e){r(a,e);var o=u(a);function a(e){var n;return t(this,a),(n=o.call(this,{eventName:\"bav2b_click\"},e)).startHandler=function(t){var e=n.autoTrack.root,r=!1,i=function(){r=!0};e.addEventListener(\"touchmove\",i,!0),e.addEventListener(\"touchend\",(function t(o){r||n.handler(o),e.removeEventListener(\"touchmove\",i,!0),e.removeEventListener(\"touchend\",t,!0)}),!0)},n._listen(),n}return n(a,[{key:\"listen\",value:function(){}},{key:\"_listen\",value:function(){this.autoTrack.root.addEventListener(\"touchstart\",this.startHandler,!0)}},{key:\"getEvent\",value:function(t){return t.changedTouches[0]}},{key:\"checkShouldTrackElement\",value:function(t){if(!s(i(a.prototype),\"_checkShouldTrackElement\",this).call(this,t))return!1;if(\"input\"===t.nodeName.toLowerCase()&&[\"text\",\"password\",\"email\",\"tel\",\"number\",\"url\"].includes(t.type))return!1;return!0}},{key:\"destroy\",value:function(){this.autoTrack.root.removeEventListener(\"touchstart\",this.startHandler,!0),this.autoTrack=null}}]),a}(v),w=function(){function e(){t(this,e),this.version=\"1.2.2\",this.options={page:!1,touch:!1,click:!1},this.root=document,this.eventInstances=[],this.started=!1}return n(e,[{key:\"init\",value:function(t,e){this.options=Object.assign(Object.assign({},this.options),t),this.reportAdapter=e}},{key:\"start\",value:function(){if(this.started)return;this.started=!0;var t=this.options,e=t.page,n=t.click,r=t.touch;e&&this.eventInstances.push(new y(this)),n&&this.eventInstances.push(new g(this)),r&&this.eventInstances.push(new m(this))}},{key:\"stop\",value:function(){this.started=!1,this.eventInstances.forEach((function(t){return t.destroy()})),this.root=null}},{key:\"report\",value:function(){this.reportAdapter&&this.reportAdapter.apply(this,arguments)}}],[{key:\"getInstance\",value:function(){return e.instance||(e.instance=new e),e.instance}}]),e}();w.instance=null;var k,b,_;if(console.log(\"Winter Is Coming!\"),!window.TEAWebviewAutoTrack){var E=null,T=function(t){E||(E=window.TEANativeReport&&\"function\"==typeof window.TEANativeReport.postMessage?function(t){window.TEANativeReport.postMessage(t)}:window.TEANativeReport&&\"function\"==typeof window.TEANativeReport?function(t){window.TEANativeReport(t)}:window.webkit&&window.webkit.messageHandlers&&window.webkit.messageHandlers.TEANativeReport&&\"function\"==typeof window.webkit.messageHandlers.TEANativeReport.postMessage?function(t){window.webkit.messageHandlers.TEANativeReport.postMessage(t)}:function(t){}),E(t)},A=(k={page:!0,touch:!0,click:!1},b=function(){for(var t=arguments.length,e=new Array(t),n=0;n<t;n++)e[n]=arguments[n];var r=e[0],i=e[1],o=[{event:r,is_bav:1,local_time_ms:+new Date,params:i}];console.log(o[0]),T(JSON.stringify(o))},(_=w.getInstance()).init(k,b),_);A.start(),window.TEAWebviewAutoTrack=A}}();";
    private static final String JS_CODE_MARQUEE =
            "!function(){\"use strict\";function e(e){if([\"LI\",\"TR\",\"DL\"].includes(e.nodeName))return!0;if(e.dataset&&e.dataset.hasOwnProperty(\"teaIdx\"))return!0;if(e.hasAttribute&&e.hasAttribute(\"data-tea-idx\"))return!0;return!1}function n(n){for(var t=[];null!==n.parentElement;)t.push(n),n=n.parentElement;var r=[],i=[];return t.forEach((function(n){var t=function(n){if(null===n)return{str:\"\",index:0};var t=0,r=n.parentElement;if(r)for(var i=0;i<r.children.length&&r.children[i]!==n;i++)r.children[i].nodeName===n.nodeName&&t++;return{str:[n.nodeName.toLowerCase(),e(n)?\"[]\":\"\"].join(\"\"),index:t}}(n),o=t.str,a=t.index;r.unshift(o),i.unshift(a)})),{element_path:\"/\".concat(r.join(\"/\")),positions:i}}var t=window.__TEA_CHUNK_MAX__||524288;function r(e){try{return new Blob([e]).size}catch(i){for(var n=e.length,t=n-1;t>=0;t--){var r=e.charCodeAt(t);r>127&&r<=2047?n++:r>2047&&r<=65535&&(n+=2),r>=56320&&r<=57343&&t--}return n}}function i(e){if(r(e)<t)return[e];var n=encodeURIComponent(e),i=Math.ceil(r(n)/t);return new Array(i).fill(\"\").map((function(e,r){return n.substr(r*t,t)}))}var o=!1,a=1,u=window.innerWidth,c=window.innerHeight,f=new Set;var l=function(e){var n=e._element_path,t=e.positions,r=e.children;e._checkList=!0;var i=n.split(\"/\").length-2;if(e.fuzzy_positions||(e.fuzzy_positions=[].concat(t)),e.fuzzy_positions[i]=\"*\",r){!function e(n){n.forEach((function(n){n.fuzzy_positions||(n.fuzzy_positions=[].concat(n.positions)),n.fuzzy_positions[i]=\"*\",n.children&&e(n.children)}))}(r)}},s=function e(t){return Array.prototype.slice.call(t,0).reduce((function(t,r){if(!r)return t;var i=r.nodeName;if(function(e){return[\"script\",\"link\",\"style\",\"embed\"].includes(e)}(i=i.toLowerCase())||function(e){var n=getComputedStyle(e,null);if(\"none\"===n.getPropertyValue(\"display\"))return!0;if(\"0\"===n.getPropertyValue(\"opacity\"))return!0;return!1}(r))return t;var o={};if(!function(e){return[\"button\",\"select\"].includes(e)}(i)&&r.children){var s=e(r.children);s&&s.length&&(o={children:s})}var h=function(e){var n=arguments.length>1&&void 0!==arguments[1]?arguments[1]:1,t=e.getBoundingClientRect().toJSON();if(1===n)return t;return Object.keys(t).reduce((function(e,r){return e[r]=Math.ceil(t[r]*n),e}),{})}(r,a);if(!function(e,n){var t=e.left,r=e.right,i=e.top,o=e.bottom,a=e.width,u=e.height;if(!(a>0&&u>0))return!1;if(t>window.innerWidth||r<0||i>window.innerHeight||o<0)return!1;return!0}(h))return o.children&&o.children.forEach((function(e){return t.push(e)})),t;h=function(e){var n={x:e.left,y:e.top,width:e.width,height:e.height};return e.top<0&&(n.y=0,n.height+=e.top),e.bottom>c&&(n.height=c-n.y),e.left<0&&(n.x=0,n.width+=e.left),e.right>u&&(n.width=u-n.x),Object.keys(n).forEach((function(e){n[e]=Math.floor(n[e])})),n}(h);var d=function(e){var t=n(e),r=t.element_path,i=t.positions.map((function(e){return\"\".concat(e)})),o=[].concat(i).reverse(),a=!1;if(-1!==r.indexOf(\"[]\")){a=!0;var u=!1;r.split(\"/\").reverse().forEach((function(e,n){u||-1===e.indexOf(\"[]\")||(u=!0,o[n]=\"*\")}))}var c=e.id,f=e.tagName,l=[\"absolute\",\"fixed\"],s=0,h=getComputedStyle(e,null).getPropertyValue(\"z-index\");\"auto\"!==h&&(s=parseInt(String(h),10));for(var d=e.parentElement;d;){var p=getComputedStyle(d,null);if(l.includes(p.getPropertyValue(\"position\"))){s+=1e4;break}d=d.parentElement}return Object.assign({element_id:c,element_type:f,_element_path:r,element_path:\"\".concat(r,\"/*\"),positions:i.concat(\"*\"),zIndex:s},a?{fuzzy_positions:o.reverse().concat(\"*\")}:{})}(r),p=d._element_path,v=!1;if(f.has(p))v=!0;else{var m=r.parentElement;if(m){var g=m.children,w=Array.from(g).filter((function(e){return e.nodeName.toLowerCase()===i})),y=w.length;if(y>=3){var _=Array.from(r.classList),b=_.length,z=Array.from(r.children).map((function(e){return e.nodeName.toLowerCase()})).join(\",\"),A=0;Array.from(w).forEach((function(e){if(e===r)return A++,void 0;var n=!1;if(b){var t=Array.from(e.classList);_.length+t.length-new Set([].concat(t,_)).size>0&&(n=!0)}else n=!0;if(n){var i=!1;if(z){var o=Array.from(e.children).map((function(e){return e.nodeName.toLowerCase()})).join(\",\");o===z&&(i=!0)}else i=!0;i&&A++}})),A>=3&&A/y>=.5&&(v=!0),v&&f.add(p)}}}return d=Object.assign(Object.assign({nodeName:i,frame:h},d),o),v&&l(d),d.children&&d.children.forEach((function(e){var n=e._element_path,t=e._checkList;f.has(n)&&!t&&l(e)})),t.push(d),t}),[])},h=function(){if(o)return;o=!0,a=function(){try{var e=window.outerWidth/window.innerWidth;if(1===e)return 1;if(e)return e;var n=document.querySelector('meta[name=\"viewport\"]');if(n){var t=n.content.match(/initial-scale=(.*?)(,|$)/);if(t&&t[1]){var r=parseFloat(t[1]);if(r)return r}}}catch(e){return 1}return 1}(),u=window.innerWidth*a,c=window.innerHeight*a,f=new Set},d=function(e){return JSON.stringify(e)};if(!window.TEAWebviewInfo){var p=[],v=function(){var e=arguments.length>0&&void 0!==arguments[0]&&arguments[0];if(p.length)return console.log(p.length,p),d({value:p.shift(),done:!p.length});h();try{var n=s(document.querySelectorAll(\"body > *\")),t={page:window.location.href,info:n},r=d(t);if(console.log(r),!e)return r;if(1===(p=i(r)).length)return p.shift();return console.log(p.length,p),d({value:p.shift(),done:!1})}catch(e){console.log(e)}return\"\"};v.version=\"1.2.2\",window.TEAWebviewInfo=v}}();";
}
