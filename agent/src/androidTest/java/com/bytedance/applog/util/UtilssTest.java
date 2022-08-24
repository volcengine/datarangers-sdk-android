// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;


import java.util.Arrays;
import java.util.HashMap;
import org.junit.*;

public class UtilssTest {

    @Test
    public void testFormat(){
        final HashMap<String, String> params = new HashMap<>();
        System.out.println(1);
        params.put("1", "aaaa");
        params.put("2", "bbbb&");
        StringBuilder result = new StringBuilder("");
        Utils.format(result, params, "UTF-8");
        String[] results = result.toString().split("&");
        Arrays.sort(results);
        String[] expects = "2=bbbb%26&1=aaaa".split("&");
        Arrays.sort(expects);
        Assert.assertArrayEquals(expects, results);
    }
}
