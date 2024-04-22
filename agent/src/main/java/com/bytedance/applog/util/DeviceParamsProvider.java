// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.accounts.Account;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.text.TextUtils;

import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.manager.ConfigManager;
import com.bytedance.applog.manager.DeviceManager;
import com.bytedance.applog.server.Api;
import com.bytedance.applog.store.AccountCacheHelper;
import com.bytedance.applog.store.CacheHelper;
import com.bytedance.applog.store.SharedPreferenceCacheHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Created by qianhong on
 * 2019/6/2.
 */
public class DeviceParamsProvider implements IDeviceRegisterParameter {
    private static final String TAG = "DeviceParamsProvider";
    private static final String LOCAL_TEST_SUFFIX_STR = "_local";
    private final Context mContext;
    private CacheHelper mCacheHandler;
    private final AccountCacheHelper mAccountCacheHandler;
    private static String sOpenUdid;
    private static String sOpenClientUdid;
    private static String sUdid;
    private static JSONArray sUdidList;
    private static volatile String sDeviceId;
    private static String[] sAccid;
    private static String sSerialNumber;

    /** 设备注册关键字段添加的后缀，用于生成新的did */
    private final String mLocalTestSuffix;

    private final AppLogInstance appLogInstance;
    private final ConfigManager mConfig;
    private final List<String> loggerTags = Collections.singletonList(TAG);

    public DeviceParamsProvider(
            AppLogInstance appLogInstance,
            Context context,
            ConfigManager cgm,
            AccountCacheHelper cache) {
        this.appLogInstance = appLogInstance;
        mConfig = cgm;
        mLocalTestSuffix = createLocalTestSuffix(cgm.isLocalTest());
        this.mContext = context.getApplicationContext();
        DeprecatedFileCleaner fileCleaner = new DeprecatedFileCleaner();
        mAccountCacheHandler = cache;
        mCacheHandler =
                new SharedPreferenceCacheHelper(
                        mContext, DeviceManager.SP_FILE_OPEN_UDID, cgm.getInitConfig().getSpName());
        mCacheHandler.setSuccessor(mAccountCacheHandler);
        if (!cgm.isAnonymous()) {
            fileCleaner.execute();
        }
        setAccount(cgm.getAccount());
    }

    /**
     * @param isLocalTest 是否为内测包
     * @return
     */
    private String createLocalTestSuffix(boolean isLocalTest) {
        return isLocalTest ? LOCAL_TEST_SUFFIX_STR : "";
    }

    @Override
    public String getOpenUdid() {
        if (!TextUtils.isEmpty(sOpenUdid)) {
            return sOpenUdid;
        }
        String openudid = !mConfig.isAndroidIdEnabled() ? ""
                : HardwareUtils.getSecureAndroidId(mContext);
        try {
            if (!Utils.isValidUDID(openudid) || DeviceManager.FAKE_ANDROID_ID.equals(openudid)) {
                // load from SharedPreferences
                SharedPreferences sp =
                        SharedPreferenceCacheHelper.getSafeSharedPreferences(
                                mContext, DeviceManager.SP_FILE_OPEN_UDID, Context.MODE_PRIVATE);
                String candidate = sp.getString(Api.KEY_OPEN_UDID, null);
                if (!Utils.isValidUDID(candidate)) {
                    final SecureRandom random = new SecureRandom();
                    candidate = new BigInteger(80, random).toString(16);
                    if (candidate.charAt(0) == '-') {
                        candidate = candidate.substring(1);
                    }
                    int left = (DeviceManager.MIN_UDID_LENGTH - candidate.length());
                    if (left > 0) {
                        StringBuilder sb = new StringBuilder();
                        while (left > 0) {
                            sb.append('F');
                            left--;
                        }
                        sb.append(candidate);
                        candidate = sb.toString();
                    }
                    // save to SharedPreference
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString(Api.KEY_OPEN_UDID, candidate);
                    editor.apply();
                } else {
                    // 单纯保存操作
                    mAccountCacheHandler.loadOpenUdid(candidate, null);
                }
                openudid = candidate;
            } else {
                openudid = mCacheHandler.loadOpenUdid(null, openudid);
            }
        } catch (Throwable e) {
            appLogInstance.getLogger().error(loggerTags, "getOpenUdid failed", e);
        }
        if (!TextUtils.isEmpty(openudid)) {
            openudid += mLocalTestSuffix;
        }
        if (!TextUtils.isEmpty(openudid)) {
            sOpenUdid = openudid;
        }
        return openudid;
    }

    /**
     * 原来的逻辑基础上加上了保存到其他3个文件
     *
     * @return udid
     */
    @Override
    public String getClientUDID() {
        if (!TextUtils.isEmpty(sOpenClientUdid)) {
            return sOpenClientUdid;
        }
        try {
            // load from SharedPreferences
            SharedPreferences sp =
                    SharedPreferenceCacheHelper.getSafeSharedPreferences(
                            mContext, DeviceManager.SP_FILE_OPEN_UDID, Context.MODE_PRIVATE);
            String candidate = sp.getString(Api.KEY_C_UDID, null);
            if (!Utils.isValidUDID(candidate)) {
                candidate = UUID.randomUUID().toString();
                // save to SharedPreference
                SharedPreferences.Editor editor = sp.edit();
                editor.putString(Api.KEY_C_UDID, candidate);
                editor.apply();
            } else {
                mAccountCacheHandler.loadClientUdid(candidate, null);
            }
            if (!TextUtils.isEmpty(candidate)) {
                candidate += mLocalTestSuffix;
            }
            sOpenClientUdid = candidate;
            return candidate;
        } catch (Throwable e) {
            appLogInstance.getLogger().error(loggerTags, "getClientUDID failed", e);
            return "";
        }
    }

    @Override
    public String getSerialNumber() {
        if (!TextUtils.isEmpty(sSerialNumber)) {
            return sSerialNumber;
        }
        try {
            String serialNumber = SensitiveUtils.getSerialNumber(mContext);
            serialNumber = mCacheHandler.loadSerialNumber(null, serialNumber);
            if (!TextUtils.isEmpty(serialNumber)) {
                serialNumber += mLocalTestSuffix;
            }
            sSerialNumber = serialNumber;
            return serialNumber;
        } catch (Throwable e) {
            appLogInstance.getLogger().error(loggerTags, "getSerialNumber failed", e);
        }
        return null;
    }

    @Override
    public String[] getSimSerialNumbers() {
        if (sAccid != null && sAccid.length > 0) {
            return sAccid;
        }
        try {
            String[] accid = SensitiveUtils.getSimSerialNumbers(mContext);
            accid = mCacheHandler.loadAccId(null, accid);
            if (accid == null) {
                accid = new String[0];
            }
            for (int i = 0; i < accid.length; ++i) {
                accid[i] += mLocalTestSuffix;
            }
            sAccid = accid;
            return accid;
        } catch (Throwable e) {
            appLogInstance.getLogger().error(loggerTags, "getSimSerialNumbers failed", e);
        }
        return null;
    }

    @Override
    public String getUdId() {
        if (!TextUtils.isEmpty(sUdid)) {
            return sUdid;
        }
        try {
            String udId =
                    mConfig.isImeiEnable()
                            ? SensitiveUtils.getDeviceId(mContext)
                            : mConfig.getAppImei();
            udId = mCacheHandler.loadUdid(null, udId);
            if (!TextUtils.isEmpty(udId)) {
                udId += mLocalTestSuffix;
            }
            sUdid = udId;
            return udId;
        } catch (Throwable e) {
            appLogInstance.getLogger().error(loggerTags, "getUdId failed", e);
        }
        return null;
    }

    @Override
    public JSONArray getUdIdList() {
        if (sUdidList != null) {
            return sUdidList;
        }
        try {
            // add imei enable switch
            if (!mConfig.isImeiEnable()) {
                return new JSONArray();
            }
            JSONArray udIdList = SensitiveUtils.getMultiImeiFromSystem(mContext);
            if (null == udIdList) {
                udIdList = SensitiveUtils.getMultiImeiFallback(mContext);
            }
            String udIdListString = mCacheHandler.loadUdidList(null, udIdList.toString());
            udIdList = new JSONArray(udIdListString);
            if (!TextUtils.isEmpty(mLocalTestSuffix)) {
                addSuffixToId(udIdList, mLocalTestSuffix);
            }
            sUdidList = udIdList;
            return sUdidList;
        } catch (Throwable e) {
            appLogInstance.getLogger().error(loggerTags, "getUdIdList failed", e);
        }
        return null;
    }

    private void addSuffixToId(JSONArray list, String suffix) throws JSONException {
        if (list != null && list.length() != 0) {
            for (int i = 0; i < list.length(); i++) {
                JSONObject vl = list.optJSONObject(i);
                if (vl != null) {
                    String id = vl.optString("id");
                    if (!TextUtils.isEmpty(id)) {
                        vl.remove("id");
                        vl.put("id", id + suffix);
                    }
                }
            }
        }
    }

    @Override
    public String getDeviceId() {
        if (!TextUtils.isEmpty(sDeviceId)) {
            return sDeviceId;
        }
        sDeviceId = mCacheHandler.loadDeviceId("", "");
        return sDeviceId;
    }

    @Override
    public void updateDeviceId(String deviceId) {
        if (!Utils.checkId(deviceId) || Utils.equals(deviceId, sDeviceId)) {
            return;
        }
        sDeviceId = mCacheHandler.loadDeviceId(deviceId, sDeviceId);
    }

    @Override
    public void setAccount(Account account) {
        if (mAccountCacheHandler != null) {
            mAccountCacheHandler.setAccount(account);
        }
    }

    @Override
    public void clear(final String clearKey) {
        mCacheHandler.clear(clearKey);

        appLogInstance
                .getLogger()
                .debug(
                        loggerTags,
                        "DeviceParamsProvider#clear clearKey="
                                + clearKey
                                + " sDeviceId="
                                + sDeviceId);
    }

    public void setCacheHandler(Handler handler) {
        mCacheHandler.setHandler(handler);
    }

    public void clearDidAndIid(Context context, final String clearKey) {
        appLogInstance
                .getLogger()
                .debug(
                        loggerTags,
                        "DeviceParamsProvider#clearDidAndIid clearKey="
                                + clearKey
                                + " sDeviceId="
                                + sDeviceId);

        if (TextUtils.isEmpty(clearKey)) {
            return;
        }
        sDeviceId = null;
        // clear sp
        String realClearKey = Api.KEY_CLEAR_KEY_PREFIX + clearKey;
        SharedPreferences sp =
                SharedPreferenceCacheHelper.getSafeSharedPreferences(
                        context, mConfig.getInitConfig().getSpName(), Context.MODE_PRIVATE);
        // 是否已经清除过了
        boolean cleared = sp.getBoolean(realClearKey, false);
        if (!cleared) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean(realClearKey, true);
            if (sp.contains(Api.KEY_DEVICE_ID)) {
                editor.remove(Api.KEY_DEVICE_ID);
            }
            if (sp.contains(Api.KEY_INSTALL_ID)) {
                editor.remove(Api.KEY_INSTALL_ID);
            }
            editor.apply();
            // clear
            mCacheHandler.clear(Api.KEY_DEVICE_ID);
            appLogInstance
                    .getLogger()
                    .debug(loggerTags, "clearKey:{} installId and deviceId finish", clearKey);
        } else {
            appLogInstance
                    .getLogger()
                    .debug(loggerTags, "clearKey:{} is already cleared", clearKey);
        }
    }
}
