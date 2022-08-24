// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import java.security.MessageDigest;
import java.util.Objects;

/**
 * 从AppLogToB抄的。
 *
 * @author shiyanlong
 * @date 2019/1/30
 */
public class DigestUtils {

    private static final char[] HEX_CHARS = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    /** get hex string of specified bytes */
    public static String toHexString(byte[] bytes) {
        if (bytes == null) {
            Assert.e("bytes is null");
            return null;
        }
        return toHexString(bytes, 0, bytes.length);
    }

    /** get hex string of specified bytes */
    public static String toHexString(byte[] bytes, int off, int len) {
        if (Assert.t(null != bytes, "bytes is null")) {
            return null;
        }
        if (off < 0 || (off + len) > Objects.requireNonNull(bytes).length) {
            throw new IndexOutOfBoundsException();
        }
        char[] buff = new char[len * 2];
        int v;
        int c = 0;
        for (int i = 0; i < len; i++) {
            v = bytes[i + off] & 0xff;
            buff[c++] = HEX_CHARS[(v >> 4)];
            buff[c++] = HEX_CHARS[(v & 0x0f)];
        }
        return new String(buff, 0, len * 2);
    }

    public static String md5Hex(byte[] data) {
        try {
            if (data == null || data.length == 0) {
                return null;
            }
            MessageDigest digester = MessageDigest.getInstance("MD5");
            digester.update(data);
            return toHexString(digester.digest());
        } catch (Exception e) {
            return null;
        }
    }
}
