// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.engine;

import com.bytedance.applog.Level;
import com.bytedance.applog.log.LogInfo;
import com.bytedance.applog.manager.ConfigManager;
import com.bytedance.applog.manager.DeviceManager;
import com.bytedance.applog.network.RangersHttpTimeoutException;
import com.bytedance.applog.server.Api;
import com.bytedance.applog.util.EncryptUtils;
import com.bytedance.applog.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

class AbConfigure extends BaseWorker {

    private static final long INTERVAL_UPDATE_AB = 10 * 60 * 1000;
    private long lastFetchTime = 0;
    private JSONObject lastFetchResult = null;

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
        try {
            return null != fetchAbConfig(Api.HTTP_DEFAULT_TIMEOUT);
        } catch (Throwable e) {
            appLogInstance.getLogger().error(LogInfo.Category.ABTEST, "Do fetch config failed", e);
        }
        return false;
    }

    @Override
    protected String getName() {
        return "AbConfigure";
    }

    public synchronized JSONObject fetchAbConfig(int timeout) throws RangersHttpTimeoutException {
        final ConfigManager config = mEngine.getConfig();
        final DeviceManager device = mEngine.getDm();
        if (device.getRegisterState() != DeviceManager.STATE_EMPTY && device.getHeader() != null) {
            long current = System.currentTimeMillis();

            // 限流
            if (null != lastFetchResult
                    && current - lastFetchTime < mEngine.getPullAbTestConfigsThrottleMills()) {
                return lastFetchResult;
            }
            lastFetchTime = current;

            JSONObject headerInfo = new JSONObject();
            try {
                headerInfo.put(Api.KEY_HEADER, device.getHeader());
                headerInfo.put(Api.KEY_MAGIC, Api.MSG_MAGIC);
                headerInfo.put("_gen_time", current);
                EncryptUtils.putRandomKeyAndIvIntoRequest(appLogInstance, headerInfo);
            } catch (Throwable e) {
                appLogInstance.getLogger().error(LogInfo.Category.ABTEST, "Set header failed", e);
            }
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
                                    headerInfo,
                                    timeout);
            if (response != null) {
                lastFetchResult = response;
                // 从server端拉到了abconfig
                JSONObject oldConfig = config.getAbConfig();
                final boolean changed = !Utils.jsonEquals(oldConfig, response);

                appLogInstance
                        .getLogger()
                        .debug(LogInfo.Category.ABTEST, "getAbConfig changed:{}", changed);

                device.setAbConfig(response);
                if (null != appLogInstance.getDataObserverHolder()) {
                    appLogInstance.getDataObserverHolder().onRemoteAbConfigGet(changed, response);
                }
                return response;
            }
        }
        return null;
    }
}
