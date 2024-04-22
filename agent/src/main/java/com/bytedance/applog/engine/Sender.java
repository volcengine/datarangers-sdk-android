// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.engine;

import android.os.Bundle;

import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.log.EventBus;
import com.bytedance.applog.log.LogInfo;
import com.bytedance.applog.log.LogUtils;
import com.bytedance.applog.manager.DeviceManager;
import com.bytedance.applog.network.CongestionController;
import com.bytedance.applog.server.Api;
import com.bytedance.applog.store.DbStoreV2;
import com.bytedance.applog.store.PackV2;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Set;

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
                mEngine.getAppLog()
                        .getLogger()
                        .debug(LogInfo.Category.EVENT, "New play session event");

                appLogInstance.onEventV3("play_session", playBundle, AppLogInstance.BUSINESS_EVENT);
                appLogInstance.flush();
            }
        }

        final DeviceManager device = mEngine.getDm();
        boolean done = false;
        if (device.getRegisterState() != DeviceManager.STATE_EMPTY) {
            device.updateNetworkAccessType(mEngine.isResume());
            JSONObject newHeader = device.getHeader();
            if (newHeader != null) {
                send(newHeader);
                done = true;
            } else {
                mEngine.getAppLog().getLogger().error(LogInfo.Category.EVENT, "Header is empty");
            }
        }
        return done;
    }

    private void send(JSONObject newHeader) {
        mEngine.getAppLog()
                .getLogger()
                .debug(LogInfo.Category.EVENT, "Send events with header:{}", newHeader);

        final DbStoreV2 dbStore = mEngine.getDbStoreV2();
        final String appId = appLogInstance.getAppId();

        if (!mCongestionController.isCanSend()) {
            return;
        }

        //  之前打包先查询一次有多少遗留 Pack 数据
        int packCount = dbStore.queryPackCount(appId);

        if (packCount < DbStoreV2.LIMIT_SELECT_PACK) {
            int canPackCount = DbStoreV2.LIMIT_SELECT_PACK - packCount;
            for (int i = 0; i < canPackCount; i++) {
                //  如果发现没数据了 就不再继续读取
                if (!dbStore.pack(appId, newHeader)) break;
            }
        }
        List<PackV2> packs = dbStore.queryPacks(appId);

        mEngine.getAppLog()
                .getLogger()
                .debug(LogInfo.Category.EVENT, "{} packs to be sent", packs.size());

        int successCount = 0;
        if (packs.size() > 0) {
            for (PackV2 pack : packs) {
                if (pack.data == null || pack.data.length <= 0) {
                    pack.fail = 0;
                    successCount++;
                    continue;
                }
                if (singlePackSend(pack)) {
                    successCount++;
                }
            }

            // 发送后的处理
            dbStore.doAfterPackSend(packs);

            mEngine.getAppLog()
                    .getLogger()
                    .debug(
                            LogInfo.Category.EVENT,
                            getName()
                                    + " successfully send "
                                    + successCount
                                    + " packs (total: "
                                    + packs.size()
                                    + ")");
        }
    }

    public boolean singlePackSend(PackV2 pack) {
        JSONObject packJson;
        String[] uris =
                appLogInstance
                        .getApiParamsUtil()
                        .getSendLogUris(mEngine, mEngine.getDm().getHeader(), pack.eventType);
        boolean isSuccess = false;
        try {
            packJson = new JSONObject(new String(pack.data));
            packJson.put(Api.KEY_LOCAL_TIME, System.currentTimeMillis() / 1000);
            int responseCode = appLogInstance.getApi().sendLog(uris, packJson, mEngine.getConfig());
            if (responseCode == HttpURLConnection.HTTP_OK) {
                mCongestionController.handleSuccess();
                pack.fail = 0;
                isSuccess = true;
                sendPackUpload2Devtools(pack.getEventLocalIds(), true);
            } else {
                // 对齐内部 / iOS 只有 5xx 的服务端错误才开启拥塞控制
                int errorType;
                if (Api.checkIfJamMsg(responseCode)) {
                    mCongestionController.handleException();
                }
                errorType = responseCode;
                mEngine.getAppLog()
                        .getLogger()
                        .error(LogInfo.Category.EVENT, "Send pack failed:{}", responseCode);
                pack.fail += 1;
                sendPackUpload2Devtools(pack.getEventLocalIds(), false);
            }
        } catch (Throwable e) {
            mEngine.getAppLog()
                    .getLogger()
                    .error(LogInfo.Category.EVENT, "Send pack failed", e);
            sendPackUpload2Devtools(pack.getEventLocalIds(), false);
        }
        return isSuccess;
    }

    @Override
    protected String getName() {
        return "sender";
    }

    /**
     * 发送上报状态到devtools
     *
     * @param success 是否上报成功
     */
    private void sendPackUpload2Devtools(final Set<String> eventIds, final boolean success) {
        if (null == eventIds || eventIds.isEmpty()) {
            return;
        }
        LogUtils.sendJsonFetcher(
                "event_upload_eid",
                new EventBus.DataFetcher() {
                    @Override
                    public Object fetch() {
                        JSONObject data = new JSONObject();
                        try {
                            data.put("$$APP_ID", appLogInstance.getAppId());
                            JSONArray idArray = new JSONArray();
                            for (String id : eventIds) {
                                idArray.put(id);
                            }
                            data.put("$$EVENT_LOCAL_ID_ARRAY", idArray);
                            data.put("$$UPLOAD_STATUS", success ? "success" : "failed");
                        } catch (JSONException ignored) {

                        }
                        return data;
                    }
                });
    }
}
