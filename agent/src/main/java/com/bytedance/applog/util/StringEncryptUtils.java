// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.text.TextUtils;
import android.util.Base64;

import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

public class StringEncryptUtils {

    private static final String SHA_256 = "SHA-256";


    public static String encryptBySHA256(String strSrc) {
        return encrypt(strSrc, SHA_256);
    }

    /**
     * 对字符串加密,加密算法使用MD5,SHA-1,SHA-256,默认使用SHA-256
     *
     * @param strSrc  要加密的字符串
     * @param encName 加密类型
     * @returnå
     */
    public static String encrypt(String strSrc, String encName) {
        MessageDigest md = null;
        String strDes = null;

        byte[] bt = strSrc.getBytes();
        try {
            if (TextUtils.isEmpty(encName)) {
                encName = SHA_256;
            }
            md = MessageDigest.getInstance(encName);
            md.update(bt);
            strDes = bytes2Hex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
        return strDes;
    }

    private static String bytes2Hex(byte[] bts) {
        if (bts == null) {
            return null;
        }
        try {
            StringBuilder result = new StringBuilder();
            for (byte bt : bts) {
                result.append(String.format("%02x", bt));
            }
            return result.toString();
        } catch (Throwable t) {
            // ignore for java.lang.NoSuchMethodError
        }
        return null;
    }

    public static byte[] encryptByDES(String data, String password) throws Exception {
        SecureRandom random = new SecureRandom();
        Key key = toKey(password.getBytes());
        Cipher cipher = Cipher.getInstance("DES");
        cipher.init(Cipher.ENCRYPT_MODE, key, random);
        return Base64.encode(cipher.doFinal(data.getBytes("UTF-8")), Base64.DEFAULT);
    }

    public static String decryptByDES(byte[] base64Data, String password) throws Exception {
        byte[] data = Base64.decode(base64Data, Base64.DEFAULT);
        SecureRandom random = new SecureRandom();
        Key key = toKey(password.getBytes());
        Cipher cipher = Cipher.getInstance("DES");
        cipher.init(Cipher.DECRYPT_MODE, key, random);
        return new String(cipher.doFinal(data), "UTF-8");
    }


    private static Key toKey(byte[] key) throws Exception {
        DESKeySpec dks = new DESKeySpec(key);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        return keyFactory.generateSecret(dks);
    }

    public static String base64DecodeToString(String origin){
        try {
            return new String(Base64.decode(origin.getBytes("UTF-8"),Base64.NO_WRAP));
        } catch (Exception e) {
            return "";
        }
    }
}
