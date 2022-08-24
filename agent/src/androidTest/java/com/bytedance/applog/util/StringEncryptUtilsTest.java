// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.support.test.runner.AndroidJUnit4;
import com.bytedance.applog.BaseAppLogTest;
import java.util.Arrays;
import org.junit.*;
import org.junit.runner.*;

/**
 * Created by lixiao on 2020/8/20.
 */
@RunWith(AndroidJUnit4.class)
public class StringEncryptUtilsTest extends BaseAppLogTest {

    @Test
    public void testEncryptByDES() throws Exception {
        byte[] bytes = StringEncryptUtils.encryptByDES("test", "passpasspasspass");
//        throw new Exception(Arrays.toString(bytes));
        Assert.assertArrayEquals(new byte[] {
                54,104,113,89,49,47,105,66,54,113,69,61,10,
        }, bytes);
    }

    @Test
    public void testDecryptByDES() throws Exception {
        String result = StringEncryptUtils.decryptByDES(new byte[] {
                54,104,113,89,49,47,105,66,54,113,69,61,10,
        }, "passpasspasspass");
        Assert.assertEquals("test", result);
    }

    @Test
    public void base64DecodeToString() {
        String result = StringEncryptUtils.base64DecodeToString("dGVzdA==");
        Assert.assertEquals("test", result);
    }
}
