// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.Arrays;
import org.junit.*;
import org.junit.runner.*;

/**
 * Created by lixiao on 2020/8/20.
 */
@RunWith(AndroidJUnit4.class)
public class EncryptUtilsTest {
    @Test
    public void genRandomKeyAndIv() throws Exception {
        String[] result = EncryptUtils.genRandomKeyAndIv();
        Assert.assertEquals(result[0].length(), 32);
        Assert.assertEquals(result[1].length(), 16);
    }

    @Ignore
    @Test
    public void transStrCharToByte() throws Exception {
        byte[] bytes = EncryptUtils.transformStrToByte("test");
        Assert.assertArrayEquals(new byte[]{116, 101, 115, 116}, bytes);
    }

    @Test
    public void decryptAesCbc() throws Exception {
        byte[] encrypt = new byte[] {117, 54, -125, 118, 119, -52, 99, 116, 96, -34, -59, -50, 48, 127, 74, 62};
        byte[] result = EncryptUtils.decryptAesCbc(encrypt, "passpasspasspass", "passpasspasspass");
        Assert.assertArrayEquals(result, "test".getBytes());
    }
}
