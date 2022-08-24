// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.profile;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.bytedance.applog.engine.Engine;
import com.bytedance.applog.manager.DeviceManager;
import com.bytedance.applog.server.Api;
import com.bytedance.applog.store.BaseData;
import com.bytedance.applog.store.Profile;
import com.bytedance.applog.util.JsonUtils;
import com.bytedance.applog.util.TLog;
import com.bytedance.applog.util.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProfileController implements Handler.Callback {
    private static final String PROFILE_PREFIX = "__profile_";
    private static final String PROFILE_SET = "set";
    private static final String PROFILE_SET_ONCE = "set_once";
    private static final String PROFILE_INCREMENT = "increment";
    private static final String PROFILE_UNSET = "unset";
    private static final String PROFILE_APPEND = "append";

    private static final int MSG_SET = 100;
    private static final int MSG_SET_ONCE = 102;
    private static final int MSG_INCREMENT = 103;
    private static final int MSG_UNSET = 104;
    private static final int MSG_APPEND = 105;
    private static final int MSG_FLUSH = 106;

    private static final long INTERVAL_1_MIN = 1000 * 60;

    private final Engine mEngine;
    private final Handler mHandler;
    private final Map<String, ProfileDataWrapper> mapForSet = new HashMap<>();
    private final Set<String> setForSetOnce = new HashSet<>();

    private String ssid = "";

    public ProfileController(Engine engine) {
        mEngine = engine;
        HandlerThread handlerThread =
                new HandlerThread("bd_tracker_profile:" + engine.getAppLog().getAppId());
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper(), this);
    }

    public void profileSet(JSONObject jsonObject) {
        sendProfileDataMessage(
                MSG_SET,
                new ProfileDataWrapper(System.currentTimeMillis(), PROFILE_SET, jsonObject));
    }

    public void profileSetOnce(JSONObject jsonObject) {
        sendProfileDataMessage(
                MSG_SET_ONCE,
                new ProfileDataWrapper(System.currentTimeMillis(), PROFILE_SET_ONCE, jsonObject));
    }

    public void profileIncrement(JSONObject jsonObject) {
        sendProfileDataMessage(
                MSG_INCREMENT,
                new ProfileDataWrapper(System.currentTimeMillis(), PROFILE_INCREMENT, jsonObject));
    }

    public void profileUnset(JSONObject jsonObject) {
        sendProfileDataMessage(
                MSG_UNSET,
                new ProfileDataWrapper(System.currentTimeMillis(), PROFILE_UNSET, jsonObject));
    }

    public void profileAppend(JSONObject jsonObject) {
        sendProfileDataMessage(
                MSG_APPEND,
                new ProfileDataWrapper(System.currentTimeMillis(), PROFILE_APPEND, jsonObject));
    }

    public void profileFlush() {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_FLUSH));
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_SET:
                handleSet((ProfileDataWrapper) msg.obj);
                break;
            case MSG_SET_ONCE:
                handleSetOnce((ProfileDataWrapper) msg.obj);
                break;
            case MSG_INCREMENT:
                handleIncrement((ProfileDataWrapper) msg.obj);
                break;
            case MSG_UNSET:
                handleUnset((ProfileDataWrapper) msg.obj);
                break;
            case MSG_APPEND:
                handleAppend((ProfileDataWrapper) msg.obj);
                break;
            case MSG_FLUSH:
                handleFlush();
                break;
            default:
                break;
        }
        return true;
    }

    private void sendProfileDataMessage(int msg, ProfileDataWrapper dataWrapper) {
        if (!mEngine.getAppLog().isPrivacyMode()) {
            mHandler.sendMessage(mHandler.obtainMessage(msg, dataWrapper));
        }
    }

    private void handleSet(ProfileDataWrapper profileKV) {
        boolean overOneMin = false;
        boolean sameValue = true;
        boolean isSameSsid = false;
        if (ssid != null) {
            isSameSsid = ssid.equals(mEngine.getAppLog().getSsid());
        }
        ssid = mEngine.getAppLog().getSsid();

        Iterator<String> iterator = profileKV.jsonObject.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            if (mapForSet.containsKey(key) && mapForSet.get(key) != null) {
                ProfileDataWrapper latestProfileDataWrapper = mapForSet.get(key);
                if (null != latestProfileDataWrapper) {
                    long timeStamp = latestProfileDataWrapper.timeStamp;
                    if ((System.currentTimeMillis() - timeStamp) >= INTERVAL_1_MIN) {
                        overOneMin = true;
                    }
                    try {
                        if (!JsonUtils.compareJsons(
                                profileKV.jsonObject, latestProfileDataWrapper.jsonObject, null)) {
                            sameValue = false;
                        }
                    } catch (Throwable e) {
                        TLog.e(e);
                    }
                }
            } else {
                overOneMin = true;
                sameValue = false;
            }
            mapForSet.put(key, profileKV);
        }

        if (!isSameSsid || overOneMin || !sameValue) {
            TLog.d("invoke profile set.");
            saveAndSend(profileKV);
        }
    }

    private void handleSetOnce(ProfileDataWrapper profileKV) {
        boolean isSameSsid = false;
        if (ssid != null) {
            isSameSsid = ssid.equals(mEngine.getAppLog().getSsid());
        }
        ssid = mEngine.getAppLog().getSsid();
        Iterator<String> iterator = profileKV.jsonObject.keys();
        boolean hasSend = true;
        while (iterator.hasNext()) {
            String key = iterator.next();
            if (!setForSetOnce.contains(key)) {
                hasSend = false;
            }
            setForSetOnce.add(key);
        }
        if (!isSameSsid || !hasSend) {
            TLog.d("invoke profile set once.");
            saveAndSend(profileKV);
        }
    }

    private void handleIncrement(ProfileDataWrapper profileKV) {
        TLog.d("invoke profile increment.");
        saveAndSend(profileKV);
    }

    private void handleUnset(ProfileDataWrapper profileKV) {
        TLog.d("invoke profile unset.");
        saveAndSend(profileKV);
    }

    private void handleAppend(ProfileDataWrapper profileKV) {
        TLog.d("invoke profile append.");
        saveAndSend(profileKV);
    }

    private void saveAndSend(ProfileDataWrapper profileDataWrapper) {
        if (mEngine == null) {
            return;
        }
        Profile profile =
                new Profile(
                        PROFILE_PREFIX + profileDataWrapper.apiName,
                        profileDataWrapper.jsonObject.toString());
        ArrayList<BaseData> profileList = new ArrayList<>();
        mEngine.getSession().fillSessionParams(mEngine.getAppLog(), profile);
        mEngine.sendToRangersEventVerify(profile);
        profileList.add(profile);
        mEngine.getDbStoreV2().saveAll(profileList);
        Message message = mHandler.obtainMessage(MSG_FLUSH);
        mHandler.sendMessageDelayed(message, 500);
    }

    private void handleFlush() {
        if (mEngine == null) {
            return;
        }
        if (mEngine.getDm().getRegisterState() != DeviceManager.STATE_EMPTY) {
            Map<String, List<Profile>> uuidProfileMap =
                    mEngine.getDbStoreV2().queryAllProfiles(mEngine.getAppLog().getAppId());
            if (uuidProfileMap.isEmpty()) {
                return;
            }
            for (Map.Entry<String, List<Profile>> entry : uuidProfileMap.entrySet()) {
                String uuid = entry.getKey();
                try {
                    JSONObject header = new JSONObject();
                    Utils.copy(header, mEngine.getAppLog().getHeader());
                    header.put(
                            Api.KEY_USER_UNIQUE_ID, Utils.isEmpty(uuid) ? JSONObject.NULL : uuid);
                    header.remove(Api.KEY_SSID);

                    final JSONObject obj = new JSONObject();
                    JSONArray jsonArray = new JSONArray();
                    for (BaseData profile : entry.getValue()) {
                        jsonArray.put(profile.toPackJson());
                        if (Utils.isNotEmpty(profile.ssid) && !header.has(Api.KEY_SSID)) {
                            header.put(Api.KEY_SSID, profile.ssid);
                        }
                    }

                    // 如果没有ssid，则重新注册获取ssid
                    if (!mEngine.fetchIfNoSsidInHeader(header)) {
                        // 注册失败后等待下次打包重试注册
                        TLog.w("Register to get ssid by temp header failed.");
                        continue;
                    }

                    obj.put(Api.KEY_V3, jsonArray);
                    obj.put(Api.KEY_MAGIC, Api.MSG_MAGIC);
                    obj.put(Api.KEY_HEADER, header);
                    obj.put(Api.KEY_TIME_SYNC, Api.mTimeSync);
                    obj.put(Api.KEY_LOCAL_TIME, System.currentTimeMillis() / 1000);
                    byte[] data =
                            mEngine.getAppLog()
                                    .getApi()
                                    .getEncryptUtils()
                                    .transformStrToByte(obj.toString());
                    mEngine.getDbStoreV2().deleteProfiles(entry.getValue());
                    String[] uris = new String[] {mEngine.getUriConfig().getProfileUri()};
                    int responseCode =
                            mEngine.getAppLog().getApi().send(uris, data, mEngine.getConfig());
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        mEngine.getDbStoreV2().saveProfiles(entry.getValue());
                    }
                } catch (Throwable e) {
                    TLog.e(e);
                }
            }
        }
    }

    static class ProfileDataWrapper {
        long timeStamp;
        String apiName;
        JSONObject jsonObject;

        public ProfileDataWrapper(long timeStamp, String apiName, JSONObject jsonObject) {
            this.timeStamp = timeStamp;
            this.apiName = apiName;
            this.jsonObject = jsonObject;
        }

        @Override
        public String toString() {
            return "ProfileDataWrapper{"
                    + "timeStamp="
                    + timeStamp
                    + ", apiName='"
                    + apiName
                    + '\''
                    + ", jsonObject="
                    + jsonObject
                    + '}';
        }
    }
}
