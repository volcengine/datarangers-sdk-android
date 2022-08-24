// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.engine;

import com.bytedance.applog.Level;
import com.bytedance.applog.manager.ConfigManager;
import com.bytedance.applog.manager.DeviceManager;
import com.bytedance.applog.server.Api;
import com.bytedance.applog.util.EncryptUtils;
import com.bytedance.applog.util.TLog;
import com.bytedance.applog.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

class AbConfigure extends BaseWorker {

    private static final long INTERVAL_UPDATE_AB = 10 * 60 * 1000;

    AbConfigure(final Engine engine) {
        super(engine);
    }

    @Override
    protected boolean needNet() {
        return true;
    }

    @Override
    protected long nextInterval() {
        long abInterval = mEngine.getConfig().getAbInterval();
        if (abInterval < INTERVAL_UPDATE_AB) {
            abInterval = INTERVAL_UPDATE_AB;
        }
        return abInterval;
    }

    @Override
    protected long[] getRetryIntervals() {
        return Register.RETRY_SAME;
    }

    @Override
    protected boolean doWork() throws JSONException {

        final ConfigManager config = mEngine.getConfig();
        final DeviceManager device = mEngine.getDm();

        JSONObject header = device.getHeader();
        if (device.getRegisterState() != DeviceManager.STATE_EMPTY && header != null) {
            long current = System.currentTimeMillis();
            JSONObject headerInfo = new JSONObject();
            headerInfo.put(Api.KEY_HEADER, device.getHeader());
            headerInfo.put(Api.KEY_MAGIC, Api.MSG_MAGIC);
            headerInfo.put("_gen_time", current);
            String url =
                    appLogInstance
                            .getApiParamsUtil()
                            .appendNetParams(
                                    device.getHeader(),
                                    mEngine.getUriConfig().getAbUri(),
                                    true,
                                    Level.L1);
            final JSONObject response =
                    appLogInstance
                            .getApi()
                            .abConfig(
                                    Api.filterQuery(url, EncryptUtils.KEYS_CONFIG_QUERY),
                                    headerInfo);
            if (response != null) {
                // 从server端拉到了abconfig
                JSONObject oldConfig = config.getAbConfig();
                final boolean changed = !Utils.jsonEquals(oldConfig, response);
                TLog.d(
                        new TLog.LogGetter() {
                            @Override
                            public String log() {
                                return "getAbConfig (changed:" + changed + ") " + response;
                            }
                        });
                device.setAbConfig(response);
                if (null != appLogInstance.getDataObserverHolder()) {
                    appLogInstance.getDataObserverHolder().onRemoteAbConfigGet(changed, response);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    protected String getName() {
        return "AbConfigure";
    }
}
