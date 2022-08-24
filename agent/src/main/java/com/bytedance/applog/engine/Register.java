// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.engine;

import androidx.annotation.NonNull;

import com.bytedance.applog.Level;
import com.bytedance.applog.manager.ConfigManager;
import com.bytedance.applog.manager.DeviceManager;
import com.bytedance.applog.server.Api;
import com.bytedance.applog.util.RequestIdGenerator;
import com.bytedance.applog.util.TLog;
import com.bytedance.applog.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.Set;

/**
 * @author shiyanlong
 * @date 2019/2/2
 */
class Register extends BaseWorker {
    private static final String KEY_PRE_INSTALL_CHANNEL = "pre_installed_channel";
    private static final String KEY_FIRST_INSTALL_TIME = "apk_first_install_time";
    private static final String KEY_IS_SYSTEM_APP = "is_system_app";

    static final int REFRESH_UI = 6 * 60 * 60 * 1000;

    static final int REFRESH_BG = 12 * 60 * 60 * 1000;

    /**
     * 要注册的uuid
     */
    private String toRegisterUuid = null;

    static final long[] RETRY_DIFF =
            new long[]{
                    60 * 1000,
                    60 * 1000,
                    60 * 1000,
                    2 * 60 * 1000,
                    2 * 60 * 1000,
                    3 * 60 * 1000,
                    3 * 60 * 1000,
                    6 * 60 * 1000,
                    6 * 60 * 1000,
                    9 * 60 * 1000,
                    9 * 60 * 1000
            };

    static final long[] RETRY_SAME =
            new long[]{
                    3 * 60 * 1000,
                    3 * 60 * 1000,
                    6 * 60 * 1000,
                    6 * 60 * 1000,
                    9 * 60 * 1000,
                    9 * 60 * 1000,
                    12 * 60 * 1000,
                    12 * 60 * 1000
            };

    private static final long[] RETRY_EMPTY =
            new long[]{
                    10 * 1000,
                    10 * 1000,
                    20 * 1000,
                    20 * 1000,
                    60 * 1000,
                    60 * 1000,
                    2 * 60 * 1000,
                    2 * 60 * 1000,
                    3 * 60 * 1000,
                    3 * 60 * 1000,
                    6 * 60 * 1000,
                    6 * 60 * 1000,
                    9 * 60 * 1000,
                    9 * 60 * 1000
            };

    Register(final Engine engine) {
        super(engine, engine.getDm().getLastRegisterTime());
    }

    @Override
    protected boolean needNet() {
        return true;
    }

    @Override
    protected long nextInterval() {
        return mEngine.getSession().hadUi() ? REFRESH_UI : REFRESH_BG;
    }

    @Override
    protected long[] getRetryIntervals() {
        long[] retry;
        switch (mEngine.getDm().getRegisterState()) {
            case DeviceManager.STATE_EMPTY:
                retry = RETRY_EMPTY;
                break;
            case DeviceManager.STATE_SAME:
                retry = RETRY_SAME;
                break;
            case DeviceManager.STATE_DIFF:
                retry = RETRY_DIFF;
                break;
            default:
                TLog.ysnp(null);
                retry = RETRY_SAME;
        }
        return retry;
    }

    @Override
    protected boolean doWork() throws JSONException {
        JSONObject newHeader = new JSONObject();
        Utils.copy(newHeader, mEngine.getDm().getHeader());
        return doRegister(newHeader);
    }

    public boolean doRegister(@NonNull JSONObject header) throws JSONException {
        TLog.d(getName() + " start work");

        // 注册的新的uuid
        final String newUuid = header.optString(Api.KEY_USER_UNIQUE_ID);

        // 记录正在注册的uuid
        toRegisterUuid = newUuid;

        final DeviceManager device = mEngine.getDm();
        final ConfigManager config = mEngine.getConfig();

        Map<String, Object> commonHeader = config.getInitConfig().getCommonHeader();

        header.put(Api.REQ_ID, RequestIdGenerator.getRequestId());

        if (commonHeader != null) {
            Set<Map.Entry<String, Object>> entries = commonHeader.entrySet();
            for (Map.Entry<String, Object> entry : entries) {
                if (entry.getValue() != null) {
                    header.put(entry.getKey(), entry.getValue());
                }
            }
        }
        JSONObject response = invokeRegister(header);
        if (response != null) {
            String deviceId = response.optString(Api.KEY_DEVICE_ID, "");
            String installId = response.optString(Api.KEY_INSTALL_ID, "");
            String ssid = response.optString(Api.KEY_SSID, "");
            String bdDid = response.optString(Api.KEY_BD_DID, "");
            String cd = response.optString(Api.KEY_CD, "");

            if (Utils.isNotEmpty(ssid)) {
                // 更新数据库中所有无ssid的uuid对应的数据
                mEngine.getDbStoreV2().updateSsid2Uuid(newUuid, ssid);
            }

            boolean save = device.saveRegisterInfo(response, deviceId, installId, ssid, bdDid, cd);
            if (save) {
                mEngine.workAbConfiger();
            }

            return save;
        } else {
            // fallback
            toRegisterUuid = appLogInstance.getUserUniqueID();
        }
        TLog.d(getName() + " work finished");
        return false;
    }

    /**
     * 触发注册
     *
     * @param header header信息
     * @return JSONObject
     */
    public JSONObject invokeRegister(@NonNull JSONObject header) {
        TLog.d("Start to invokeRegister");
        try {
            JSONObject request = new JSONObject();
            request.put(Api.KEY_HEADER, header);
            request.put(Api.KEY_MAGIC, Api.MSG_MAGIC);
            request.put("_gen_time", System.currentTimeMillis());
            String url =
                    appLogInstance
                            .getApiParamsUtil()
                            .appendNetParams(
                                    header,
                                    mEngine.getUriConfig().getRegisterUri(),
                                    true,
                                    Level.L1);
            return appLogInstance.getApi().register(url, request);
        } catch (Throwable e) {
            TLog.ysnp(e);
        }
        return null;
    }

    public JSONObject invokeReportOaid(@NonNull JSONObject header) {
        try {
            JSONObject request = new JSONObject();
            request.put(Api.KEY_HEADER, header);
            request.put(Api.KEY_MAGIC, Api.MSG_MAGIC);
            request.put("_gen_time", System.currentTimeMillis());
            String url = mEngine.getUriConfig().getReportOaidUri();
            return appLogInstance.getApi().reportOaid(url, request);
        } catch (Throwable e) {
            TLog.ysnp(e);
        }
        return null;
    }

    @Override
    protected String getName() {
        return "register";
    }

    /**
     * 读取正在注册的uuid
     *
     * @return pendingSsidUuid
     */
    public String getToRegisterUuid() {
        return toRegisterUuid;
    }
}
