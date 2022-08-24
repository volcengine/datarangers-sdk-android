// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.server;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.SystemClock;
import android.text.TextUtils;

import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.IActiveCustomParamsCallback;
import com.bytedance.applog.manager.ConfigManager;
import com.bytedance.applog.monitor.model.NetworkTrace;
import com.bytedance.applog.network.RangersHttpException;
import com.bytedance.applog.util.EncryptUtils;
import com.bytedance.applog.util.RequestIdGenerator;
import com.bytedance.applog.util.SensitiveUtils;
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
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

/**
 * @author shiyanlong
 * @date 2019/1/16
 */
public class Api {

    public static final String KEY_DEVICE_TOKEN = "device_token";
    private static final String[] HTTP_METHOD = {"GET", "POST"};

    public static final int METHOD_GET = 0;

    public static final int METHOD_POST = 1;

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

    private static final String BLOCK_LIST_KEY = "blocklist";

    private static final String BLOCK_LIST_V1 = "v1";

    private static final String BLOCK_LIST_V3 = "v3";

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
    public static final String KEY_SCREEN_ORIENTATION = "$screen_orientation";
    public static final String KEY_GPS_LONGITUDE = "$longitude";
    public static final String KEY_GPS_LATITUDE = "$latitude";
    public static final String KEY_GPS_GCS = "$geo_coordinate_system";
    public static final String KEY_EVENT_DURATION = "$event_duration";
    public static final String KEY_PAGE_DURATION = "$page_duration";
    public static final String LEAVE_PAGE_EVENT_NAME = "$bav2b_page_leave";

    protected final AppLogInstance appLogInstance;
    private final EncryptUtils encryptUtils;

    public Api(final AppLogInstance appLogInstance) {
        this.appLogInstance = appLogInstance;
        this.encryptUtils = new EncryptUtils(appLogInstance);
    }

    public EncryptUtils getEncryptUtils() {
        return encryptUtils;
    }

    private static class HttpResp {
        int mType = 0; // 0: default resp string; 1: resp byte array
        String mRespStr;
        byte[] mRespByteArray;

        HttpResp(int type) {
            mType = type;
        }
    }

    private static String addQuery(String url, String key, String value) {
        try {
            if (TextUtils.isEmpty(key) || TextUtils.isEmpty(value)) {
                return url;
            }
            Uri uri = Uri.parse(url);
            return uri.buildUpon().appendQueryParameter(key, value).build().toString();
        } catch (Throwable e) {
            TLog.e("addQuery", e);
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

    private static void updateTimeDiff(final JSONObject o) {
        try {
            long serverTime = o.optLong(KEY_SERVER_TIME);
            if (serverTime > 0L) {
                JSONObject ts = new JSONObject();
                ts.put(KEY_SERVER_TIME, serverTime);
                long localTime = System.currentTimeMillis() / 1000L;
                ts.put(KEY_LOCAL_TIME, localTime);
                mTimeSync = ts;
            }
        } catch (Exception ignore) {
        }
    }

    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>(2);
        if (appLogInstance.getEncryptAndCompress()) {
            headers.put("Content-Type", "application/octet-stream;tt-data=a");
        } else {
            headers.put("Content-Type", "application/json; charset=utf-8");
        }
        return headers;
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
        HashMap<String, String> headers = getHeaders();
        String response = null;
        try {
            byte[] data = encryptUtils.transformStrToByte(request.toString());
            String key = request.optString("key");
            String iv = request.optString("iv");
            boolean isStream = !TextUtils.isEmpty(key) && !TextUtils.isEmpty(iv);
            if (isStream) {
                byte[] respByteArray = appLogInstance.getNetClient().postStream(uri, data, headers);
                if (respByteArray == null) {
                    isStream = false;
                } else {
                    byte[] decryptedData = EncryptUtils.decryptAesCbc(respByteArray, key, iv);
                    if (decryptedData != null) {
                        byte[] bytes = EncryptUtils.gzipUncompress(decryptedData);
                        if (bytes != null) {
                            response = new String(bytes);
                        }
                    } else {
                        response = new String(respByteArray);
                    }
                }
            }
            if (!isStream) {
                response = appLogInstance.getNetClient().post(uri, data, headers);
            }
        } catch (Throwable e) {
            TLog.e(e);
        }
        JSONObject obj = null;
        if (response != null) {
            try {
                obj = new JSONObject(response);
                updateTimeDiff(obj);
            } catch (JSONException e) {
                TLog.e(e);
            }
        }
        boolean success = obj != null && Api.MSG_MAGIC.equals(obj.optString(KEY_MAGIC, ""));
        if (success) {
            return obj.optJSONObject("config");
        } else {
            return null;
        }
    }

    public JSONObject abConfig(String uri, final JSONObject request) {
        String response = null;
        try {
            response =
                    appLogInstance
                            .getNetClient()
                            .post(
                                    uri,
                                    encryptUtils.transformStrToByte(request.toString()),
                                    CONTENT_TYPE);
        } catch (Exception e) {
            TLog.e(e);
        }
        JSONObject obj = null;
        if (response != null) {
            try {
                obj = new JSONObject(response);
                updateTimeDiff(obj);
            } catch (JSONException e) {
                TLog.e(e);
            }
        }
        boolean success = obj != null && Api.MSG_OK.equals(obj.optString(KEY_MSG, ""));
        if (success) {
            return obj.optJSONObject("data");
        } else {
            return null;
        }
    }

    public JSONObject simulateLogin(
            final String aid,
            String appVersion,
            int width,
            int height,
            String deviceId,
            String qrParam) {
        JSONObject request = new JSONObject();
        try {
            JSONObject header = getHeader(aid, appVersion);
            header.put("width", width);
            header.put("height", height);
            header.put("device_id", deviceId);
            request.put("header", header);

            request.put("qr_param", qrParam);
        } catch (JSONException e) {
            TLog.ysnp(e);
            return null;
        }

        HashMap<String, String> header = getHeaders();
        String resp = null;
        try {
            resp =
                    http(
                            METHOD_POST,
                            sSchemeHost + PATH_SIMULATE_LOGIN,
                            header,
                            encryptUtils.transformStrToByte(request.toString()));
        } catch (RangersHttpException ignore) {
        }
        if (TextUtils.isEmpty(resp)) {
            return null;
        }
        try {
            return new JSONObject(resp);
        } catch (JSONException e) {
            TLog.ysnp(e);
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
        JSONObject request = new JSONObject();
        try {
            JSONObject header = getHeader(aid, appVersion);
            Utils.copy(header, appLogInstance.getHeader());
            header.put("width", width);
            header.put("height", height);
            header.put("device_id", deviceId);
            header.put(Api.KEY_DEVICE_MODEL, Build.MODEL);
            request.put("header", header);
        } catch (JSONException e) {
            TLog.ysnp(e);
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
                        http(
                                METHOD_POST,
                                sSchemeHost + PATH_SIMULATE_LOGIN_NO_QR,
                                header,
                                encryptUtils.transformStrToByte(request.toString()));
                JSONObject rspJSON = new JSONObject(resp);
                int retry = rspJSON.getJSONObject("data").getInt("retry");
                if (retry == 0) {
                    return null;
                } else if (retry == 2) {
                    break;
                }
                sync_id = rspJSON.getJSONObject("data").getString("sync_id");
            } catch (Exception e) {
                TLog.e(e);
            }
            long elapse = System.currentTimeMillis() - startRequest;
            if (elapse < 1000) {
                try {
                    Thread.sleep(1000 - elapse);
                } catch (InterruptedException e) {
                    TLog.e(e);
                }
            }
        }
        if (TextUtils.isEmpty(resp)) {
            return null;
        }
        try {
            return new JSONObject(resp);
        } catch (JSONException e) {
            TLog.ysnp(e);
        }
        return null;
    }

    public boolean sendToRangersEventVerify(final JSONObject event, final String cookie) {
        JSONObject request = new JSONObject();
        try {
            JSONObject header = appLogInstance.getHeader();
            request.put("header", header);

            JSONArray eventArray = new JSONArray();
            if (event != null) {
                eventArray.put(event);
            }
            request.put("event_v3", eventArray);
        } catch (JSONException e) {
        }
        HashMap<String, String> header = getHeaders();
        header.put(KEY_COOKIES, cookie);
        boolean keep = true;
        String resp = null;
        try {
            resp =
                    http(
                            METHOD_POST,
                            sSchemeHost + PATH_SIMULATE_EVENT_VERIFY,
                            header,
                            encryptUtils.transformStrToByte(request.toString()));
            JSONObject jsonResp = new JSONObject(resp);
            JSONObject data = jsonResp.getJSONObject("data");
            keep = data.getBoolean("keep");
        } catch (Exception e) {
            return false;
        }
        if (!keep) {
            appLogInstance.setRangersEventVerifyEnable(false, cookie);
        }
        return true;
    }

    public void setSchemeHost(String host) {
        sSchemeHost = host;
    }

    public String getSchemeHost() {
        return sSchemeHost;
    }

    public int send(String[] uris, final byte[] data, final ConfigManager config) {
        HashMap<String, String> headers = getHeaders();

        int resultCode = HTTP_NO_MESSAGE;
        JSONObject o = null;
        for (final String uriSendHeader : uris) {
            try {
                String response = appLogInstance.getNetClient().post(uriSendHeader, data, headers);
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
                    TLog.ysnp(e);
                    // 默认值
                }
            }
        }

        if (o != null) {
            config.updateLogRespConfig(o);
            JSONObject blockList = o.optJSONObject(BLOCK_LIST_KEY);
            if (blockList != null) {
                JSONArray blockV1 = blockList.optJSONArray(BLOCK_LIST_V1);
                int countV1 = blockV1 != null ? blockV1.length() : 0;
                HashSet<String> blockSetV1 = new HashSet<>(countV1);
                for (int i = 0; i < countV1; ++i) {
                    String blockName = blockV1.optString(i, null);
                    if (!TextUtils.isEmpty(blockName)) {
                        blockSetV1.add(blockName);
                    }
                }

                JSONArray blockV3 = blockList.optJSONArray(BLOCK_LIST_V3);
                int countV3 = blockV3 != null ? blockV3.length() : 0;
                HashSet<String> blockSetV3 = new HashSet<>(countV3);
                for (int i = 0; i < countV3; ++i) {
                    String blockName = blockV3.optString(i, null);
                    if (!TextUtils.isEmpty(blockName)) {
                        blockSetV3.add(blockName);
                    }
                }

                config.updateBlock(blockSetV1, blockSetV3);
            }
        }

        return resultCode;
    }

    private HttpResp httpRequestInner(
            int method,
            String urlString,
            HashMap<String, String> headers,
            byte[] data,
            int respType,
            int timeout)
            throws RangersHttpException {
        TLog.d("Start request http url: " + urlString);
        if (TLog.DEBUG) {
            if (headers != null) {
                for (HashMap.Entry<String, String> entry : headers.entrySet()) {
                    if (!TextUtils.isEmpty(entry.getKey())
                            && !TextUtils.isEmpty(entry.getValue())) {
                        TLog.d("http headers key:" + entry.getKey() + " value:" + entry.getValue());
                    }
                }
            }
            if (data != null) {
                TLog.d("http data length:" + data.length);
            }
        }

        HttpResp httpResp = new HttpResp(respType);
        DataOutputStream dataOutputStream = null;
        BufferedReader reader = null;
        InputStream is = null;
        ByteArrayOutputStream baos = null;
        long timeStart = SystemClock.elapsedRealtime();
        try {
            final URL url = new URL(urlString);
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            if (timeout > 0) {
                conn.setConnectTimeout(timeout);
            }
            if (method == METHOD_GET) {
                conn.setDoOutput(false);
            } else if (method == METHOD_POST) {
                conn.setDoOutput(true);
            } else {
                TLog.ysnp(null);
            }
            conn.setRequestMethod(HTTP_METHOD[method]);
            if (headers != null && !headers.isEmpty()) {
                for (HashMap.Entry<String, String> entry : headers.entrySet()) {
                    if (!TextUtils.isEmpty(entry.getKey())
                            && !TextUtils.isEmpty(entry.getValue())) {
                        conn.addRequestProperty(entry.getKey(), entry.getValue());
                    } else {
                        TLog.ysnp(null);
                    }
                }
            }

            conn.setRequestProperty("Accept-Encoding", "gzip");
            if (data != null && data.length > 0) {
                dataOutputStream = new DataOutputStream(conn.getOutputStream());
                dataOutputStream.write(data);
                dataOutputStream.flush();
            }
            int responseCode = conn.getResponseCode();
            if (TLog.DEBUG) {
                TLog.d("http response message:" + responseCode + " " + conn.getResponseMessage());
            }
            if (responseCode == HttpURLConnection.HTTP_OK) {
                if (respType == 0) {
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

                        httpResp.mRespStr = sb.toString();
                        JSONObject object = new JSONObject(httpResp.mRespStr);
                        // fixme the following code will not run if NetworkClient is set by
                        // bussiness caller
                        // cookie in response header for circle login
                        String cookie = conn.getHeaderField(KEY_SET_COOKIE);
                        object.put(KEY_SET_COOKIE, cookie);
                        httpResp.mRespStr = object.toString();
                    } else {
                        TLog.ysnp(null);
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
                    httpResp.mRespByteArray = baos.toByteArray();
                }
            } else {
                if (null != appLogInstance.getMonitor()) {
                    long timeEnd = SystemClock.elapsedRealtime();
                    NetworkTrace trace = new NetworkTrace();
                    trace.setDuration(timeEnd - timeStart);
                    trace.setUrl(urlString);
                    trace.setErrorCode(responseCode);
                    trace.setErrorMsg(conn.getResponseMessage());
                    appLogInstance.getMonitor().trace(trace);
                }
                throw new RangersHttpException(responseCode, conn.getResponseMessage());
            }
        } catch (Throwable t) {
            TLog.ysnp(t);
            if (t instanceof RangersHttpException) {
                throw (RangersHttpException) t;
            }
        } finally {
            Utils.closeSafely(dataOutputStream);
            Utils.closeSafely(reader);
            Utils.closeSafely(is);
            Utils.closeSafely(baos);
        }
        TLog.d("http responseBody: " + httpResp.mRespStr);
        return httpResp;
    }

    public JSONObject register(String uri, final JSONObject request) {
        HashMap<String, String> headers = getHeaders();
        String response = null;
        try {
            // 内部版register也加密
            byte[] data = encryptUtils.transformStrToByte(request.toString());
            response =
                    appLogInstance.getNetClient().post(encryptUtils.encryptUrl(uri), data, headers);
            TLog.d("request register success: " + response);
        } catch (Exception e) {
            TLog.e("request register error.", e);
        }
        JSONObject obj = null;
        if (response != null) {
            try {
                obj = new JSONObject(response);
                updateTimeDiff(obj);
            } catch (JSONException e) {
                TLog.e("parse register response error:" + response, e);
            }
        }
        return obj;
    }

    public JSONObject reportOaid(String uri, final JSONObject request) {
        HashMap<String, String> headers = getHeaders();
        String response = null;
        try {
            // 内部版register也加密
            byte[] data = encryptUtils.transformStrToByte(request.toString());
            response =
                    appLogInstance.getNetClient().post(encryptUtils.encryptUrl(uri), data, headers);
            TLog.d("reportOaid success: " + response);
        } catch (Exception e) {
            TLog.e("reportOaid error.", e);
        }
        JSONObject obj = null;
        if (response != null) {
            try {
                obj = new JSONObject(response);
                updateTimeDiff(obj);
            } catch (JSONException e) {
                TLog.e("parse reportOaid response error:" + response, e);
            }
        }
        return obj;
    }

    public String http(int method, String urlString, HashMap<String, String> headers, byte[] data)
            throws RangersHttpException {
        return http(method, urlString, headers, data, -1);
    }

    public String http(
            int method, String urlString, HashMap<String, String> headers, byte[] data, int timeout)
            throws RangersHttpException {
        HttpResp httpResp = httpRequestInner(method, urlString, headers, data, 0, timeout);
        return httpResp.mRespStr;
    }

    public byte[] httpStream(
            int method, String urlString, HashMap<String, String> headers, byte[] data)
            throws RangersHttpException {
        HttpResp httpResp = httpRequestInner(method, urlString, headers, data, 1, -1);
        return httpResp.mRespByteArray;
    }
}
