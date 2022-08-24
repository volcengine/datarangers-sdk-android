// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.profile;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.bytedance.applog.engine.Engine;
import com.bytedance.applog.server.Api;
import com.bytedance.applog.util.TLog;

import org.json.JSONException;
import org.json.JSONObject;

/** Created by zhangxiaolong on 2018/5/16. */
public class UserProfileHelper {

    private static final long ONE_MINITE = 60 * 1000;

    private static final String[] HEADER =
            new String[] {
                Api.KEY_AID,
                Api.KEY_REGION,
                Api.KEY_OS,
                Api.KEY_PACKAGE,
                Api.KEY_APP_VERSION,
                Api.KEY_SDK_VERSION,
                Api.KEY_OS_VERSION,
                Api.KEY_DEVICE_MODEL,
                Api.KEY_RESOLUTION,
                Api.KEY_LANGUAGE,
                Api.KEY_TIMEZONE,
                Api.KEY_ACCESS,
                Api.KEY_DISPLAY_NAME,
                Api.KEY_CHANNEL,
                Api.KEY_CARRIER,
                Api.KEY_APP_LANGUAGE,
                Api.KEY_APP_REGION,
                Api.KEY_TZ_NAME,
                Api.KEY_TZ_OFFSET,
                Api.KEY_INSTALL_ID,
                Api.KEY_OPEN_UDID,
                Api.KEY_MCC_MNC,
                Api.KEY_ROM,
                Api.KEY_MANIFEST_VERSION_CODE,
                Api.KEY_DEVICE_MANUFACTURER,
                Api.KEY_C_UDID,
                Api.KEY_SIG_HASH,
                Api.KEY_DISPLAY_DENSITY,
                Api.KEY_OS_API,
                Api.KEY_UPDATE_VERSION_CODE,
                Api.KEY_DENSITY_DPI,
                Api.KEY_VERSION_CODE,
                Api.KEY_SIM_SERIAL_NUMBER,
                Api.KEY_RELEASE_BUILD,
                Api.KEY_UDID,
                Api.KEY_CPU_ABI,
                Api.KEY_GOOGLE_AID
            };

    private static final String USER_PROFILE_PATH = "/service/api/v3/userprofile/%s/%s";

    private static final String[] METHOD_ARRAY = {"setOnce", "synchronize"};

    public static final int METHOD_SET = 0;

    public static final int METHOD_SYNC = 1;

    private static final int[] SIGN_ARRAY = {-1, -1};

    private static final long[] TIME_ARRAY = {-1, -1};

    public static void exec(
            final Engine engine,
            final int method,
            final JSONObject jsonObject,
            final UserProfileCallback callback,
            Handler handler,
            boolean force) {
        boolean changed = true;
        if (!force) {
            boolean timeEnough = System.currentTimeMillis() - TIME_ARRAY[method] > ONE_MINITE;
            changed = jsonObject != null && SIGN_ARRAY[method] != jsonObject.toString().hashCode();
            TLog.d("exec " + method + ", " + timeEnough + ", " + changed);

            if (!timeEnough) {
                if (callback != null) {
                    callback.onFail(UserProfileCallback.REQUEST_LIMIT);
                }
                return;
            }
        }

        if (changed) {
            Context context = engine.getAppLog().getContext();
            String deviceId = engine.getAppLog().getDid();
            String aid = engine.getAppLog().getAppId();
            String usrProfileUrl = engine.getUriConfig().getProfileUri();
            if (!TextUtils.isEmpty(deviceId)
                    && !TextUtils.isEmpty(aid)
                    && !TextUtils.isEmpty(usrProfileUrl)) {
                String path = String.format(USER_PROFILE_PATH, aid, METHOD_ARRAY[method]);
                String url = usrProfileUrl + path;
                String sb = getBody(engine.getAppLog().getHeader(), jsonObject);

                final UserProfileCallback innerCb =
                        new UserProfileCallback() {
                            @Override
                            public void onSuccess() {
                                SIGN_ARRAY[method] = jsonObject.toString().hashCode();
                                TIME_ARRAY[method] = System.currentTimeMillis();
                                callback.onSuccess();
                            }

                            @Override
                            public void onFail(int code) {
                                callback.onFail(code);
                            }
                        };

                final UserProfileReporter reporter =
                        new UserProfileReporter(engine.getAppLog(), url, aid, sb, innerCb, context);

                if (handler != null) {
                    handler.post(reporter);
                } else {
                    if (Looper.myLooper() == Looper.getMainLooper()) {
                        TLog.ysnp(null);
                    }
                    reporter.run();
                }
                return;
            }

            if (callback != null) {
                callback.onFail(UserProfileCallback.NOT_SATISFY);
            }
        } else {
            // 内容不变，直接当做已成功
            if (callback != null) {
                callback.onSuccess();
            }
        }
    }

    /**
     * 获取请求体
     *
     * @param data 自定义用户画像
     */
    private static String getBody(JSONObject appLogHeader, JSONObject data) {
        JSONObject body = new JSONObject();
        try {
            body.put("header", getHeader(appLogHeader));
            body.put("profile", data);
            body.put("user", getUser(appLogHeader));
        } catch (JSONException e) {
            TLog.e(e);
        }
        return body.toString();
    }

    private static JSONObject getUser(JSONObject appLogHeader) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        if (null != appLogHeader) {
            jsonObject.put(Api.KEY_DEVICE_ID, appLogHeader.opt(Api.KEY_DEVICE_ID));
            jsonObject.put(Api.KEY_USER_ID, appLogHeader.opt(Api.KEY_USER_ID));
            jsonObject.put(Api.KEY_SSID, appLogHeader.opt(Api.KEY_SSID));
        }
        return jsonObject;
    }

    private static JSONObject getHeader(JSONObject appLogHeader) {
        if (null != appLogHeader) {
            try {
                JSONObject jsonObject = new JSONObject(appLogHeader, HEADER);
                jsonObject.put(
                        Api.KEY_SDK_VERSION, appLogHeader.opt(Api.KEY_SDK_VERSION).toString());
                jsonObject.put(Api.KEY_TZ_OFFSET, appLogHeader.opt(Api.KEY_TZ_OFFSET).toString());
                return jsonObject;
            } catch (JSONException e) {
                TLog.e(e);
            }
        }
        return new JSONObject();
    }
}
