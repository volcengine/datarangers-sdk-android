// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.engine;

import android.os.Bundle;

import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.IHeaderCustomTimelyCallback;
import com.bytedance.applog.manager.ConfigManager;
import com.bytedance.applog.manager.DeviceManager;
import com.bytedance.applog.network.CongestionController;
import com.bytedance.applog.store.DbStoreV2;
import com.bytedance.applog.store.PackV2;
import com.bytedance.applog.util.TLog;
import com.bytedance.applog.util.Utils;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.List;

/**
 * @author shiyanlong
 * @date 2019/2/2
 */
class Sender extends BaseWorker {

    private static final long INTERVAL_PACK = 10 * 1000;

    private static final long INTERVAL_PLAY = 50 * 1000;

    private static final long[] RETRY_INTERVALS = new long[] {INTERVAL_PACK};

    private final CongestionController mCongestionController;

    Sender(final Engine engine) {
        super(engine);
        mCongestionController = new CongestionController("sender_", engine.getConfig());
    }

    @Override
    protected boolean needNet() {
        return true;
    }

    @Override
    protected long nextInterval() {
        return mEngine.getConfig().getEventInterval();
    }

    @Override
    protected long[] getRetryIntervals() {
        return RETRY_INTERVALS;
    }

    @Override
    public boolean doWork() {
        final long currentTs = System.currentTimeMillis();
        final Session session = mEngine.getSession();
        if (session != null) {
            final Bundle playBundle = session.getPlayBundle(currentTs, INTERVAL_PLAY);
            if (playBundle != null) {
                appLogInstance.onEventV3("play_session", playBundle, AppLogInstance.BUSINESS_EVENT);
                appLogInstance.flush();
            }
        }

        final DeviceManager device = mEngine.getDm();
        boolean done = false;
        if (device.getRegisterState() != DeviceManager.STATE_EMPTY) {
            device.updateNetworkAccessType();
            JSONObject newHeader = Utils.transferHeaderOaid(device.getHeader());
            if (newHeader != null) {
                IHeaderCustomTimelyCallback callback = appLogInstance.getHeaderCustomCallback();
                if (callback != null) {
                    callback.updateHeader(newHeader);
                }
                send(newHeader);
                done = true;
            } else {
                TLog.ysnp(null);
            }
        }
        return done;
    }

    private void send(JSONObject newHeader) {
        final DbStoreV2 dbStore = mEngine.getDbStoreV2();
        final String appId = appLogInstance.getAppId();
        dbStore.pack(appId, newHeader);

        if (!mCongestionController.isCanSend()) {
            return;
        }

        final ConfigManager mConfig = mEngine.getConfig();
        final DeviceManager device = mEngine.getDm();

        List<PackV2> packs = dbStore.queryPacks(appId);

        int successCount = 0;
        if (packs.size() > 0) {
            for (PackV2 pack : packs) {
                if (pack.data == null || pack.data.length <= 0) {
                    pack.fail = 0;
                    successCount++;
                    continue;
                }
                String[] uris =
                        appLogInstance
                                .getApiParamsUtil()
                                .getSendLogUris(mEngine, device.getHeader(), pack.eventType);
                byte[] data = appLogInstance.getApi().getEncryptUtils().encrypt(pack.data);
                int responseCode = appLogInstance.getApi().send(uris, data, mConfig);
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    mCongestionController.handleSuccess();
                    pack.fail = 0;
                    successCount++;
                } else {
                    mCongestionController.handleException();
                    pack.fail += 1;
                }
            }

            // 发送后的处理
            dbStore.doAfterPackSend(packs);

            TLog.d(
                    getName()
                            + " successfully send "
                            + successCount
                            + " packs (total: "
                            + packs.size()
                            + ")");
        }
    }

    @Override
    protected String getName() {
        return "sender";
    }
}
