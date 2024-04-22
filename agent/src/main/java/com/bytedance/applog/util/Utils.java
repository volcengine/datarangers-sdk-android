// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.bytedance.applog.log.LoggerImpl;
import com.bytedance.applog.manager.DeviceManager;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

/**
 * @author shiyanlong
 * @date 2019/3/26
 */
public class Utils {
    private static final String PARAMETER_SEPARATOR = "&";
    private static final String NAME_VALUE_SEPARATOR = "=";
    private static final String DEFAULT_CONTENT_CHARSET = "ISO-8859-1";
    private static String sProcessName;

    public static String toString(Object obj) {
        return null != obj ? obj.toString() : "";
    }

    @SuppressWarnings({"EqualsReplaceableByObjectsCall"})
    public static boolean equals(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    /**
     * 判断字符串不为空
     *
     * @param text 字符串
     * @return true: 不为空
     */
    public static boolean isNotEmpty(String text) {
        return null != text && text.length() > 0;
    }

    /**
     * 判断字符串为空
     *
     * @param text 字符串
     * @return true: 为空
     */
    public static boolean isEmpty(String text) {
        return !isNotEmpty(text);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean jsonEquals(JSONObject a, JSONObject b) {
        if (a != null && b != null) {
            return a.toString().equals(b.toString());
        }
        return equals(a, b);
    }

    public static String getProcessName() {
        String procName = sProcessName;
        if (!TextUtils.isEmpty(procName)) {
            return procName;
        }
        sProcessName = getProcessNameFromProc();
        LoggerImpl.global().debug("getProcessName: " + sProcessName);
        return sProcessName;
    }

    public static boolean isValidUDID(final String s) {
        final int length = s != null ? s.length() : 0;
        if (length < DeviceManager.MIN_UDID_LENGTH || length > DeviceManager.MAX_UDID_LENGTH) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F' || c == '-') {
                // nothing to do
            } else {
                return false;
            }
        }
        return true;
    }

    private static String getProcessNameFromProc() {
        BufferedReader cmdlineReader = null;
        try {
            cmdlineReader =
                    new BufferedReader(
                            new InputStreamReader(
                                    new FileInputStream(
                                            "/proc/" + android.os.Process.myPid() + "/cmdline"),
                                    "iso-8859-1"));
            int c;
            StringBuilder processName = new StringBuilder();
            while ((c = cmdlineReader.read()) > 0) {
                processName.append((char) c);
            }
            return processName.toString();
        } catch (Throwable ignore) {
            // ignore
        } finally {
            closeSafely(cmdlineReader);
        }
        return null;
    }

    public static JSONObject copy(
            final JSONObject newHeader, @Nullable final JSONObject oldHeader) {
        if (null != oldHeader) {
            try {
                final Iterator<String> iter = oldHeader.keys();
                while (iter.hasNext()) {
                    String key = iter.next();
                    newHeader.put(key, oldHeader.opt(key));
                }
            } catch (Throwable e) {
                LoggerImpl.global().error("copy json error", e);
            }
        }
        return newHeader;
    }

    public static void format(
            final StringBuilder stringBuilder,
            final HashMap<String, String> params,
            final String encoding) {
        final StringBuilder result = stringBuilder;

        for (final String key : params.keySet()) {
            final String encodedName = encode(key, encoding);
            final String value = params.get(key);
            final String encodedValue = value != null ? encode(value, encoding) : "";
            if (result.length() > 0) {
                result.append(PARAMETER_SEPARATOR);
            }
            result.append(encodedName).append(NAME_VALUE_SEPARATOR).append(encodedValue);
        }
    }

    private static String encode(final String content, final String encoding) {
        try {
            return URLEncoder.encode(
                    content, encoding != null ? encoding : DEFAULT_CONTENT_CHARSET);
        } catch (UnsupportedEncodingException ignore) {
        }
        return "";
    }

    public static boolean equals(String s1, String s2) {
        boolean bothNull = TextUtils.isEmpty(s1) && TextUtils.isEmpty(s2);
        boolean equals = s1 != null && s1.equals(s2);
        return bothNull || equals;
    }

    public static String getYesNoString(boolean bool) {
        if (bool) {
            return "yes";
        } else {
            return "no";
        }
    }

    public static void closeSafely(Closeable res) {
        if (res != null) {
            try {
                res.close();
            } catch (Throwable e) {
                LoggerImpl.global().error("closeSafely error", e);
            }
        }
    }

    public static void closeSafely(Cursor res) {
        if (res != null) {
            try {
                res.close();
            } catch (Throwable e) {
                LoggerImpl.global().error("closeSafely error", e);
            }
        }
    }

    public static void endDbTransactionSafely(SQLiteDatabase db) {
        if (db != null) {
            try {
                db.endTransaction();
            } catch (Throwable e) {
                LoggerImpl.global().error("endDbTransactionSafely error", e);
            }
        }
    }

    public static boolean checkId(String uuid) {
        if (!TextUtils.isEmpty(uuid)
                && !"unknown".equalsIgnoreCase(uuid)
                && !"Null".equalsIgnoreCase(uuid)) {

            boolean allZero = true;
            for (int i = 0; i < uuid.length(); i++) {
                if (uuid.charAt(i) != '0') {
                    allZero = false;
                    break;
                }
            }
            return !allZero;
        }
        return false;
    }

    /**
     * 命令行读取系统配置
     *
     * @param key 关键词
     * @return String
     */
    public static String getSysPropByExec(String key) {
        String line = "";
        if (TextUtils.isEmpty(key)) {
            return line;
        }
        BufferedReader input = null;
        try {
            Process p = Runtime.getRuntime().exec("getprop " + key);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            line = input.readLine();
            p.destroy();
        } catch (Throwable e) {
            LoggerImpl.global().error("getSysPropByExec error", e);
        } finally {
            Utils.closeSafely(input);
        }
        return line;
    }

    /**
     * 获取唯一的event id
     *
     * @return String
     */
    public static synchronized String getUniqueEventId() {
        return UUID.randomUUID().toString().replace("-", "").toLowerCase()
                + System.currentTimeMillis();
    }
}
