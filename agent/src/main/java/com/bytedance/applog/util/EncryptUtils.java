// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Pair;

import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.IAppLogInstance;
import com.bytedance.applog.encryptor.CustomEncryptor;
import com.bytedance.applog.encryptor.IEncryptor;
import com.bytedance.applog.server.Api;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptUtils {

    /** key for encrypted original url query */
    private static final String KEY_TT_INFO = "tt_info";

    private static final String KEY_TT_DATA = "tt_data";

    public static final String KEY_DEVICE_PLATFORM = "device_platform";

    public static final String KEY_IID = "iid";

    public static final String KEY_EVENT_FILTER = "event_filter";

    public static final String[] KEYS_CONFIG_QUERY =
            new String[] {KEY_TT_DATA, KEY_DEVICE_PLATFORM};

    public static final String[] KEYS_REPORT_QUERY =
            new String[] {
                Api.KEY_AID, Api.KEY_VERSION_CODE, Api.KEY_AB_VERSION, KEY_IID, KEY_DEVICE_PLATFORM
            };

    private static final String[] KEYS_PLAINTEXT =
            new String[] {Api.KEY_AID, Api.KEY_APP_VERSION, KEY_TT_DATA, Api.KEY_DEVICE_ID};

    private final AppLogInstance appLogInstance;

    public EncryptUtils(final AppLogInstance appLogInstance) {
        this.appLogInstance = appLogInstance;
    }

    public String encryptUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }
        if (!appLogInstance.getEncryptAndCompress()) {
            return url;
        }
        Uri u = Uri.parse(url);
        String oq = u.getEncodedQuery();
        List<Pair<String, String>> plaintextPairs = new ArrayList<>();
        for (String key : KEYS_PLAINTEXT) {
            String value = u.getQueryParameter(key);
            if (!TextUtils.isEmpty(value)) {
                plaintextPairs.add(new Pair<>(key, value));
            }
        }
        Uri.Builder builder = u.buildUpon();
        builder.clearQuery();
        for (Pair<String, String> plaintextPair : plaintextPairs) {
            builder.appendQueryParameter(plaintextPair.first, plaintextPair.second);
        }
        byte[] eq = transformStrToByte(oq);
        byte[] bq = Base64.encode(eq, Base64.URL_SAFE);
        builder.appendQueryParameter(KEY_TT_INFO, new String(bq));
        url = builder.build().toString();
        return url;
    }

    /**
     * 包含压缩+加密，可用开关控制
     *
     * @param str 明文
     * @return byte[]
     */
    public byte[] transformStrToByte(String str) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(8192);
        GZIPOutputStream zos = null;
        try {
            if (appLogInstance.getEncryptAndCompress()) {
                zos = new GZIPOutputStream(bos);
                zos.write(str.getBytes("UTF-8"));
            } else {
                bos.write(str.getBytes("UTF-8"));
            }
        } catch (Throwable tr) {
            appLogInstance
                    .getLogger()
                    .error(
                            Collections.singletonList("EncryptUtils"),
                            "Convert string to bytes failed",
                            tr);
        } finally {
            Utils.closeSafely(zos);
        }
        byte[] data = bos.toByteArray();
        return encrypt(data);
    }

    public byte[] encrypt(byte[] data) {
        if (appLogInstance.getEncryptAndCompress()) {
            if (null != appLogInstance.getInitConfig()
                    && null != appLogInstance.getInitConfig().getEncryptor()) {
                IEncryptor encryptor = appLogInstance.getInitConfig().getEncryptor();
                return encryptor.encrypt(data, data.length);
            } else {
                return internalEncrypt(data, data.length);
            }
        }
        return data;
    }

    private byte[] internalEncrypt(byte[] inputData, int length) {
        try {
            Class<?> clazz = Class.forName("com.bytedance.applog.encryptor.EncryptorUtil");
            Method method = clazz.getDeclaredMethod("encrypt", byte[].class, int.class);
            method.setAccessible(true);
            return (byte[]) method.invoke(null, inputData, length);
        } catch (Throwable e) {
            appLogInstance
                    .getLogger()
                    .error("encrypt failed, please add encryptor library or custom encryption.", e);
        }
        return inputData;
    }

    private static String byte2String(byte[] b) {
        StringBuilder hs = new StringBuilder();
        String stmp;
        for (int n = 0; b != null && n < b.length; n++) {
            stmp = Integer.toHexString(b[n] & 0XFF);
            if (stmp.length() == 1) {
                hs.append('0');
            }
            hs.append(stmp);
        }
        return hs.toString();
    }

    public static String[] genRandomKeyAndIv() {
        String[] keyAndIv = new String[2];
        try {
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            SecureRandom sr = new SecureRandom();
            kgen.init(128, sr);
            SecretKey secretKey = kgen.generateKey();
            byte[] rawKey = secretKey.getEncoded();
            keyAndIv[0] = byte2String(rawKey);

            byte[] iv = new byte[8];
            sr.nextBytes(iv);
            keyAndIv[1] = byte2String(iv);

            if (!TextUtils.isEmpty(keyAndIv[0])
                    && keyAndIv[0].length() == 32
                    && !TextUtils.isEmpty(keyAndIv[1])
                    && keyAndIv[1].length() == 16) {
                return keyAndIv;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static void putRandomKeyAndIvIntoRequest(IAppLogInstance appLogInstance, JSONObject request) {
        if (appLogInstance.getEncryptAndCompress()) {
            String[] keyAndIv = EncryptUtils.genRandomKeyAndIv();
            if (keyAndIv != null) {
                try {
                    request.put("key", keyAndIv[0]);
                    request.put("iv", keyAndIv[1]);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static byte[] transStrCharToByte(String str) {
        int len = str.length();
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            result[i] = (byte) str.charAt(i);
        }
        return result;
    }

    public static byte[] decryptAesCbc(byte[] cipherText, String key, String iv) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7PADDING");
            SecretKeySpec keySpec = new SecretKeySpec(transStrCharToByte(key), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(transStrCharToByte(iv));
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            return cipher.doFinal(cipherText);
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static byte[] gzipUncompress(byte[] bytes) {
        if (bytes == null || bytes.length <= 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = null;
        GZIPInputStream ungzip = null;
        //noinspection TryFinallyCanBeTryWithResources
        try {
            in = new ByteArrayInputStream(bytes);
            ungzip = new GZIPInputStream(in);
            byte[] buffer = new byte[1024];
            int n;
            while ((n = ungzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
        } catch (IOException ignored) {
        } finally {
            Utils.closeSafely(ungzip);
            Utils.closeSafely(in);
        }
        return out.toByteArray();
    }

    public static HashMap<String, String> putContentTypeHeader(HashMap<String, String> headers, AppLogInstance appLogInstance) {
        String encryptorTypeStr = CustomEncryptor.DEFAULT_ENCRYPTOR;
        if (appLogInstance.getInitConfig() != null
                && appLogInstance.getInitConfig().getEncryptor() != null
                && appLogInstance.getInitConfig().getEncryptor() instanceof CustomEncryptor) {
            encryptorTypeStr = ((CustomEncryptor)appLogInstance.getInitConfig().getEncryptor()).encryptorType();
        }
        if (appLogInstance.getEncryptAndCompress()) {
            headers.put("Content-Type", "application/octet-stream;tt-data=" + encryptorTypeStr);
        } else {
            headers.put("Content-Type", "application/json; charset=utf-8");
        }
        return headers;
    }
}
