// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.content.Context;
import androidx.annotation.Nullable;
import com.bytedance.applog.Level;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import com.bytedance.applog.server.ApiParamsUtil;

public class SensitiveUtils {

    private static final String DEFAULT_MAC_ADDRESS = "02:00:00:00:00:00";

    public static final String CHANNEL_APP_KEY = "";

    static JSONArray getMultiImei(final Context context) {
        return new JSONArray();
    }

    public static JSONArray getMultiImeiFromSystem(final Context context) {
        return new JSONArray();
    }

    public static JSONArray getMultiImeiFallback(Context context) {
        return new JSONArray();
    }

    public static boolean validMultiImei(@Nullable String udidList) {
        return false;
    }

    public static boolean validMultiImei(@Nullable JSONArray udidList) {
        return false;
    }

    static String getDeviceId(Context context) {
        return "";
    }

    static String getIMSI(Context context) {
        return null;
    }

    public static String[] getSimSerialNumbers(Context context) {
        return null;
    }

    /** 获取已连接的Wifi路由器的Mac地址 */
    static String getConnectedWifiMacAddress(Context context) {
        return null;
    }

    public static String getMacAddress(Context context) {
        return DEFAULT_MAC_ADDRESS;
    }

    public static String getMacAddressFromSystem(Context context) {
        return DEFAULT_MAC_ADDRESS;
    }

    static String getBluetoothMacAddress(Context context) {
        return null;
    }

    public static String getSerialNumber(Context context) {
        return null;
    }

    public static void appendSensitiveParams(
            final ApiParamsUtil apiParamsUtil,
            final JSONObject header,
            final Map<String, String> params,
            boolean acceptAgreement,
            Level level) {}

    public static void addSensitiveParamsForUrlQuery(
            final ApiParamsUtil apiParamsUtil, StringBuilder sb, final JSONObject request) {}
}
