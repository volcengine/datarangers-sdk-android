// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.server;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION;
import android.text.TextUtils;

import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.InitConfig;
import com.bytedance.applog.UriConfig;
import com.bytedance.applog.log.EventBus;
import com.bytedance.applog.log.LogInfo;
import com.bytedance.applog.log.LogUtils;
import com.bytedance.applog.manager.ConfigManager;
import com.bytedance.applog.manager.DeviceManager;
import com.bytedance.applog.network.INetworkClient;
import com.bytedance.applog.network.RangersHttpException;
import com.bytedance.applog.network.RangersHttpTimeoutException;
import com.bytedance.applog.util.EncryptUtils;
import com.bytedance.applog.util.JsonUtils;
import com.bytedance.applog.util.TLog;
import com.bytedance.applog.util.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

/**
 * @author shiyanlong
 * @date 2019/1/16
 */
public class Api {

    public static final int HTTP_DEFAULT_TIMEOUT = 60 * 1000;
    public static final int HTTP_SHORT_TIMEOUT = 5 * 1000;

    public static final String KEY_DEVICE_TOKEN = "device_token";

    private static final int LENGTH_MAX = 1024 * 10;

    public static final String KEY_OPEN_UDID = "openudid";

    public static final String KEY_C_UDID = "clientudid";

    public static final String KEY_SSID = "ssid";

    public static final String KEY_AID = "aid";

    public static final String KEY_SIG_HASH = "sig_hash";

    public static final String KEY_SDK_VERSION = "sdk_version";

    public static final String KEY_SDK_VERSION_CODE = "sdk_version_code";

    public static final String KEY_SDK_VERSION_NAME = "sdk_version_name";

    public static final String KEY_PACKAGE = "package";

    public static final String KEY_REAL_PACKAGE_NAME = "real_package_name";

    public static final String KEY_CHANNEL = "channel";

    public static final String KEY_TOUCH_POINT = "touch_point";

    public static final String KEY_DISPLAY_NAME = "display_name";

    public static final String KEY_APP_VERSION = "app_version";
    public static final String KEY_EVENT_APP_VERSION = "$app_version";

    public static final String KEY_VERSION_CODE = "version_code";

    public static final String KEY_TIMEZONE = "timezone";

    public static final String KEY_ACCESS = "access";

    public static final String KEY_OS = "os";

    public static final String KEY_PLATFORM = "platform";
    public static final String KEY_SDK_LIB = "sdk_lib";

    public static final String KEY_OS_VERSION = "os_version";

    public static final String KEY_OS_API = "os_api";

    public static final String KEY_ROM = "rom";

    public static final String KEY_DEVICE_MODEL = "device_model";

    public static final String KEY_DEVICE_BRAND = "device_brand";

    public static final String KEY_DEVICE_MANUFACTURER = "device_manufacturer";

    public static final String KEY_CPU_ABI = "cpu_abi";

    public static final String KEY_LANGUAGE = "language";

    public static final String KEY_RESOLUTION = "resolution";

    public static final String KEY_DISPLAY_DENSITY = "display_density";

    public static final String KEY_DENSITY_DPI = "density_dpi";

    public static final String KEY_UDID = "udid";

    public static final String KEY_UDID_LIST = "udid_list";

    public static final String KEY_CARRIER = "carrier";

    public static final String KEY_MCC_MNC = "mcc_mnc";

    public static final String KEY_RELEASE_BUILD = "release_build";

    public static final String REQ_ID = "req_id";

    /** 用于服务端判断是否通过 applog 下发 push 通道 */
    public static final String KEY_NOT_REQUEST_SENDER = "not_request_sender";

    public static final String KEY_UPDATE_VERSION_CODE = "update_version_code";

    public static final String KEY_MANIFEST_VERSION_CODE = "manifest_version_code";

    public static final String KEY_REGION = "region";

    public static final String KEY_TZ_NAME = "tz_name";

    public static final String KEY_TZ_OFFSET = "tz_offset";

    public static final String KEY_SIM_REGION = "sim_region";

    public static final String KEY_CUSTOM = "custom";

    public static final String KEY_APPKEY = "appkey";

    public static final String KEY_ROM_VERSION = "rom_version";

    public static final String KEY_TRACER_DATA = "tracer_data";

    /** 内部版无user_unique_id，仅有user_id */
    public static final String KEY_USER_ID = "user_id";

    public static final String KEY_USER_UNIQUE_ID = "user_unique_id";

    public static final String KEY_USER_UNIQUE_ID_TYPE = "user_unique_id_type";

    public static final String KEY_USER_UNIQUE_ID_TYPE_NEW = "$user_unique_id_type";

    public static final String KEY_APP_TRACK = "app_track";

    public static final String KEY_GOOGLE_AID = "google_aid";

    public static final String KEY_APP_LANGUAGE = "app_language";

    public static final String KEY_APP_REGION = "app_region";

    public static final String KEY_INSTALL_ID = "install_id";

    public static final String KEY_DEVICE_ID = "device_id";

    public static final String KEY_OLD_DID = "old_did";

    public static final String KEY_BD_DID = "bd_did";

    /** 加密的did */
    public static final String KEY_CD = "cd";

    public static final String KEY_USER_AGENT = "user_agent";

    private static final String KEY_SERVER_TIME = "server_time";

    public static final String KEY_REGISTER_TIME = "register_time";

    private static final String MSG_OK = "success";

    private static final String KEY_MSG = "message";

    public static final String KEY_MAGIC = "magic_tag";

    public static final String MSG_MAGIC = "ss_app_log";

    public static final String KEY_TIME_SYNC = "time_sync";

    /** 修正客户端时间 */
    public static JSONObject mTimeSync;

    public static final String KEY_LOCAL_TIME = "local_time";


    public static final String KEY_AB_SDK_VERSION = "ab_sdk_version";

    public static final String KEY_AB_TEST_EXPOSURE = "abtest_exposure";

    public static final String KEY_AB_VERSION = "ab_version";

    public static final String KEY_HEADER = "header";

    public static final String KEY_SERIAL_NUMBER = "serial_number";

    public static final String KEY_SIM_SERIAL_NUMBER = "sim_serial_number";

    public static final String KEY_CLEAR_KEY_PREFIX = "clear_key_prefix";

    public static final String CONTENT_TYPE = "application/json; charset=utf-8";

    // TODO: 调用时，增加加密逻辑。
    public static final String KEY_APP_NAME = "app_name";

    public static final String KEY_TWEAKED_CHANNEL = "tweaked_channel";

    public static final String KEY_APP_VERSION_MINOR = "app_version_minor";

    public static final String KEY_SET_COOKIE = "Set-Cookie";

    protected static final String KEY_COOKIES = "Cookie";

    public static final String KEY_SDK_TARGET_VERSION = "sdk_target_version";

    public static final String KEY_GIT_HASH = "git_hash";

    private static final int HTTP_FAIL = 101;

    private static final int HTTP_NO_MESSAGE = 102;

    private static final String PATH_SIMULATE_LOGIN = "/simulator/mobile/login";
    private static final String PATH_SIMULATE_LOGIN_NO_QR = "/simulator/limited_mobile/try_link";
    private static final String PATH_SIMULATE_EVENT_VERIFY = "/simulator/mobile/log";

    private String sSchemeHost = "https://databyterangers.com.cn";

    public static final String KEY_DATETIME = "datetime";
    public static final String KEY_SESSION_ID = "session_id";
    public static final String KEY_LOCAL_TIME_MS = "local_time_ms";
    public static final String KEY_EVENT_INDEX = "tea_event_index";
    public static final String KEY_ACTIVITES = "activites";
    public static final String KEY_LAUNCH_FROM = "launch_from";
    public static final String KEY_LAUNCH = "launch";
    public static final String KEY_TERMINATE = "terminate";
    public static final String KEY_V3 = "event_v3";
    public static final String KEY_LOG_DATA = "log_data";
    public static final String KEY_SCREEN_ORIENTATION = "$screen_orientation";
    public static final String KEY_GPS_LONGITUDE = "$longitude";
    public static final String KEY_GPS_LATITUDE = "$latitude";
    public static final String KEY_GPS_GCS = "$geo_coordinate_system";
    public static final String KEY_EVENT_DURATION = "$event_duration";
    public static final String KEY_PAGE_DURATION = "$page_duration";
    public static final String LEAVE_PAGE_EVENT_NAME = "$bav2b_page_leave";

    public static final String KEY_RESPONSE_LOG_ID = "x-tt-logid";

    protected final AppLogInstance appLogInstance;
    private final EncryptUtils encryptUtils;

    public Api(final AppLogInstance appLogInstance) {
        this.appLogInstance = appLogInstance;
        this.encryptUtils = new EncryptUtils(appLogInstance);
    }

    public EncryptUtils getEncryptUtils() {
        return encryptUtils;
    }

    private String addQuery(String url, String key, String value) {
        try {
            if (TextUtils.isEmpty(key) || TextUtils.isEmpty(value)) {
                return url;
            }
            Uri uri = Uri.parse(url);
            return uri.buildUpon().appendQueryParameter(key, value).build().toString();
        } catch (Throwable e) {
            appLogInstance.getLogger().error(LogInfo.Category.REQUEST, "addQuery", e);
        }
        return url;
    }

    public static String filterQuery(final String url, final String[] filter) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }
        Uri u = Uri.parse(url);
        HashMap<String, String> filterMap = new HashMap<>(filter.length);
        for (String key : filter) {
            String value = u.getQueryParameter(key);
            if (!TextUtils.isEmpty(value)) {
                filterMap.put(key, value);
            }
        }
        Uri.Builder builder = u.buildUpon();
        builder.clearQuery();
        for (String key : filterMap.keySet()) {
            builder.appendQueryParameter(key, filterMap.get(key));
        }
        return builder.build().toString();
    }

    public static void addParams(StringBuilder sb, String key, String param) {
        if (sb == null || TextUtils.isEmpty(key) || TextUtils.isEmpty(param)) {
            return;
        }
        String url = sb.toString();
        if (url.indexOf('?') < 0) {
            sb.append("?");
        } else {
            sb.append("&");
        }

        sb.append(key).append("=").append(Uri.encode(param));
    }

    private void updateTimeDiff(final JSONObject o) {
        try {
            long serverTime = o.optLong(KEY_SERVER_TIME);
            if (serverTime > 0L) {
                final JSONObject ts = new JSONObject();
                ts.put(KEY_SERVER_TIME, serverTime);
                long localTime = System.currentTimeMillis() / 1000L;
                ts.put(KEY_LOCAL_TIME, localTime);
                mTimeSync = ts;
                if (!LogUtils.isDisabled()) {
                    LogUtils.sendJsonFetcher(
                            "server_time_sync",
                            new EventBus.DataFetcher() {
                                @Override
                                public Object fetch() {
                                    JSONObject params = new JSONObject();
                                    JsonUtils.mergeJsonObject(ts, params);
                                    try {
                                        params.put("appId", appLogInstance.getAppId());
                                    } catch (Throwable ignored) {
                                    }
                                    return params;
                                }
                            });
                }
            }
        } catch (Exception ignore) {
        }
    }

    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>(2);
        InitConfig initConfig = appLogInstance.getInitConfig();
        if (null != initConfig) {
            Map<String, String> httpHeaders = initConfig.getHttpHeaders();
            if (null != httpHeaders && !httpHeaders.isEmpty()) {
                headers.putAll(httpHeaders);
            }
        }
        return EncryptUtils.putContentTypeHeader(headers, appLogInstance);
    }

    /** 是否服务器返回的堵塞消息 */
    public static boolean checkIfJamMsg(int code) {
        return code >= 500 && code < 600;
    }

    protected static JSONObject getHeader(String aid, String appVersion) throws JSONException {
        JSONObject header = new JSONObject();
        header.put("aid", aid);
        header.put("os", "Android");
        header.put("os_version", String.valueOf(VERSION.SDK_INT));
        header.put("sdk_version", TLog.SDK_VERSION_NAME);
        header.put("app_version", appVersion);
        return header;
    }

    public JSONObject config(String uri, final JSONObject request) {
        appLogInstance
                .getLogger()
                .debug(
                        LogInfo.Category.REQUEST,
                        "Start to get config to uri:{} with request:{}...",
                        uri,
                        request);

        HashMap<String, String> headers = getHeaders();
        String response = null;
        try {
            response = requestWithKeyIv(request, uri, headers, HTTP_DEFAULT_TIMEOUT);
        } catch (Throwable e) {
            appLogInstance.getLogger().error(LogInfo.Category.REQUEST, "Config failed", e);
        }

        appLogInstance
                .getLogger()
                .debug(LogInfo.Category.REQUEST, "Get config with response:{}", response);
        JSONObject obj = handleTimeDiff(response);
        boolean success = obj != null && Api.MSG_MAGIC.equals(obj.optString(KEY_MAGIC, ""));
        if (success) {
            return obj.optJSONObject("config");
        } else {
            return null;
        }
    }

    public JSONObject abConfig(String uri, final JSONObject request, int timeout)
            throws RangersHttpTimeoutException {
        appLogInstance
                .getLogger()
                .debug(
                        LogInfo.Category.REQUEST,
                        "Start to get ab config to uri:{} with request:{}...",
                        uri,
                        request);

        HashMap<String, String> header = getHeaders();
        String response;
        try {
            response = requestWithKeyIv(request, uri, header, timeout);
        } catch (Throwable e) {
            appLogInstance.getLogger().error(LogInfo.Category.REQUEST, "Post ab config failed", e);
            if (e instanceof RangersHttpTimeoutException) {
                throw (RangersHttpTimeoutException) e;
            }
            return null;
        }

        appLogInstance
                .getLogger()
                .debug(LogInfo.Category.REQUEST, "Get ab config with response:{}", response);

        JSONObject obj = handleTimeDiff(response);
        boolean success = obj != null && Api.MSG_OK.equals(obj.optString(KEY_MSG, ""));
        if (success) {
            return obj.optJSONObject("data");
        } else {
            return null;
        }
    }

    private String requestWithKeyIv(
            JSONObject request, String uri, Map<String, String> header, int timeout)
            throws RangersHttpException {
        String response = null;
        String key = request.optString("key");
        String iv = request.optString("iv");
        boolean isStream = !TextUtils.isEmpty(key) && !TextUtils.isEmpty(iv);
        byte[] respByteArray =
                appLogInstance
                        .getNetClient()
                        .execute(
                                INetworkClient.METHOD_POST,
                                uri,
                                request,
                                header,
                                isStream
                                        ? INetworkClient.RESPONSE_TYPE_STREAM
                                        : INetworkClient.RESPONSE_TYPE_STRING,
                                true,
                                timeout);
        if (isStream) {
            boolean isNeedRequestAgainWithoutStreamMode = false;
            if (respByteArray == null) {
                isNeedRequestAgainWithoutStreamMode = true;
            } else {
                byte[] decryptedData = EncryptUtils.decryptAesCbc(respByteArray, key, iv);
                if (decryptedData != null) {
                    byte[] bytes = EncryptUtils.gzipUncompress(decryptedData);
                    if (bytes != null) {
                        response = new String(bytes);
                    } else {
                        isNeedRequestAgainWithoutStreamMode = true;
                    }
                } else {
                    response = new String(respByteArray);
                }
            }

            if (isNeedRequestAgainWithoutStreamMode) {
                response =
                        new String(
                                appLogInstance
                                        .getNetClient()
                                        .execute(
                                                INetworkClient.METHOD_POST,
                                                uri,
                                                request,
                                                header,
                                                INetworkClient.RESPONSE_TYPE_STRING,
                                                true,
                                                HTTP_DEFAULT_TIMEOUT));
            }
        } else {
            response = new String(respByteArray);
        }
        return response;
    }

    private JSONObject handleTimeDiff(String response) {
        if (response != null) {
            try {
                JSONObject obj = new JSONObject(response);
                updateTimeDiff(obj);
                return obj;
            } catch (Throwable e) {
                appLogInstance.getLogger().error(LogInfo.Category.REQUEST, "JSON handle failed", e);
            }
        }
        return null;
    }

    public JSONObject simulateLogin(
            final String aid,
            String appVersion,
            int width,
            int height,
            String deviceId,
            String qrParam) {
        appLogInstance
                .getLogger()
                .debug(
                        LogInfo.Category.REQUEST,
                        "Start to login simulator with device id:{} and qrParam:{}...",
                        deviceId,
                        qrParam);

        JSONObject request = new JSONObject();
        try {
            JSONObject header = getHeader(aid, appVersion);
            header.put("width", width);
            header.put("height", height);
            header.put("device_id", deviceId);
            request.put("header", header);

            request.put("qr_param", qrParam);
        } catch (Throwable e) {
            appLogInstance.getLogger().error(LogInfo.Category.REQUEST, "JSON handle failed", e);
            return null;
        }

        HashMap<String, String> header = getHeaders();
        try {
            String resp =
                    new String(
                            appLogInstance
                                    .getNetClient()
                                    .execute(
                                            INetworkClient.METHOD_POST,
                                            sSchemeHost + PATH_SIMULATE_LOGIN,
                                            request,
                                            header,
                                            INetworkClient.RESPONSE_TYPE_STRING,
                                            true,
                                            HTTP_SHORT_TIMEOUT));

            appLogInstance
                    .getLogger()
                    .debug(LogInfo.Category.REQUEST, "Login simulator with response:{}", resp);

            if (Utils.isEmpty(resp)) {
                return null;
            }
            return new JSONObject(resp);
        } catch (Throwable e) {
            appLogInstance.getLogger().error(LogInfo.Category.REQUEST, "Login simulator failed", e);
        }
        return null;
    }

    public JSONObject simulateLoginWithoutQR(
            final AsyncTask<?, ?, ?> simulateLoginTask,
            final String aid,
            String appVersion,
            int width,
            int height,
            String deviceId) {
        appLogInstance
                .getLogger()
                .debug(
                        LogInfo.Category.REQUEST,
                        "Start to login simulator with device id:{}...",
                        deviceId);

        JSONObject request = new JSONObject();
        try {
            JSONObject header = getHeader(aid, appVersion);
            Utils.copy(header, appLogInstance.getHeader());
            header.put("width", width);
            header.put("height", height);
            header.put("device_id", deviceId);
            header.put(Api.KEY_DEVICE_MODEL, Build.MODEL);
            request.put("header", header);
        } catch (Throwable e) {
            appLogInstance.getLogger().error(LogInfo.Category.REQUEST, "JSON handle failed", e);
            return null;
        }

        HashMap<String, String> header = getHeaders();
        String resp = null;
        String sync_id = "";
        // try two minute
        while (!simulateLoginTask.isCancelled()) {
            long startRequest = System.currentTimeMillis();
            try {
                request.put("sync_id", sync_id);
                resp =
                        new String(
                                appLogInstance
                                        .getNetClient()
                                        .execute(
                                                INetworkClient.METHOD_POST,
                                                sSchemeHost + PATH_SIMULATE_LOGIN_NO_QR,
                                                request,
                                                header,
                                                INetworkClient.RESPONSE_TYPE_STRING,
                                                true,
                                                HTTP_SHORT_TIMEOUT));
                JSONObject rspJSON = new JSONObject(resp);
                int retry = rspJSON.getJSONObject("data").getInt("retry");
                if (retry == 0) {
                    return null;
                } else if (retry == 2) {
                    break;
                }
                sync_id = rspJSON.getJSONObject("data").getString("sync_id");
            } catch (Throwable e) {
                appLogInstance
                        .getLogger()
                        .error(LogInfo.Category.REQUEST, "Post to simulate login failed", e);
            }
            long elapse = System.currentTimeMillis() - startRequest;
            if (elapse < 1000) {
                try {
                    Thread.sleep(1000 - elapse);
                } catch (InterruptedException e) {
                    appLogInstance
                            .getLogger()
                            .error(LogInfo.Category.REQUEST, "Sleep interrupted", e);
                }
            }
        }

        appLogInstance
                .getLogger()
                .debug(LogInfo.Category.REQUEST, "Login simulator with response:{}", resp);

        if (Utils.isEmpty(resp)) {
            return null;
        }
        try {
            return new JSONObject(resp);
        } catch (Throwable e) {
            appLogInstance.getLogger().error(LogInfo.Category.REQUEST, "JSON handle failed", e);
        }
        return null;
    }

    public boolean sendToRangersEventVerify(final JSONObject event, final String cookie) {
        appLogInstance
                .getLogger()
                .debug(
                        LogInfo.Category.REQUEST,
                        "Start to send event:{} with cookie:{} to et...",
                        event,
                        cookie);

        JSONObject request = new JSONObject();
        try {
            JSONObject header = appLogInstance.getHeader();
            request.put("header", header);
            if (event != null) {
                JSONArray eventArray = new JSONArray();
                eventArray.put(event);
                request.put("event_v3", eventArray);
            }
        } catch (Throwable e) {
            appLogInstance.getLogger().error(LogInfo.Category.REQUEST, "JSON handle failed", e);
        }
        HashMap<String, String> header = getHeaders();
        header.put(KEY_COOKIES, cookie);
        try {
            String resp =
                    new String(
                            appLogInstance
                                    .getNetClient()
                                    .execute(
                                            INetworkClient.METHOD_POST,
                                            sSchemeHost + PATH_SIMULATE_EVENT_VERIFY,
                                            request,
                                            header,
                                            INetworkClient.RESPONSE_TYPE_STRING,
                                            true,
                                            HTTP_SHORT_TIMEOUT));
            JSONObject jsonResp = new JSONObject(resp);
            JSONObject data = jsonResp.getJSONObject("data");
            boolean keep = data.optBoolean("keep", false);
            if (!keep) {
                appLogInstance.setRangersEventVerifyEnable(false, cookie);
            }
            appLogInstance
                    .getLogger()
                    .debug(LogInfo.Category.REQUEST, "Send event to et with response:{}", resp);
        } catch (Throwable e) {
            appLogInstance
                    .getLogger()
                    .error(LogInfo.Category.REQUEST, "Post to event verify failed", e);
            return false;
        }
        return true;
    }

    public void setSchemeHost(String host) {
        sSchemeHost = host;
    }

    public String getSchemeHost() {
        return sSchemeHost;
    }

    public int sendLog(String[] uris, final JSONObject data, final ConfigManager config) {
        HashMap<String, String> headers = getHeaders();
        int resultCode = HTTP_NO_MESSAGE;
        JSONObject o = null;
        for (final String uriSendHeader : uris) {
            try {
                String response =
                        new String(
                                appLogInstance
                                        .getNetClient()
                                        .execute(
                                                INetworkClient.METHOD_POST,
                                                uriSendHeader,
                                                data,
                                                headers,
                                                INetworkClient.RESPONSE_TYPE_STRING,
                                                true,
                                                HTTP_DEFAULT_TIMEOUT));
                if (!TextUtils.isEmpty(response)) {
                    o = new JSONObject(response);
                    updateTimeDiff(o);
                    if (MSG_MAGIC.equals(o.optString(KEY_MAGIC))) {
                        if (MSG_OK.equals(o.optString(KEY_MSG))) {
                            resultCode = HttpURLConnection.HTTP_OK;
                            break;
                        } else {
                            resultCode = HTTP_FAIL;
                        }
                    } else {
                        resultCode = HTTP_NO_MESSAGE;
                    }
                }
            } catch (Exception e) {
                if (e instanceof RangersHttpException) {
                    resultCode = ((RangersHttpException) e).getResponseCode();
                    // 启动拥塞控制，收到特定消息才能跳过重试
                    if (config.getInitConfig().isCongestionControlEnable()
                            && checkIfJamMsg(resultCode)) {
                        break;
                    }
                } else {
                    appLogInstance
                            .getLogger()
                            .error(LogInfo.Category.REQUEST, "Send to server failed", e);
                    // 默认值
                }
            }
        }

        if (resultCode == HttpURLConnection.HTTP_OK && o != null) {
            config.updateLogRespConfig(o);
            config.parseEventConfigList(o);
        }

        return resultCode;
    }

    public JSONObject register(String uri, final JSONObject request) {
        appLogInstance
                .getLogger()
                .debug(
                        LogInfo.Category.REQUEST,
                        "Start to register to uri:{} with request:{}...",
                        uri,
                        request);

        HashMap<String, String> headers = getHeaders();
        String response = null;
        try {
            response =
                    new String(
                            appLogInstance
                                    .getNetClient()
                                    .execute(
                                            INetworkClient.METHOD_POST,
                                            encryptUtils.encryptUrl(uri),
                                            request,
                                            headers,
                                            INetworkClient.RESPONSE_TYPE_STRING,
                                            true,
                                            HTTP_DEFAULT_TIMEOUT));
            appLogInstance
                    .getLogger()
                    .debug(LogInfo.Category.REQUEST, "request register success: {}", response);
        } catch (Throwable e) {
            appLogInstance.getLogger().error(LogInfo.Category.REQUEST, "request register error", e);
        }
        return handleTimeDiff(response);
    }

    public JSONObject bindID(String uri, final JSONObject request) {
        appLogInstance
                .getLogger()
                .debug(
                        LogInfo.Category.REQUEST,
                        "Start to bind id to uri:{} with request:{}...",
                        uri,
                        request);

        HashMap<String, String> headers = getHeaders();
        String response = null;
        try {
            response =
                    new String(
                            appLogInstance
                                    .getNetClient()
                                    .execute(
                                            INetworkClient.METHOD_POST,
                                            encryptUtils.encryptUrl(uri),
                                            request,
                                            headers,
                                            INetworkClient.RESPONSE_TYPE_STRING,
                                            true,
                                            HTTP_DEFAULT_TIMEOUT));
            appLogInstance
                    .getLogger()
                    .debug(LogInfo.Category.REQUEST, "bindId success: {}", response);
        } catch (Exception e) {
            appLogInstance.getLogger().error(LogInfo.Category.REQUEST, "bindId error", e);
        }
        return handleTimeDiff(response);
    }

    public byte[] httpRequestInner(
            final byte method,
            final String urlString,
            final Map<String, String> headers,
            final JSONObject data,
            final boolean encrypt,
            final byte respType,
            final int timeout)
            throws RangersHttpException {
        appLogInstance
                .getLogger()
                .debug(LogInfo.Category.REQUEST, "Start request http url: {}", urlString);
        if (appLogInstance.isDebugMode()) {
            if (headers != null) {
                try {
                    for (HashMap.Entry<String, String> entry : headers.entrySet()) {
                        if (!TextUtils.isEmpty(entry.getKey())
                                && !TextUtils.isEmpty(entry.getValue())) {
                            appLogInstance
                                    .getLogger()
                                    .debug(
                                            LogInfo.Category.REQUEST,
                                            "http headers key:"
                                                    + entry.getKey()
                                                    + " value:"
                                                    + entry.getValue());
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        }

        DataOutputStream dataOutputStream = null;
        BufferedReader reader = null;
        InputStream is = null;
        ByteArrayOutputStream baos = null;
        final long startTimestamp = System.currentTimeMillis();
        final String requestId = UUID.randomUUID().toString();
        byte[] responseBytes = new byte[0];
        HttpURLConnection conn = null;
        try {
            final URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            if (conn instanceof HttpsURLConnection) {
                InitConfig initConfig = appLogInstance.getInitConfig();
                if (null != initConfig) {
                    SSLSocketFactory sslSocketFactory = initConfig.getSslSocketFactory();
                    if (null != sslSocketFactory) {
                        ((HttpsURLConnection) conn).setSSLSocketFactory(sslSocketFactory);
                        appLogInstance
                                .getLogger()
                                .debug(LogInfo.Category.REQUEST, "use sslSocketFactory: {}", sslSocketFactory);
                    }
                }
            }
            if (timeout > 0) {
                conn.setConnectTimeout(timeout);
                conn.setReadTimeout(timeout);
            }
            if (method == INetworkClient.METHOD_GET) {
                conn.setDoOutput(false);
                conn.setRequestMethod("GET");
            } else if (method == INetworkClient.METHOD_POST) {
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
            } else {
                appLogInstance.getLogger().warn(LogInfo.Category.REQUEST, "Unknown method");
                return null;
            }
            if (headers != null && !headers.isEmpty()) {
                for (HashMap.Entry<String, String> entry : headers.entrySet()) {
                    if (!TextUtils.isEmpty(entry.getKey())
                            && !TextUtils.isEmpty(entry.getValue())) {
                        conn.addRequestProperty(entry.getKey(), entry.getValue());
                    } else {
                        appLogInstance
                                .getLogger()
                                .error(LogInfo.Category.REQUEST, "Header key is empty");
                    }
                }
            }
            conn.setRequestProperty("Accept-Encoding", "gzip");

            final HttpURLConnection finalConn = conn;
            if (!LogUtils.isDisabled()) {
                LogUtils.sendJsonFetcher(
                        "do_request_begin",
                        new EventBus.DataFetcher() {
                            @Override
                            public Object fetch() {
                                JSONObject json = new JSONObject();
                                final JSONObject reqHeaderJson = new JSONObject();
                                try {
                                    Map<String, List<String>> props = finalConn.getRequestProperties();
                                    if (!props.isEmpty()) {
                                        for (Map.Entry<String, List<String>> entry : props.entrySet()) {
                                            reqHeaderJson.put(
                                                    entry.getKey(),
                                                    TextUtils.join(",", entry.getValue()));
                                        }
                                    }
                                    json.put("appId", appLogInstance.getAppId());
                                    json.put("nid", requestId);
                                    json.put("url", urlString);
                                    json.put("data", data);
                                    json.put("header", reqHeaderJson);
                                    json.put("method", method);
                                    json.put("time", startTimestamp);
                                } catch (Throwable ignored) {

                                }
                                return json;
                            }
                        });
            }

            byte[] encryptBytes = null;
            if (null != data) {
                String jsonStr = data.toString();
                if (encrypt) {
                    encryptBytes = encryptUtils.transformStrToByte(jsonStr);
                } else {
                    encryptBytes = jsonStr.getBytes("UTF-8");
                }
            }
            if (encryptBytes != null && encryptBytes.length > 0) {
                dataOutputStream = new DataOutputStream(conn.getOutputStream());
                dataOutputStream.write(encryptBytes);
                dataOutputStream.flush();
            }

            final int responseCode = conn.getResponseCode();
            final long endTimestamp = System.currentTimeMillis();

            appLogInstance
                    .getLogger()
                    .debug(
                            LogInfo.Category.REQUEST,
                            "http response:{} message:{}",
                            responseCode,
                            conn.getResponseMessage());
            if (responseCode == HttpURLConnection.HTTP_OK) {
                if (respType == INetworkClient.RESPONSE_TYPE_STRING) {
                    int contentLength = conn.getContentLength();
                    if (contentLength < LENGTH_MAX) {
                        final InputStream inputStream = conn.getInputStream();
                        String encodingType = conn.getContentEncoding();
                        if ("gzip".equalsIgnoreCase(encodingType)) {
                            reader =
                                    new BufferedReader(
                                            new InputStreamReader(
                                                    new GZIPInputStream(inputStream)));
                        } else {
                            reader = new BufferedReader(new InputStreamReader(inputStream));
                        }
                        StringBuilder sb = new StringBuilder(inputStream.available());
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line).append("\n");
                        }

                        appLogInstance
                                .getLogger()
                                .debug(
                                        LogInfo.Category.REQUEST,
                                        "http responseBody: {}",
                                        sb.toString());

                        JSONObject object = new JSONObject(sb.toString());
                        // fixme the following code will not run if NetworkClient is set by
                        // bussiness caller
                        // cookie in response header for circle login
                        String cookie = dealCookie(conn.getHeaderFields());
                        object.put(KEY_SET_COOKIE, cookie);
                        parseResponseLogId(conn, object, urlString);
                        responseBytes = object.toString().getBytes("UTF-8");
                    } else {
                        appLogInstance
                                .getLogger()
                                .error(LogInfo.Category.REQUEST, "contentLength large than max");
                    }
                } else {
                    final InputStream inputStream = conn.getInputStream();
                    String encodingType = conn.getContentEncoding();
                    if ("gzip".equalsIgnoreCase(encodingType)) {
                        is = new GZIPInputStream(inputStream);
                    } else {
                        is = inputStream;
                    }
                    baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        baos.write(buffer, 0, len);
                    }
                    responseBytes = baos.toByteArray();

                    appLogInstance
                            .getLogger()
                            .debug(
                                    LogInfo.Category.REQUEST,
                                    "http responseBody byte length: {}",
                                    responseBytes.length);
                }

                sendRequestEnd2DevTools(
                        requestId, responseCode, responseBytes, null, endTimestamp, conn);
            } else {
                sendRequestEnd2DevTools(
                        requestId,
                        responseCode,
                        null,
                        conn.getResponseMessage(),
                        endTimestamp,
                        conn);
                throw new RangersHttpException(responseCode, conn.getResponseMessage());
            }
        } catch (final Throwable t) {
            appLogInstance.getLogger().error(LogInfo.Category.REQUEST, "Send request failed", t);

            if (t instanceof RangersHttpException) {
                throw (RangersHttpException) t;
            } else {
                sendRequestEnd2DevTools(
                        requestId, -1, null, t.getMessage(), System.currentTimeMillis(), conn);
                if (t instanceof SocketTimeoutException) {
                    throw new RangersHttpTimeoutException("Request timeout");
                }
            }
        } finally {
            Utils.closeSafely(dataOutputStream);
            Utils.closeSafely(reader);
            Utils.closeSafely(is);
            Utils.closeSafely(baos);
        }
        return responseBytes;
    }

    private String dealCookie(Map<String, List<String>> headerFields) {
        StringBuilder cookie = new StringBuilder();
        if (headerFields != null && headerFields.containsKey(KEY_SET_COOKIE)) {
            for (String itemCookie : headerFields.get(KEY_SET_COOKIE)) {
                cookie.append(itemCookie).append(";");
            }
        }
        return cookie.toString();
    }

    private JSONObject getResponseHeaders(HttpURLConnection conn) {
        if (null == conn) {
            return null;
        }
        Map<String, List<String>> responseHeaders = conn.getHeaderFields();
        if (responseHeaders.isEmpty()) {
            return null;
        }
        final JSONObject resHeaderJson = new JSONObject();
        for (String k : responseHeaders.keySet()) {
            if (Utils.isNotEmpty(k)) {
                try {
                    resHeaderJson.put(k, responseHeaders.get(k));
                } catch (Throwable ignored) {

                }
            }
        }
        return resHeaderJson;
    }

    private void sendRequestEnd2DevTools(
            final String requestId,
            final int responseCode,
            final byte[] responseBytes,
            final String responseString,
            final long time,
            final HttpURLConnection connection) {
        LogUtils.sendJsonFetcher(
                "do_request_end",
                new EventBus.DataFetcher() {
                    @Override
                    public Object fetch() {
                        JSONObject json = new JSONObject();
                        try {
                            json.put("appId", appLogInstance.getAppId());
                            json.put("nid", requestId);
                            json.put("statusCode", responseCode);
                            json.put("responseByte", responseBytes);
                            json.put("responseString", responseString);
                            json.put("time", time);
                            json.put("header", getResponseHeaders(connection));
                        } catch (Throwable ignored) {

                        }
                        return json;
                    }
                });
    }

    private void parseResponseLogId(HttpURLConnection conn, JSONObject object, String urlString) {
        String logId = getResponseHeaders(conn).optString(KEY_RESPONSE_LOG_ID);
        if (!TextUtils.isEmpty(logId)
                && !TextUtils.isEmpty(urlString)
                && urlString.contains(UriConfig.PATH_REGISTER)
                && !DeviceManager.isValidDidAndIid(object)) {
            try {
                object.put(KEY_RESPONSE_LOG_ID, logId);
            } catch (JSONException e) {
                appLogInstance.getLogger().error("parseResponseLogId failed", e);
            }
        }
    }

    public static JSONObject buildRequestBody(JSONObject header) throws JSONException {
        JSONObject request = new JSONObject();
        request.put(Api.KEY_MAGIC, Api.MSG_MAGIC);
        request.put(Api.KEY_HEADER, header);
        request.put("_gen_time", System.currentTimeMillis());
        return request;
    }
}
