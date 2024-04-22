// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.simulate;

import static com.bytedance.applog.simulate.SimulateLaunchActivity.BIND_QUERY;
import static com.bytedance.applog.simulate.SimulateLaunchActivity.DEBUG_LOG;

import android.content.pm.PackageInfo;
import android.os.AsyncTask;
import android.widget.Toast;

import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.BuildConfig;
import com.bytedance.applog.IPicker;
import com.bytedance.applog.server.Api;
import com.bytedance.applog.util.PackageUtils;
import com.bytedance.applog.util.Utils;

import org.json.JSONObject;

import java.util.Collections;

/**
 * 实时埋点验证|圈选的登陆和启动
 *
 * @author luodong.seu
 */
public class SimulateLoginTask extends AsyncTask<Void, Void, JSONObject> {
    private int mWidth;
    private int mHeight;
    private String mQrParam;
    private String mAppVersion;
    private String mDid;
    private String mType;
    private final AppLogInstance appLogInstance;

    /**
     * 开启任务，如果存在相同的任务则过滤
     *
     * @param instance AppLogInstance
     */
    public static void start(AppLogInstance instance) {
        new SimulateLoginTask(instance).execute();
    }

    private SimulateLoginTask(AppLogInstance instance) {
        this.appLogInstance = instance;
        init();
    }

    private void init() {
        appLogInstance.getApi().setSchemeHost(SimulateLaunchActivity.entryUrlPrefix);

        mType = SimulateLaunchActivity.entryType;
        mQrParam = SimulateLaunchActivity.entryQrParam;
        mDid = appLogInstance.getDid();

        String resolution = appLogInstance.getHeaderValue(Api.KEY_RESOLUTION, null, String.class);
        if (Utils.isNotEmpty(resolution)) {
            String[] hw = resolution.split("x");
            mHeight = Integer.parseInt(hw[0]);
            mWidth = Integer.parseInt(hw[1]);
        }

        String packageName = appLogInstance.getContext().getApplicationInfo().packageName;
        PackageInfo packageInfo =
                PackageUtils.getPackageInfo(appLogInstance.getContext(), packageName);
        mAppVersion = null != packageInfo ? packageInfo.versionName : "1.0.0";

        appLogInstance
                .getLogger()
                .debug(
                        Collections.singletonList("SimulateLoginTask"),
                        "Simulate task init success");
    }

    @Override
    protected JSONObject doInBackground(Void... voids) {
        if (SimulateLaunchActivity.entryMode == SimulateLaunchActivity.MODE_QR) {
            return appLogInstance
                    .getApi()
                    .simulateLogin(
                            appLogInstance.getAppId(),
                            mAppVersion,
                            mWidth,
                            mHeight,
                            mDid,
                            mQrParam);
        } else {
            return appLogInstance
                    .getApi()
                    .simulateLoginWithoutQR(
                            this, appLogInstance.getAppId(), mAppVersion, mWidth, mHeight, mDid);
        }
    }

    @Override
    protected void onPostExecute(JSONObject jsonObject) {
        appLogInstance
                .getLogger()
                .debug(
                        Collections.singletonList("SimulateLoginTask"),
                        "Simulate login with response: {}",
                        jsonObject);

        if (null == jsonObject) {
            if (BuildConfig.IS_I18N) {
                Toast.makeText(
                                appLogInstance.getContext(),
                                "Launch event verification failed for server no response",
                                Toast.LENGTH_LONG)
                        .show();
            } else {
                Toast.makeText(appLogInstance.getContext(), "启动埋点验证|圈选失败，服务端无响应", Toast.LENGTH_LONG)
                        .show();
            }
            return;
        }

        String message = jsonObject.optString("message");
        String cookie = jsonObject.optString(Api.KEY_SET_COOKIE);
        int status = jsonObject.optInt("status");
        if (SimulateLaunchActivity.entryMode == SimulateLaunchActivity.MODE_NO_QR) {
            JSONObject data = jsonObject.optJSONObject("data");
            if (data != null) {
                mType = data.optString("mode", "").equals("log") ? DEBUG_LOG : BIND_QUERY;
            }
        }
        if (status == 0 && "OK".equals(message)) {
            if (DEBUG_LOG.equals(mType)) {
                // 实时埋点验证
                appLogInstance.setRangersEventVerifyEnable(true, cookie);
            } else {
                IPicker picker = null;
                if (appLogInstance.getInitConfig() != null
                        && appLogInstance.getInitConfig().getPicker() != null) {
                    picker = appLogInstance.getInitConfig().getPicker();
                }
                if (picker != null) {
                    picker.setMarqueeCookie(cookie);
                }
                appLogInstance.startSimulator(cookie);
            }
        } else if (status != 0 && Utils.isNotEmpty(jsonObject.optString("message"))) {
            if (BuildConfig.IS_I18N) {
                Toast.makeText(
                                appLogInstance.getContext(),
                                "Launch event verify failed: " + jsonObject.optString("message"),
                                Toast.LENGTH_LONG)
                        .show();
            } else {
                Toast.makeText(
                                appLogInstance.getContext(),
                                "启动埋点验证|圈选失败: " + jsonObject.optString("message"),
                                Toast.LENGTH_LONG)
                        .show();
            }
        } else {
            appLogInstance
                    .getLogger()
                    .warn(
                            Collections.singletonList("SimulateLoginTask"),
                            "Start simulator failed, please check server response: {}",
                            jsonObject);
        }
    }
}
