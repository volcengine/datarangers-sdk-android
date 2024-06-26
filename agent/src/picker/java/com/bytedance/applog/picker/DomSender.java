// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.picker;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import com.bytedance.applog.engine.BaseWorker;
import com.bytedance.applog.engine.Engine;
import com.bytedance.applog.util.Utils;
import com.bytedance.applog.util.ViewUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;

public class DomSender extends BaseWorker
        implements Handler.Callback, CircleHelper.CircleInfoListener {

    private static final long INTERVAL_SEND_DOM = 1000;
    private static final int MSG_UPLOAD_DOM = 1;
    private static final long[] DEFAULT_RETRY_INTERVALS = new long[] {INTERVAL_SEND_DOM};

    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper(), this);
    private final Handler network;

    private int mWidth;
    private int mHeight;
    private final Context mApp;
    private final String mAid;
    private final PickerApi pickerApi;
    private final String mAppVersion;
    private final String mCookie;
    private final CircleHelper circleHelper;

    public DomSender(final Engine engine, final String cookie) {
        super(engine);
        // picker api
        HandlerThread networkThread = new HandlerThread("dom_work");
        networkThread.start();
        network = new Handler(networkThread.getLooper(), this);
        pickerApi = new PickerApi(appLogInstance);
        circleHelper = new CircleHelper(appLogInstance, this, Looper.myLooper());
        mApp = engine.getContext();
        mAid = engine.getDm().getAid();
        mAppVersion = engine.getDm().getVersionName();
        String resolution =
                appLogInstance.getHeaderValue(
                        com.bytedance.applog.server.Api.KEY_RESOLUTION, null, String.class);
        if (Utils.isNotEmpty(resolution)) {
            String[] hw = resolution.split("x");
            mHeight = Integer.parseInt(hw[0]);
            mWidth = Integer.parseInt(hw[1]);
        }
        mCookie = cookie;
    }

    @Override
    protected boolean needNet() {
        return true;
    }

    @Override
    protected long nextInterval() {
        return INTERVAL_SEND_DOM;
    }

    @Override
    protected long[] getRetryIntervals() {
        return DEFAULT_RETRY_INTERVALS;
    }

    @Override
    protected boolean doWork() {
        circleHelper.getCircleInfo();
        return true;
    }

    @Override
    protected String getName() {
        return "d";
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == MSG_UPLOAD_DOM) {
            LinkedList<DomPageModel> domPageModelList = (LinkedList<DomPageModel>) msg.obj;
            JSONObject result =
                    pickerApi.uploadDom(
                            appLogInstance.getApi().getSchemeHost(),
                            mAid,
                            mAppVersion,
                            mCookie,
                            domPageModelList);
            if (result != null) {
                JSONObject data = result.optJSONObject("data");
                if (data != null) {
                    boolean keep = data.optBoolean("keep", true);
                    if (!keep) {
                        String errorMsg = result.optString("message");
                        Message message = mMainThreadHandler.obtainMessage();
                        message.obj = errorMsg;
                        mMainThreadHandler.sendMessage(message);
                        setStop(true);
                    }
                }
            }
        } else {
            String errorMsg = (String) msg.obj;
            Toast.makeText(mApp, errorMsg, Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    @Override
    public void onGetCircleInfoFinish(Map<Integer, CircleHelper.CircleInfo> circleInfo) {
        if (circleInfo == null) {
            return;
        }
        LinkedList<DomPageModel> domPageModelList = new LinkedList<>();
        DomPageModel domPageModel = new DomPageModel();
        domPageModel.height = mHeight;
        domPageModel.width = mWidth;
        domPageModelList.add(domPageModel); // 第一个为主显示屏
        for (Integer key : circleInfo.keySet()) {
            CircleHelper.CircleInfo info = circleInfo.get(key);
            if (null == info || null == info.rootCircle) {
                continue;
            }
            if (ViewUtils.isMainDisplay(appLogInstance.getContext(), key)) {
                domPageModel = domPageModelList.getFirst();
            } else {
                domPageModel = new DomPageModel();
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        DisplayManager displayManager =
                                (DisplayManager) mApp.getSystemService(Context.DISPLAY_SERVICE);
                        domPageModel.height = displayManager.getDisplay(key).getHeight();
                        domPageModel.width = displayManager.getDisplay(key).getWidth();
                    }
                } catch (Throwable e) {
                    appLogInstance
                            .getLogger()
                            .error(
                                    Collections.singletonList("DomSender"),
                                    "Get display pixels failed",
                                    e);
                }
                domPageModelList.add(domPageModel);
            }
            domPageModel.domPagerArray =
                    DomPagerHelper.getDomPagerArray(info.rootCircle, info.getWebInfoList());
            domPageModel.shotBase64 = DomPagerHelper.getShotBase64(key);
        }
        network.obtainMessage(MSG_UPLOAD_DOM, domPageModelList).sendToTarget();
    }

    @Override
    public void onGetCircleInfoFinish(int displayId, JSONArray domPagerArray) {
        if (domPagerArray == null || domPagerArray.length() < 1) {
            return;
        }
        LinkedList<DomPageModel> domPageModelList = new LinkedList<>();
        DomPageModel domPageModel = new DomPageModel();
        domPageModel.width = mWidth;
        domPageModel.height = mHeight;
        domPageModel.domPagerArray = domPagerArray;
        domPageModel.shotBase64 = DomPagerHelper.getShotBase64(displayId);
        domPageModelList.add(domPageModel);
        JSONObject result =
                pickerApi.uploadDom(
                        appLogInstance.getApi().getSchemeHost(),
                        mAid,
                        mAppVersion,
                        mCookie,
                        domPageModelList);
        if (result != null) {
            JSONObject data = result.optJSONObject("data");
            if (data != null) {
                boolean keep = data.optBoolean("keep", true);
                if (!keep) {
                    String errorMsg = result.optString("message");
                    Message message = mMainThreadHandler.obtainMessage();
                    message.obj = errorMsg;
                    mMainThreadHandler.sendMessage(message);
                    setStop(true);
                }
            }
        }
    }

    static class DomPageModel {
        String shotBase64;
        JSONArray domPagerArray = new JSONArray();
        int width;
        int height;
    }
}
