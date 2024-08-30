// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.engine;

import com.bytedance.applog.AppLogHelper;
import com.bytedance.applog.Level;
import com.bytedance.applog.filter.AbstractEventFilter;
import com.bytedance.applog.log.EventBus;
import com.bytedance.applog.log.LogUtils;
import com.bytedance.applog.manager.ConfigManager;
import com.bytedance.applog.manager.DeviceManager;
import com.bytedance.applog.server.Api;
import com.bytedance.applog.util.EncryptUtils;
import com.bytedance.applog.util.JsonUtils;
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
            JSONObject request = Api.buildRequestBody(header);
            if (mEngine.getConfig().getInitConfig().isEventFilterEnable()) {
                request.put(EncryptUtils.KEY_EVENT_FILTER, 1);
            }
            EncryptUtils.putRandomKeyAndIvIntoRequest(appLogInstance, request);

            String url =
                    appLogInstance
                            .getApiParamsUtil()
                            .appendNetParams(
                                    device.getHeader(),
                                    mEngine.getUriConfig().getSettingUri(),
                                    true,
                                    Level.L1);
            final JSONObject config =
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

                mEngine.checkAbConfiger();
                if (mEngine.getConfig().getInitConfig().isEventFilterEnable()) {
                    String spName =
                            AppLogHelper.getInstanceSpName(
                                    appLogInstance, AbstractEventFilter.SP_FILTER_NAME);
                    mEngine.setEventFilter(
                            AbstractEventFilter.parseFilterFromServer(
                                    mEngine.getContext(), spName, config));
                }
                if (!LogUtils.isDisabled()) {
                    LogUtils.sendJsonFetcher(
                            "fetch_log_settings_end",
                            new EventBus.DataFetcher() {
                                @Override
                                public Object fetch() {
                                    JSONObject data = new JSONObject();
                                    JsonUtils.mergeJsonObject(config, data);
                                    try {
                                        data.put("appId", appLogInstance.getAppId());
                                    } catch (Throwable ignored) {
                                    }
                                    return data;
                                }
                            });
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
