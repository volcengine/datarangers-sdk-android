// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.profile;

import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.bytedance.applog.AppLog;
import com.bytedance.applog.BaseAppLogTest;
import com.bytedance.applog.engine.Engine;
import com.bytedance.applog.util.ReflectUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.HashSet;

@Ignore
@RunWith(AndroidJUnit4.class)
public class ProfileApiTest extends BaseAppLogTest {

    private static final String TAG = "ProfileApiTest";

    @Before
    public void setUp() {}

    @After
    public void down() {
        AppLog.flush();
    }

    @Test
    public void profileSetTest1() {
        AppLog.onEventV3("start");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        final Engine engine = ReflectUtils.getFieldValue(AppLog.class, "sEngine");
        ProfileController profileController = new ProfileController(engine);

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
        Log.i(TAG, "profileSetTest1: profileController:" + profileController);
        profileController.profileSet(object1);

        HashMap<String, ProfileController.ProfileDataWrapper> map =
                ReflectUtils.getFieldValue(profileController, "mapForSet");

        Log.i(TAG, "profileSetTest: map.size():" + map.size());
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(map.size() > 0);

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
        profileController.profileSet(object2);

        UnitTestUtils.assertEventV3Count("__profile_set", 1);
    }

    @Test
    public void profileSetTest2() {
        AppLog.onEventV3("start");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        final Engine engine = ReflectUtils.getFieldValue(AppLog.class, "sEngine");
        ProfileController profileController = new ProfileController(engine);

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
        Log.i(TAG, "profileSetTest2: profileController:" + profileController);
        profileController.profileSet(object1);

        HashMap<String, ProfileController.ProfileDataWrapper> map =
                ReflectUtils.getFieldValue(profileController, "mapForSet");

        Log.i(TAG, "profileSetTest2: map.size():" + map.size());
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(map.size() > 0);

        JSONObject object2 = new JSONObject();
        try {
            object2.put("trade", "web");
            JSONArray array = new JSONArray();
            array.put("ball");
            array.put("photo");
            array.put("running");
            object2.put("interests", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        profileController.profileSet(object2);

        UnitTestUtils.assertEventV3Count("__profile_set", 1);
    }

    @Test
    public void profileSetTest3() {
        AppLog.onEventV3("start");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        final Engine engine = ReflectUtils.getFieldValue(AppLog.class, "sEngine");
        ProfileController profileController = new ProfileController(engine);
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
        profileController.profileSet(object1);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        HashMap<String, ProfileController.ProfileDataWrapper> map =
                ReflectUtils.getFieldValue(profileController, "mapForSet");

        Log.i(TAG, "profileSetTest: map.size():" + map.size());
        Assert.assertTrue(map.size() > 0);

        JSONObject object2 = new JSONObject();
        try {
            object2.put("trade", "web");
            JSONArray array = new JSONArray();
            array.put("ball");
            array.put("photo");
            array.put("photo");
            object2.put("interests", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        profileController.profileSet(object2);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        UnitTestUtils.assertEventV3Count("__profile_set", 2);
    }

    @Test
    public void profileSetTest4() {
        AppLog.onEventV3("start");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        final Engine engine = ReflectUtils.getFieldValue(AppLog.class, "sEngine");
        ProfileController profileController = new ProfileController(engine);

        JSONObject object1 = new JSONObject();
        try {
            object1.put("trade", "web");
            JSONArray array = new JSONArray();
            array.put("ball");
            array.put("running");
            array.put("photo");
            array.put("photo");
            object1.put("interests", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        profileController.profileSet(object1);

        HashMap<String, ProfileController.ProfileDataWrapper> map =
                ReflectUtils.getFieldValue(profileController, "mapForSet");

        Log.i(TAG, "profileSetTest: map.size():" + map.size());
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(map.size() > 0);

        JSONObject object2 = new JSONObject();
        try {
            object2.put("trade", "web");
            JSONArray array = new JSONArray();
            array.put("ball");
            array.put("photo");
            array.put("photo");
            array.put("running");
            object2.put("interests", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        profileController.profileSet(object2);

        UnitTestUtils.assertEventV3Count("__profile_set", 1);
    }

    @Test
    public void test() {
        AppLog.onEventV3("start");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        final Engine engine = ReflectUtils.getFieldValue(AppLog.class, "sEngine");
        ProfileController profileController = new ProfileController(engine);

        for (int i = 0; i < 10; i++) {

            long l = System.currentTimeMillis();
            JSONObject object = new JSONObject();
            try {
                object.put("test_" + i, l);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            profileController.profileSet(object);
        }
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        UnitTestUtils.assertEventV3Count("__profile_set", 0);
    }

    @Test
    public void testSetOnce1() {
        AppLog.onEventV3("start");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        final Engine engine = ReflectUtils.getFieldValue(AppLog.class, "sEngine");
        ProfileController profileController = new ProfileController(engine);

        JSONObject object1 = new JSONObject();
        try {
            object1.put("trade", "web");
            JSONArray array = new JSONArray();
            array.put("ball");
            array.put("running");
            array.put("photo");
            array.put("photo");
            object1.put("interests", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        profileController.profileSetOnce(object1);

        HashSet<String> set = ReflectUtils.getFieldValue(profileController, "setForSetOnce");

        Log.i(TAG, "profileSetTest: map.size():" + set.size());
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(set.size() > 0);

        JSONObject object2 = new JSONObject();
        try {
            object2.put("trade", "web");
            JSONArray array = new JSONArray();
            array.put("ball");
            array.put("photo");
            array.put("photo");
            array.put("running");
            object2.put("interests", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        profileController.profileSetOnce(object2);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        UnitTestUtils.assertEventV3Count("__profile_set_once", 1);
    }

    @Test
    public void testSetOnce2() {
        AppLog.onEventV3("start");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        final Engine engine = ReflectUtils.getFieldValue(AppLog.class, "sEngine");
        ProfileController profileController = new ProfileController(engine);

        JSONObject object1 = new JSONObject();
        try {
            object1.put("trade", "web");
            JSONArray array = new JSONArray();
            array.put("ball");
            array.put("running");
            array.put("photo");
            array.put("photo");
            object1.put("interests", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        profileController.profileSetOnce(object1);

        HashSet<String> set = ReflectUtils.getFieldValue(profileController, "setForSetOnce");

        Log.i(TAG, "profileSetTest: set.size():" + set.size());
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(set.size() > 0);

        JSONObject object2 = new JSONObject();
        try {
            object2.put("trade", "web");
            object2.put("test", "testing");
            JSONArray array = new JSONArray();
            array.put("ball");
            array.put("photo");
            array.put("photo");
            array.put("running");
            object2.put("interests", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        profileController.profileSetOnce(object2);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        UnitTestUtils.assertEventV3Count("__profile_set_once", 2);
    }

    @Test
    public void test1() {
        AppLog.onEventV3("start");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        final Engine engine = ReflectUtils.getFieldValue(AppLog.class, "sEngine");
        ProfileController profileController = new ProfileController(engine);
        JSONObject object1 = new JSONObject();
        try {
            object1.put("user_level", 10);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        profileController.profileSet(object1);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        UnitTestUtils.assertEventV3Count("__profile_set", 1);

        JSONObject object2 = new JSONObject();
        try {
            object2.put("user_level", 10);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        profileController.profileUnset(object2);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        UnitTestUtils.assertEventV3Count("__profile_unset", 1);

        JSONObject object3 = new JSONObject();
        try {
            object3.put("user_level", 10);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        profileController.profileSet(object3);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        UnitTestUtils.assertEventV3Count("__profile_set", 0);
    }

    @Test
    public void test2() {
        AppLog.onEventV3("start");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        final Engine engine = ReflectUtils.getFieldValue(AppLog.class, "sEngine");
        ProfileController profileController = new ProfileController(engine);
        JSONObject object1 = new JSONObject();
        try {
            object1.put("user_level", 10);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        profileController.profileSetOnce(object1);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        UnitTestUtils.assertEventV3Count("__profile_set_once", 1);

        JSONObject object2 = new JSONObject();
        try {
            object2.put("user_level", 10);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        profileController.profileUnset(object2);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        UnitTestUtils.assertEventV3Count("__profile_unset", 1);

        JSONObject object3 = new JSONObject();
        try {
            object3.put("user_level", 10);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        profileController.profileSetOnce(object3);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        UnitTestUtils.assertEventV3Count("__profile_set_once", 0);
    }

    @Test
    public void test3() {
        AppLog.onEventV3("start");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        final Engine engine = ReflectUtils.getFieldValue(AppLog.class, "sEngine");
        ProfileController profileController = new ProfileController(engine);
        JSONObject object1 = new JSONObject();
        try {
            object1.put("user_level", 10);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        profileController.profileSet(object1);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        UnitTestUtils.assertEventV3Count("__profile_set", 1);

        JSONObject object2 = new JSONObject();
        try {
            object2.put("user_level", 10);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        profileController.profileSet(object2);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        UnitTestUtils.assertEventV3Count("__profile_set", 0);
    }

    @Test
    public void test4() {
        AppLog.onEventV3("start");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        final Engine engine = ReflectUtils.getFieldValue(AppLog.class, "sEngine");
        ProfileController profileController = new ProfileController(engine);
        JSONObject object1 = new JSONObject();
        try {
            object1.put("user_level", 10);
            JSONArray array = new JSONArray();
            array.put("ball");
            array.put("running");
            array.put("photo");
            object1.put("interests", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        profileController.profileSet(object1);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        UnitTestUtils.assertEventV3Count("__profile_set", 1);

        JSONObject object2 = new JSONObject();
        try {
            object2.put("user_level", 10);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        profileController.profileSet(object2);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        UnitTestUtils.assertEventV3Count("__profile_set", 1);

        JSONObject object3 = new JSONObject();
        try {
            object3.put("user_level", 10);
            JSONArray array = new JSONArray();
            array.put("ball");
            array.put("running");
            array.put("photo");
            object3.put("interests", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        profileController.profileSet(object3);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        UnitTestUtils.assertEventV3Count("__profile_set", 1);
    }

    @Test
    public void test5() {
        AppLog.onEventV3("start");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        final Engine engine = ReflectUtils.getFieldValue(AppLog.class, "sEngine");
        ProfileController profileController = new ProfileController(engine);
        JSONObject object1 = new JSONObject();
        try {
            object1.put("user_level", 10);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        profileController.profileSet(object1);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        UnitTestUtils.assertEventV3Count("__profile_set", 1);

        JSONObject object2 = new JSONObject();
        try {
            object2.put("user_level", 11);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        profileController.profileSet(object2);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        UnitTestUtils.assertEventV3Count("__profile_set", 1);

        JSONObject object3 = new JSONObject();
        try {
            object3.put("user_level", 10);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        profileController.profileSet(object3);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        UnitTestUtils.assertEventV3Count("__profile_set", 1);
    }

    @Test
    public void test6() {
        AppLog.onEventV3("start");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        final Engine engine = ReflectUtils.getFieldValue(AppLog.class, "sEngine");
        ProfileController profileController = new ProfileController(engine);
        JSONObject object1 = new JSONObject();
        try {
            object1.put("user_level", 10);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        profileController.profileSet(object1);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        UnitTestUtils.assertEventV3Count("__profile_set", 1);

        JSONObject object2 = new JSONObject();
        try {
            object2.put("user_level", 10);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        profileController.profileSet(object2);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        UnitTestUtils.assertEventV3Count("__profile_set", 0);
    }

    @Test
    public void test7() {
        AppLog.onEventV3("start");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        final Engine engine = ReflectUtils.getFieldValue(AppLog.class, "sEngine");
        ProfileController profileController = new ProfileController(engine);
        JSONObject object1 = new JSONObject();
        try {
            object1.put("user_level", 10);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        profileController.profileSet(object1);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        UnitTestUtils.assertEventV3Count("__profile_set", 1);

        try {
            Thread.sleep(61 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        JSONObject object2 = new JSONObject();
        try {
            object2.put("user_level", 10);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        profileController.profileSet(object2);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        UnitTestUtils.assertEventV3Count("__profile_set", 1);
    }
}
