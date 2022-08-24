// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.engine;

import com.bytedance.applog.AppLogHelper;
import com.bytedance.applog.Level;
import com.bytedance.applog.filter.AbstractEventFilter;
import com.bytedance.applog.manager.ConfigManager;
import com.bytedance.applog.manager.DeviceManager;
import com.bytedance.applog.server.Api;
import com.bytedance.applog.util.EncryptUtils;
import com.bytedance.applog.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author shiyanlong
 * @date 2019/2/2
 */
class Configure extends BaseWorker {

    Configure(final Engine engine) {
        super(engine, engine.getConfig().getConfigTs());
    }

    @Override
    protected boolean needNet() {
        return true;
    }

    @Override
    protected long nextInterval() {
        return mEngine.getConfig().getConfigInterval();
    }

    @Override
    protected long[] getRetryIntervals() {
        return Register.RETRY_SAME;
    }

    @Override
    public boolean doWork() throws JSONException {
        final DeviceManager device = mEngine.getDm();

        JSONObject header = device.getHeader();
        if (device.getRegisterState() != DeviceManager.STATE_EMPTY && header != null) {
            JSONObject request = new JSONObject();
            request.put(Api.KEY_MAGIC, Api.MSG_MAGIC);
            request.put(Api.KEY_HEADER, header);
            request.put("_gen_time", System.currentTimeMillis());
            if (mEngine.getConfig().getInitConfig().isEventFilterEnable()) {
                request.put(EncryptUtils.KEY_EVENT_FILTER, 1);
            }
            if (appLogInstance.getEncryptAndCompress()) {
                String[] keyAndIv = EncryptUtils.genRandomKeyAndIv();
                if (keyAndIv != null) {
                    request.put("key", keyAndIv[0]);
                    request.put("iv", keyAndIv[1]);
                }
            }

            String url =
                    appLogInstance
                            .getApiParamsUtil()
                            .appendNetParams(
                                    device.getHeader(),
                                    mEngine.getUriConfig().getSettingUri(),
                                    true,
                                    Level.L1);
            JSONObject config =
                    appLogInstance
                            .getApi()
                            .config(Api.filterQuery(url, EncryptUtils.KEYS_CONFIG_QUERY), request);

            final ConfigManager configManager = mEngine.getConfig();

            if (null != appLogInstance.getDataObserverHolder()) {
                appLogInstance
                        .getDataObserverHolder()
                        .onRemoteConfigGet(
                                !Utils.jsonEquals(config, configManager.getConfig()), config);
            }

            if (config != null) {
                configManager.setConfig(config);

                // 禁用monitor
                if (!configManager.isMonitorEnabled()) {
                    mEngine.disableMonitor();
                }

                mEngine.checkAbConfiger();
                if (mEngine.getConfig().getInitConfig().isEventFilterEnable()) {
                    String spName =
                            AppLogHelper.getInstanceSpName(
                                    appLogInstance, AbstractEventFilter.SP_FILTER_NAME);
                    mEngine.setEventFilter(
                            AbstractEventFilter.parseFilterFromServer(
                                    mEngine.getContext(), spName, config));
                }
                return true;
            }
        }
        return false;
    }

    @Override
    protected String getName() {
        return "Configure";
    }
}
