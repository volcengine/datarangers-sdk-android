// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class JsonUtilsTest {
    private static final String TAG = "JsonUtilsTest";

    @Test
    public void jsonUtilsTest() {

        JSONObject object1 = new JSONObject();
        try {
            object1.put("trade", "web");
            JSONArray array = new JSONArray();
            array.put("ball");
            array.put("running");
            array.put("photo");
            object1.put("interests", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject object2 = new JSONObject();
        try {
            object2.put("trade", "web2");
            JSONArray array = new JSONArray();
            array.put("ball");
            array.put("running");
            array.put("photo");
            object2.put("interests", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        boolean ret = true;
        try {
            ret = JsonUtils.compareJsons(object1, object2, null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "jsonUtilsTest: ret:" + ret);
        Assert.assertFalse(ret);
    }

    @Test
    public void jsonUtilsTest2() {

        JSONObject object1 = new JSONObject();
        try {
            object1.put("trade", "web");
            JSONArray array = new JSONArray();
            array.put("ball");
            array.put("running");
            array.put("photo");
            object1.put("interests", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject object2 = new JSONObject();
        try {
            object2.put("trade", "web");
            JSONArray array = new JSONArray();
            array.put("ball");
            array.put("running");
            array.put("photo");
            object2.put("interests", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        boolean ret = true;
        try {
            ret = JsonUtils.compareJsons(object1, object2, null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "jsonUtilsTest2: ret:" + ret);
        Assert.assertTrue(ret);
    }

    @Test
    public void jsonUtilsTest3() {

        JSONObject object1 = new JSONObject();
        try {
            object1.put("trade", "web");
            JSONArray array = new JSONArray();
            array.put("running");
            array.put("ball");
            array.put("photo");
            object1.put("interests", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject object2 = new JSONObject();
        try {
            object2.put("trade", "web");
            JSONArray array = new JSONArray();
            array.put("ball");
            array.put("running");
            array.put("photo");
            object2.put("interests", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        boolean ret = true;
        try {
            ret = JsonUtils.compareJsons(object1, object2, null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "jsonUtilsTest3: ret:" + ret);
        Assert.assertTrue(ret);
    }

    @Test
    public void jsonUtilsTest4() {

        JSONObject object1 = new JSONObject();
        try {
            object1.put("trade", "web");
            JSONArray array = new JSONArray();
            array.put("running");
            array.put("ball");
            array.put("photo");
            object1.put("interests", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject object2 = new JSONObject();
        try {
            object2.put("trade", "web");
            JSONArray array = new JSONArray();
            array.put("ball");
            array.put("running");
            array.put("photo");
            array.put("program");
            object2.put("interests", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        boolean ret = true;
        try {
            ret = JsonUtils.compareJsons(object1, object2, null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "jsonUtilsTest4: ret:" + ret);
        Assert.assertFalse(ret);
    }

    @Test
    public void jsonUtilsTest5() {

        JSONObject object1 = new JSONObject();
        try {
            object1.put("trade", "web");
            JSONArray array = new JSONArray();
            array.put("running");
            array.put("ball");
            array.put("photo");
            array.put("program");
            array.put("running");
            object1.put("interests", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject object2 = new JSONObject();
        try {
            object2.put("trade", "web");
            JSONArray array = new JSONArray();
            array.put("ball");
            array.put("running");
            array.put("photo");
            array.put("program");
            object2.put("interests", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        boolean ret = true;
        try {
            ret = JsonUtils.compareJsons(object1, object2, null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "jsonUtilsTest5: ret:" + ret);
        Assert.assertFalse(ret);
    }

    @Test
    public void jsonUtilsTest6() {

        JSONObject object1 = new JSONObject();
        try {
            object1.put("trade", "web");
            JSONArray array = new JSONArray();
            array.put("running");
            array.put("ball");
            array.put("photo");
            array.put("program");
            array.put("running");
            object1.put("interests", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject object2 = new JSONObject();
        try {
            object2.put("trade", "web");
            JSONArray array = new JSONArray();
            array.put("running");
            array.put("running");
            array.put("ball");
            array.put("photo");
            array.put("program");
            object2.put("interests", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        boolean ret = true;
        try {
            ret = JsonUtils.compareJsons(object1, object2, null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "jsonUtilsTest6: ret:" + ret);
        Assert.assertTrue(ret);
    }

    @Test
    public void jsonUtilsTest7() {

        JSONObject object1 = new JSONObject();
        try {
            object1.put("trade", "web");
            JSONArray array = new JSONArray();
            array.put("running");
            array.put("ball");
            array.put("photo");
            array.put("program");
            array.put("running2");
            object1.put("interests", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject object2 = new JSONObject();
        try {
            object2.put("trade", "web");
            JSONArray array = new JSONArray();
            array.put("running");
            array.put("running");
            array.put("ball");
            array.put("photo");
            array.put("program");
            object2.put("interests", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        boolean ret = true;
        try {
            ret = JsonUtils.compareJsons(object1, object2, null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "jsonUtilsTest6: ret:" + ret);
        Assert.assertFalse(ret);
    }


    @Test
    public void jsonUtilsTest8() {

        JSONObject object1 = new JSONObject();
        try {
            object1.put("trade", "web");
            object1.put("test", "testing");
            JSONArray array = new JSONArray();
            array.put("running");
            array.put("ball");
            array.put("photo");
            array.put("program");
            array.put("running2");
            object1.put("interests", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject object2 = new JSONObject();
        try {
            object2.put("trade", "web");
            JSONArray array = new JSONArray();
            array.put("running");
            array.put("running");
            array.put("ball");
            array.put("photo");
            array.put("program");
            object2.put("interests", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        boolean ret = true;
        try {
            ret = JsonUtils.compareJsons(object1, object2, null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "jsonUtilsTest6: ret:" + ret);
        Assert.assertFalse(ret);
    }

    @Test
    public void jsonUtilsTest9() {

        JSONObject object1 = new JSONObject();
        try {


            JSONArray array = new JSONArray();
            array.put("running");
            array.put("ball");
            array.put("photo");
            array.put("program");
            array.put("running");
            object1.put("interests", array);
            object1.put("trade", "web");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject object2 = new JSONObject();
        try {
            object2.put("trade", "web");
            JSONArray array = new JSONArray();
            array.put("running");
            array.put("running");
            array.put("ball");
            array.put("photo");
            array.put("program");
            object2.put("interests", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        boolean ret = true;
        try {
            ret = JsonUtils.compareJsons(object1, object2, null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "jsonUtilsTest9: ret:" + ret);
        Assert.assertTrue(ret);
    }


    @Test
    public void jsonUtilsTest10() {

        JSONObject object1 = new JSONObject();
        try {
            JSONArray array = new JSONArray();
            array.put("running");
            array.put("ball");
            array.put("photo");
            array.put("program");
            array.put("running");
            object1.put("interests", array);
            object1.put("trade", "web");
            object1.put("num",2);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject object2 = new JSONObject();
        try {
            object2.put("trade", "web");
            JSONArray array = new JSONArray();
            array.put("running");
            array.put("running");
            array.put("ball");
            array.put("photo");
            array.put("program");
            object2.put("interests", array);
            object2.put("num",2.0f);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        boolean ret = true;
        try {
            ret = JsonUtils.compareJsons(object1, object2, null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "jsonUtilsTest10: ret:" + ret);
        Assert.assertFalse(ret);
    }



    @Test
    public void jsonUtilsTest11() {

        JSONObject object1 = new JSONObject();
        try {
            JSONArray array = new JSONArray();
            array.put("running");
            array.put("ball");
            array.put("photo");
            array.put("program");
            array.put("running");
            object1.put("interests", array);
            object1.put("trade", "web");
            object1.put("num",2);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject object2 = new JSONObject();
        try {
            object2.put("trade", "web");
            JSONArray array = new JSONArray();
            array.put("running");
            array.put("running");
            array.put("ball");
            array.put("photo");
            array.put("program");
            object2.put("interests", array);
            object2.put("num","2");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        boolean ret = true;
        try {
            ret = JsonUtils.compareJsons(object1, object2, null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "jsonUtilsTest10: ret:" + ret);
        Assert.assertFalse(ret);
    }

    @Test
    public void jsonUtilsTest12() {

        JSONObject object1 = new JSONObject();
        try {
            JSONArray array = new JSONArray();
            array.put("running");
            array.put("ball");
            array.put("photo");
            array.put("program");
            array.put("running");
            object1.put("interests", array);
            object1.put("trade", "web");
            object1.put("bool",true);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject object2 = new JSONObject();
        try {
            object2.put("trade", "web");
            JSONArray array = new JSONArray();
            array.put("running");
            array.put("running");
            array.put("ball");
            array.put("photo");
            array.put("program");
            object2.put("interests", array);
            object2.put("bool",true);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        boolean ret = true;
        try {
            ret = JsonUtils.compareJsons(object1, object2, null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "jsonUtilsTest10: ret:" + ret);
        Assert.assertTrue(ret);
    }

}
