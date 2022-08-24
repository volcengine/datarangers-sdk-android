// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.manager;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bytedance.applog.AppLogHelper;
import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.migrate.MigrateDetector;
import com.bytedance.applog.server.Api;
import com.bytedance.applog.util.DeviceParamsProvider;
import com.bytedance.applog.util.GaidGetter;
import com.bytedance.applog.util.IDeviceRegisterParameter;
import com.bytedance.applog.util.NetworkUtils;
import com.bytedance.applog.util.TLog;
import com.bytedance.applog.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

/**
 * 设备信息管理器，{@link #mHeader}中记录了所有的设备信息。
 *
 * @author shiyanlong
 * @date 2019/1/16
 */
public class DeviceManager {

    public static final String SP_FILE_OPEN_UDID = "snssdk_openudid";

    public static final String FAKE_ANDROID_ID = "9774d56d682e549c";

    private static final String KEY_NEW_USER = "new_user";

    private static final int MAX_PERMISSION = 10;

    private volatile boolean mAllReady;

    private final Context mApp;

    private final ConfigManager mConfig;

    @NonNull private volatile JSONObject mHeader;

    private boolean mIsNewUser;

    private final ArrayList<BaseLoader> mLoaders = new ArrayList<>(32);

    private static final String[] NOT_EMPTY =
            new String[] {Api.KEY_CHANNEL, Api.KEY_PACKAGE, Api.KEY_APP_VERSION};

    private final SharedPreferences mSp;
    private final IDeviceRegisterParameter mDeviceRegisterParameterProvider;
    private final AppLogInstance appLogInstance;

    public DeviceManager(
            final AppLogInstance appLogInstance, final Context app, final ConfigManager config) {
        this.appLogInstance = appLogInstance;
        mApp = app;
        mConfig = config;
        mSp = config.getStatSp();
        mHeader = new JSONObject();
        mDeviceRegisterParameterProvider =
                appLogInstance.getDeviceRegisterParameterFactory().getProvider(mApp, mConfig);
    }

    public void setProviderHandler(@NonNull Handler handler) {
        mDeviceRegisterParameterProvider.setCacheHandler(handler);
        clearMigrationInfo(mApp);
    }

    private void clearMigrationInfo(Context context) {
        MigrateDetector migrateDetector =
                new MigrateDetector(
                        context,
                        AppLogHelper.getInstanceSpName(appLogInstance, MigrateDetector.SP_FILE));
        if (migrateDetector.isMigrate()) {
            GaidGetter.clearSp(mConfig);
            MigrateDetector.saveOldDid(mSp, mDeviceRegisterParameterProvider.getDeviceId(), true);
            mDeviceRegisterParameterProvider.clear(Api.KEY_OPEN_UDID);
            mDeviceRegisterParameterProvider.clear(Api.KEY_C_UDID);
            mDeviceRegisterParameterProvider.clear(Api.KEY_SERIAL_NUMBER);
            mDeviceRegisterParameterProvider.clear(Api.KEY_SIM_SERIAL_NUMBER);
            mDeviceRegisterParameterProvider.clear(Api.KEY_UDID);
            mDeviceRegisterParameterProvider.clear(Api.KEY_UDID_LIST);
            mDeviceRegisterParameterProvider.clear(Api.KEY_DEVICE_ID);
            clearDidAndIid("clearMigrationInfo");
        }
        migrateDetector.disableComponent();
    }

    public void setAccount(final Account account) {
        appLogInstance.getDeviceRegisterParameterFactory().setAccount(account);
    }

    public IDeviceRegisterParameter getProvider() {
        return mDeviceRegisterParameterProvider;
    }

    @SuppressLint("ApplySharedPref")
    public void clearDidAndIid(String clearKey) {
        if (mDeviceRegisterParameterProvider instanceof DeviceParamsProvider) {
            ((DeviceParamsProvider) mDeviceRegisterParameterProvider)
                    .clearDidAndIid(mApp, clearKey);
        }
        mConfig.getStatSp().edit().remove(Api.KEY_DEVICE_TOKEN).commit();
    }

    @Nullable
    public JSONObject getHeader() {
        return mAllReady ? getConstHeader() : null;
    }

    @Nullable
    public <T> T getHeaderValue(String key, T fallbackValue, Class<T> tClass) {
        return appLogInstance
                .getApiParamsUtil()
                .getValue(getConstHeader(), key, fallbackValue, tClass);
    }

    /**
     * 这个命名，是希望外部尽量不修改。 该header sdk内部不会remove或者put任何字段，所有的修改都是直接赋值新JSONObject
     *
     * @return header
     */
    @androidx.annotation.NonNull
    private JSONObject getConstHeader() {
        return mHeader;
    }

    public int getVersionCode() {
        int code = mAllReady ? getConstHeader().optInt(Api.KEY_VERSION_CODE, -1) : -1;
        for (int i = 0; i < 3 && code == -1; ++i) {
            load();
            code = mAllReady ? getConstHeader().optInt(Api.KEY_VERSION_CODE, -1) : -1;
        }
        return code;
    }

    public String getVersionName() {
        String name = mAllReady ? getConstHeader().optString(Api.KEY_APP_VERSION, null) : null;
        for (int i = 0; i < 3 && name == null; ++i) {
            load();
            name = mAllReady ? getConstHeader().optString(Api.KEY_APP_VERSION, null) : null;
        }
        return name;
    }

    private int mCountPermission = 0;

    public boolean load() {
        synchronized (mLoaders) {
            if (mLoaders.size() == 0) {
                mLoaders.add(new BuildLoader(mApp, mConfig));
                mLoaders.add(new ConfigLoader(mApp, mConfig));
                mLoaders.add(new DisplayLoader(mApp));
                mLoaders.add(new LocaleLoader(mApp));
                mLoaders.add(
                        new SensitiveLoader(
                                mApp,
                                mConfig,
                                this,
                                null != appLogInstance.getInitConfig()
                                        ? appLogInstance.getInitConfig().getSensitiveInfoProvider()
                                        : null));
                mLoaders.add(new NetLoader(mApp));
                mLoaders.add(new PackageLoader(mApp, mConfig));
                mLoaders.add(new RomLoader());
                mLoaders.add(new ServerIdLoader(mApp, mConfig, this));
                mLoaders.add(new SigHashLoader(mApp));
                mLoaders.add(new SimCountryLoader(mApp));
                mLoaders.add(new DeviceParamsLoader(mApp, mConfig, this));
                mLoaders.add(new AppKeyLoader(mApp));
            }
        }

        JSONObject oldHeader = getConstHeader();
        JSONObject newHeader = new JSONObject();
        Utils.copy(newHeader, oldHeader);

        int permissionFail = 0;
        int loadFail = 0;
        boolean allReady = true;
        for (BaseLoader loader : mLoaders) {
            if (!loader.mReady || loader.mShouldUpdate || needSyncFromSub(loader)) {
                try {
                    loader.mReady = loader.doLoad(newHeader);
                } catch (JSONException e) {
                    TLog.ysnp(e);
                } catch (SecurityException e) {
                    if (!loader.mOptional) {
                        ++permissionFail;
                        TLog.w("loadHeader" + ", " + mCountPermission, e);

                        if (!loader.mReady && mCountPermission > MAX_PERMISSION) {
                            loader.mReady = true;
                        }
                    }
                }

                if (!loader.mReady && !loader.mOptional) {
                    ++loadFail;
                }
            }
            allReady &= loader.mReady || loader.mOptional;
        }
        if (allReady) {
            for (String key : NOT_EMPTY) {
                boolean isEmpty = TextUtils.isEmpty(newHeader.optString(key));
                allReady &= !isEmpty;
                if (isEmpty) {
                    TLog.w("Key " + key + " is empty!", null);
                }
            }
        }

        TLog.d("All loaders are ready? " + allReady);

        // 先保存mHeader,因为如果这个时候mHeader有值,是用户重新设置的,优先级应该要比从sp设置的值高,替换了mHeader之后要进行更新
        JSONObject header = mHeader;
        mHeader = newHeader;
        Iterator<String> keys = header.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = header.opt(key);
            updateHeader(key, value);
        }
        mAllReady = allReady;

        if (TLog.DEBUG) {
            TLog.d(
                    new TLog.LogGetter() {
                        @Override
                        public String log() {
                            return "loadHeader"
                                    + ", "
                                    + mAllReady
                                    + ", "
                                    + mCountPermission
                                    + ", "
                                    + mHeader;
                        }
                    });
        } else {
            TLog.i("loadHeader" + ", " + mAllReady + ", " + mCountPermission);
        }

        if (permissionFail > 0 && permissionFail == loadFail) {
            // 如果全是权限错误引起的加载失败，计次，外部会重试，最多MAX_PERMISSION次权限错误，就忽略权限错误。
            ++mCountPermission;
            if (getRegisterState() != STATE_EMPTY) {
                // 如果已经注册过，则忽略权限错误
                mCountPermission += MAX_PERMISSION;
            }
        }
        // 回调本地id已经加载，这个时机，外面才能通过get接口获取数据
        if (mAllReady && null != appLogInstance.getDataObserverHolder()) {
            appLogInstance
                    .getDataObserverHolder()
                    .onIdLoaded(appLogInstance.getDid(), getIid(), getSsid());
        }

        return mAllReady;
    }

    private boolean needSyncFromSub(final BaseLoader loader) {
        final boolean ret = !mConfig.isMainProcess() && loader.syncFromSub;
        TLog.d(
                new TLog.LogGetter() {
                    @Override
                    public String log() {
                        return "needSyncFromSub " + loader + " " + ret;
                    }
                });
        return ret;
    }

    public synchronized void setExternalAbVersion(final String version) {
        // 得到旧的外部设置即服务端ab实验vid列表
        Set<String> oldExternalVidSet = getSetFromString(mConfig.getExternalAbVersion());

        // 从ab_sdk_version中过滤出客户端ab实验已曝光vid列表
        Set<String> newVidSet =
                getSetFromString(getConstHeader().optString(Api.KEY_AB_SDK_VERSION));
        newVidSet.removeAll(oldExternalVidSet);

        // 与新的外部设置即服务端ab实验vid列表进行合并
        Set<String> newExternalVidSet = getSetFromString(version);
        newVidSet.addAll(newExternalVidSet);

        mConfig.setExternalAbVersion(version);
        setAbSdkVersion(getStringsFromSet(newVidSet));
    }

    public void setAbConfig(final JSONObject config) {
        mConfig.setAbConfig(config);
        updateExposedVidByNewConfig(config);
    }

    /**
     * 拉到新的客户端ab config，需要遍历已曝光客户端ab vid列表，如果新config不存在这个vid，则将该vid移出已曝光列表；
     * 同时服务端ab实验拉到vid列表即曝光，这里需要保留
     */
    private synchronized void updateExposedVidByNewConfig(JSONObject abConfig) {
        if (abConfig == null) {
            TLog.w("null abconfig", null);
        }

        String abVersion = getConstHeader().optString(Api.KEY_AB_SDK_VERSION);
        if (!TextUtils.isEmpty(abVersion)) {
            Set<String> exposedVidSet = getSetFromString(abVersion);

            Set<String> newVidSet = new HashSet<>();
            if (abConfig != null) {
                Iterator iterator = abConfig.keys();
                while (iterator.hasNext()) {
                    Object value = iterator.next();
                    if (value instanceof String) {
                        String key = (String) value;
                        if (!TextUtils.isEmpty(key)) {
                            try {
                                JSONObject jsonObject = abConfig.getJSONObject(key);
                                String vid = jsonObject.optString("vid");
                                newVidSet.add(vid);
                            } catch (JSONException e) {
                                TLog.ysnp(e);
                            }
                        }
                    }
                }
            }

            // 得到外部设置即服务端ab实验vid列表
            String externalAbVersion = mConfig.getExternalAbVersion();
            newVidSet.addAll(getSetFromString(externalAbVersion));

            // 根据已曝光客户端ab实验vid列表和拉取到新的客户端ab config进行合并，同时保留服务端ab实验vid列表
            exposedVidSet.retainAll(newVidSet);
            String abSdkVersion = getStringsFromSet(exposedVidSet);
            setAbSdkVersion(abSdkVersion);

            if (!TextUtils.equals(abVersion, abSdkVersion)) {
                notifyVidsChange(abSdkVersion, externalAbVersion);
            }
        }
    }

    private String getStringsFromSet(Set<String> exposedVidSet) {
        StringBuilder stringBuilder = new StringBuilder();
        Iterator<String> iterator = exposedVidSet.iterator();
        while (iterator.hasNext()) {
            stringBuilder.append(iterator.next());
            if (iterator.hasNext()) {
                stringBuilder.append(",");
            }
        }
        return stringBuilder.toString();
    }

    private Set<String> getSetFromString(String abSdkVersion) {
        Set<String> vidSet = new HashSet<String>();
        if (!TextUtils.isEmpty(abSdkVersion)) {
            String[] abVids = abSdkVersion.split(",");
            if (abVids != null && abVids.length > 0) {
                for (String vid : abVids) {
                    if (!TextUtils.isEmpty(vid)) {
                        vidSet.add(vid);
                    }
                }
            }
        }
        return vidSet;
    }

    public void setCustom(final HashMap<String, Object> custom) {
        JSONObject newCustom = null;
        if (custom != null && !custom.isEmpty()) {
            newCustom = new JSONObject();
            JSONObject oldCustom = getCustomInfo();
            if (oldCustom != null) {
                Utils.copy(newCustom, oldCustom);
            }
            try {
                for (Entry<String, Object> entry : custom.entrySet()) {
                    if (!TextUtils.isEmpty(entry.getKey())) {
                        newCustom.put(entry.getKey(), entry.getValue());
                    }
                }
            } catch (Exception e) {
                TLog.e(e);
            }
        }
        updateCustomInfo(newCustom);
    }

    public void updateNetworkAccessType() {
        String access = NetworkUtils.getNetworkAccessType(mApp);
        updateHeader(Api.KEY_ACCESS, access);
    }

    public void setTracerData(final JSONObject tracerData) {
        updateHeader(Api.KEY_TRACER_DATA, tracerData);
    }

    /**
     * 设置ALink预置属性
     *
     * @param utmAttrs
     */
    public void setALinkUtmAttr(JSONObject utmAttrs) {
        for (Iterator<String> it = utmAttrs.keys(); it.hasNext(); ) {
            String key = it.next();
            updateHeader(key, utmAttrs.optString(key));
        }
    }

    public void removeHeaderInfo(final String key) {
        if (TextUtils.isEmpty(key)) {
            return;
        }
        JSONObject oldCustom = getCustomInfo();
        if (oldCustom != null && oldCustom.has(key)) {
            JSONObject newCustom = new JSONObject();
            Utils.copy(newCustom, oldCustom);
            newCustom.remove(key);
            updateCustomInfo(newCustom);
        }
    }

    private JSONObject getCustomInfo() {
        JSONObject customInfo = null;
        if (mAllReady) {
            customInfo = getConstHeader().optJSONObject(Api.KEY_CUSTOM);
        } else if (mConfig != null) {
            try {
                customInfo = new JSONObject(mConfig.getCustomInfo());
            } catch (Exception e) {
            }
        }
        return customInfo;
    }

    private void updateCustomInfo(JSONObject customHeaderInfo) {
        if (updateHeader(Api.KEY_CUSTOM, customHeaderInfo)) {
            mConfig.setCustomInfo(customHeaderInfo);
        }
    }

    public String getUserUniqueId() {
        String uuid = "";
        if (mAllReady) {
            uuid = getConstHeader().optString(Api.KEY_USER_UNIQUE_ID, "");
        } else if (mConfig != null) {
            uuid = mConfig.getUserUniqueId();
        }
        return uuid;
    }

    /** 返回后台生成的did，如果没有，返回"" */
    public String getUserUniqueIdType() {
        return getConstHeader()
                .optString(Api.KEY_USER_UNIQUE_ID_TYPE, mConfig.getUserUniqueIdType());
    }

    /** 返回后台生成的did，如果没有，返回"" */
    public String getDid() {
        return getConstHeader().optString(Api.KEY_DEVICE_ID, "");
    }

    /** 返回后台生成的bd did，如果没有，返回"" */
    public String getBdDid() {
        return getConstHeader().optString(Api.KEY_BD_DID, "");
    }

    /** 返回Aid */
    public String getAid() {
        return mConfig.getAid();
    }

    /** 返回客户端生成的udid，如果没有，返回"" */
    public String getUdid() {
        return getConstHeader().optString(Api.KEY_UDID, "");
    }

    /** 返回iid，如果没有，返回"" */
    public String getIid() {
        return getConstHeader().optString(Api.KEY_INSTALL_ID, "");
    }

    /** 返回Ssid，如果没有，返回"" */
    public String getSsid() {
        return getConstHeader().optString(Api.KEY_SSID, "");
    }

    public String[] getSimSerialNumbers() {
        // TODO linguoqing
        return getProvider().getSimSerialNumbers();
    }

    /** 返回client_udid，如果没有，返回"" */
    public String getClientUdid() {
        return getConstHeader().optString(Api.KEY_C_UDID, "");
    }

    /** 返回open udid,如果没有，返回"" */
    public String getOpenUdid() {
        return getConstHeader().optString(Api.KEY_OPEN_UDID, "");
    }

    /** DeviceManager内部统一设置ab_sdk_version的接口，合并处理后调用，不对外开放 */
    public void setAbSdkVersion(final String version) {
        if (updateHeader(Api.KEY_AB_SDK_VERSION, version)) {
            mConfig.setAbSdkVersion(version);
        }
    }

    public void setAppTrackJson(final JSONObject appTrack) {
        if (updateHeader(Api.KEY_APP_TRACK, appTrack)) {
            mConfig.setAppTrack(appTrack.toString());
        }
    }

    public void setUserAgent(final String ua) {
        if (updateHeader(Api.KEY_USER_AGENT, ua)) {
            mConfig.setUserAgent(ua);
        }
    }

    public boolean setUserUniqueId(final String id) {
        if (updateHeader(Api.KEY_USER_UNIQUE_ID, id)) {
            mConfig.setUserUniqueId(id);
            return true;
        }
        return false;
    }

    public boolean setUserUniqueId(final String id, final String type) {
        if (updateHeader(Api.KEY_USER_UNIQUE_ID, id)) {
            mConfig.setUserUniqueId(id);
            setUserUniqueIdType(type);
            return true;
        }
        return false;
    }

    public void setUserUniqueIdType(final String type) {
        if (updateHeader(Api.KEY_USER_UNIQUE_ID_TYPE, type)) {
            mConfig.setUserUniqueIdType(type);
        }
    }

    public boolean setSsid(final String id) {
        if (updateHeader(Api.KEY_SSID, id)) {
            mSp.edit().putString(mConfig.getSsidSpKey(), id).apply();
            return true;
        } else {
            return false;
        }
    }

    /** 设置坐标信息 */
    public void setGPSLocation(float longitude, float latitude, String geoCoordinateSystem) {
        updateHeader(Api.KEY_GPS_LONGITUDE, longitude);
        updateHeader(Api.KEY_GPS_LATITUDE, latitude);
        updateHeader(Api.KEY_GPS_GCS, geoCoordinateSystem);
    }

    private boolean updateHeader(final String key, final Object value) {
        boolean changed = false;
        Object old = getConstHeader().opt(key);
        if (!Utils.equals(value, old)) {
            synchronized (this) {
                try {
                    final JSONObject oldHeader = mHeader;
                    final JSONObject newHeader = new JSONObject();
                    Utils.copy(newHeader, oldHeader);
                    newHeader.put(key, value);
                    mHeader = newHeader;
                } catch (JSONException e) {
                    TLog.ysnp(e);
                }
            }
            changed = true;
        }

        TLog.d("updateHeader" + ", " + key + ", " + old + ", " + value + ", changed:" + changed);

        return changed;
    }

    static void putString(JSONObject obj, String key, String value) throws JSONException {
        if (!TextUtils.isEmpty(value)) {
            obj.put(key, value);
        }
    }

    public static final int MIN_UDID_LENGTH = 13;

    public static final int MAX_UDID_LENGTH = 128;

    public static final int STATE_EMPTY = 0;

    public static final int STATE_SAME = 1;

    public static final int STATE_DIFF = 2;

    public int getRegisterState() {
        String did = getConstHeader().optString(Api.KEY_DEVICE_ID, "");
        String iid = getConstHeader().optString(Api.KEY_INSTALL_ID, "");
        String bdDid = getConstHeader().optString(Api.KEY_BD_DID, "");
        if ((Utils.checkId(did) || Utils.checkId(bdDid)) && Utils.checkId(iid)) {
            int lastVersion = mSp.getInt(Api.KEY_VERSION_CODE, 0);
            int currentVersion = getConstHeader().optInt(Api.KEY_VERSION_CODE, -1);
            return lastVersion == currentVersion ? STATE_SAME : STATE_DIFF;
        } else {
            return STATE_EMPTY;
        }
    }

    public long getLastRegisterTime() {
        return getConstHeader().optLong(Api.KEY_REGISTER_TIME, 0L);
    }

    public boolean saveRegisterInfo(
            final JSONObject resp,
            final String did,
            final String iid,
            final String ssid,
            final String bdDid,
            final String cd) {
        TLog.d(
                new TLog.LogGetter() {
                    @Override
                    public String log() {
                        return "saveRegisterInfo"
                                + ", "
                                + did
                                + ", "
                                + iid
                                + ", "
                                + ssid
                                + ", "
                                + bdDid
                                + ", "
                                + cd
                                + ", "
                                + resp;
                    }
                });
        mIsNewUser = resp.optInt(KEY_NEW_USER, 0) > 0;
        String deviceToken = resp.optString(Api.KEY_DEVICE_TOKEN, "");
        final boolean didChecked = Utils.checkId(did);
        final boolean iidChecked = Utils.checkId(iid);
        final boolean bdDidChecked = Utils.checkId(bdDid);
        final boolean cdChecked = Utils.checkId(cd);
        try {
            boolean ssidChecked = Utils.checkId(ssid);

            int lastVersionCode = mSp.getInt(Api.KEY_VERSION_CODE, 0);
            int currVersionCode = getConstHeader().optInt(Api.KEY_VERSION_CODE, 0);

            Editor editor = mSp.edit();
            if (lastVersionCode != currVersionCode) {
                editor.putInt(Api.KEY_VERSION_CODE, currVersionCode);
            }

            String lastChannel = mSp.getString(Api.KEY_CHANNEL, "");
            String curChannel = getConstHeader().optString(Api.KEY_CHANNEL, "");
            if (!TextUtils.equals(lastChannel, curChannel)) {
                editor.putString(Api.KEY_CHANNEL, curChannel);
            }

            editor.putString(Api.KEY_DEVICE_TOKEN, deviceToken);
            if ((didChecked || (bdDidChecked && cdChecked)) && iidChecked) {
                long curTime = System.currentTimeMillis();
                editor.putLong(Api.KEY_REGISTER_TIME, curTime);
                updateHeader(Api.KEY_REGISTER_TIME, curTime);
            } else if (!(didChecked || (bdDidChecked && cdChecked))) {
                JSONObject param = new JSONObject();
                param.put("response", resp);
                appLogInstance.onEventV3("tt_fetch_did_error", param);
            }

            boolean anyIdChanged = false;
            String oldDid = getProvider().getDeviceId();
            String oldBdDid = mSp.getString(Api.KEY_BD_DID, null);
            TLog.d("device: od=" + oldDid + " nd=" + did + " ck=" + didChecked);
            if (didChecked) {
                String didInHeader = mHeader.optString(Api.KEY_DEVICE_ID);
                if (!did.equals(didInHeader)) {
                    final JSONObject oldHeader = mHeader;
                    final JSONObject newHeader = new JSONObject();
                    Utils.copy(newHeader, oldHeader);
                    newHeader.put(Api.KEY_DEVICE_ID, did);
                    mHeader = newHeader;
                    getProvider().updateDeviceId(did);
                    anyIdChanged = true;
                }
                if (!did.equals(oldDid)) {
                    anyIdChanged = true;
                }
            }
            if (bdDidChecked && updateHeader(Api.KEY_BD_DID, bdDid)) {
                editor.putString(Api.KEY_BD_DID, bdDid);
                anyIdChanged = true;
            }

            String oldIid = getConstHeader().optString(Api.KEY_INSTALL_ID, "");
            if (iidChecked && updateHeader(Api.KEY_INSTALL_ID, iid)) {
                editor.putString(Api.KEY_INSTALL_ID, iid);
                anyIdChanged = true;
            }

            String oldSsid = getConstHeader().optString(Api.KEY_SSID, "");
            if (ssidChecked && setSsid(ssid)) {
                anyIdChanged = true;
            }
            if (null != appLogInstance.getDataObserverHolder()) {
                appLogInstance
                        .getDataObserverHolder()
                        .onRemoteIdGet(anyIdChanged, oldBdDid, bdDid, oldIid, iid, oldSsid, ssid);
            }
            editor.apply();
        } catch (JSONException e) {
            TLog.ysnp(e);
        }
        return (didChecked || (bdDidChecked && cdChecked)) && iidChecked;
    }

    public boolean isNewUser() {
        return mIsNewUser;
    }

    public boolean setAppLanguage(String language) {
        if (updateHeader(Api.KEY_APP_LANGUAGE, language)) {
            mConfig.setAppLanguage(language);
            return true;
        }
        return false;
    }

    public boolean setAppRegion(String region) {
        if (updateHeader(Api.KEY_APP_REGION, region)) {
            mConfig.setAppRegion(region);
            return true;
        }
        return false;
    }

    public void setGoogleAid(String gaid) {
        if (updateHeader(Api.KEY_GOOGLE_AID, gaid)) {
            mConfig.setGoogleAid(gaid);
        }
    }

    public <T> T getAbConfig(final String key, final T defaultValue) {
        JSONObject config = mConfig.getAbConfig();
        JSONObject jsonObject = config.optJSONObject(key);
        if (jsonObject != null) {
            // 拿到了，则该实验添加到已曝光区域
            String vid = jsonObject.optString("vid");
            Object value = jsonObject.opt("val");
            addExposedVid(vid);
            try {
                JSONObject params = new JSONObject();
                params.put(Api.KEY_AB_SDK_VERSION, vid);
                appLogInstance.onEventV3(Api.KEY_AB_TEST_EXPOSURE, params);
            } catch (JSONException e) {
                TLog.e(e);
            }
            return checkValue(value, defaultValue);
        } else {
            return defaultValue;
        }
    }

    private synchronized void addExposedVid(String vidToBeAdd) {
        String abVersion = getConstHeader().optString(Api.KEY_AB_SDK_VERSION);
        if (TextUtils.isEmpty(abVersion)) {
            abVersion = vidToBeAdd;
        } else {
            String[] vids = abVersion.split(",");
            for (String vid : vids) {
                if (!TextUtils.isEmpty(vid) && vid.equals(vidToBeAdd)) {
                    TLog.d("addExposedVid ready added: " + abVersion);
                    return;
                }
            }
            abVersion += ("," + vidToBeAdd);
        }
        setAbSdkVersion(abVersion);
        notifyVidsChange(abVersion, mConfig.getExternalAbVersion());
    }

    public String getAbSdkVersion() {
        if (mAllReady) {
            return getConstHeader().optString(Api.KEY_AB_SDK_VERSION, "");
        } else if (mConfig != null) {
            return mConfig.getAbSdkVersion();
        }
        return "";
    }

    private static <T> T checkValue(Object value, T defaultValue) {
        T result = null;
        if (value != null) {
            try {
                result = (T) value;
            } catch (Throwable t) {
                // ignore
            }
        }
        if (result == null) {
            result = defaultValue;
        }
        return result;
    }

    private void notifyVidsChange(String abSdkVersion, String externalAbVersion) {
        if (mConfig.isAbEnable() && mConfig.getInitConfig().isAbEnable()) {
            Set<String> abVidSet = getSetFromString(abSdkVersion);
            Set<String> externalVidSet = getSetFromString(externalAbVersion);
            abVidSet.removeAll(externalVidSet);
            if (null != appLogInstance.getDataObserverHolder()) {
                appLogInstance
                        .getDataObserverHolder()
                        .onAbVidsChange(getStringsFromSet(abVidSet), externalAbVersion);
            }
        }
    }

    public void clearAllAb() {
        setAbSdkVersion(null);
        setExternalAbVersion("");
        setAbConfig(null);
    }

    public int getLastVersionCode() {
        return mSp.getInt(Api.KEY_VERSION_CODE, 0);
    }
}
