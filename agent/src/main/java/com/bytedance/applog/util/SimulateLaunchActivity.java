// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.TextView;

import com.bytedance.applog.AppLog;
import com.bytedance.applog.AppLogHelper;
import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.BuildConfig;
import com.bytedance.applog.IPageMeta;
import com.bytedance.applog.IPicker;
import com.bytedance.applog.R;
import com.bytedance.applog.annotation.PageMeta;
import com.bytedance.applog.server.Api;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

/**
 * 用于接收scheme的Activity, 模拟器圈选或埋点验证
 *
 * @author wuzhijun
 */
@PageMeta(path = "/simulateLaunch", title = "圈选/埋点验证")
public class SimulateLaunchActivity extends AppCompatActivity implements IPageMeta {

    private static final String TAG = "SimulateLaunchActivity";

    private static final String SYNC_QUERY = "sync_query";

    private static final String BIND_QUERY = "bind_query";

    private static final String DEBUG_LOG = "debug_log";

    public static final String KEY_QR_PARAM = "qr_param";

    private static final String KEY_URL_PREFIX = "url_prefix";

    private static final String KEY_URL_PREFIX_NO_QR = "url_prefix_no_qr";

    private static final String KEY_AID_NO_QR = "aid_no_qr";

    private Mode mCurrentMode = Mode.QR;

    private String mAid;

    private int mWidth;

    private int mHeight;

    private String mQrParam;

    private String mAppVersion;

    private String mDid;

    private String mType;

    private TextView mTextTip;

    private SimulateLoginTask mSimulateLoginTask;

    /**
     * 无二维码模式启动
     *
     * @param urlPrefix URL前缀
     */
    public static void startSimulatorWithoutQR(@NonNull Context context, String urlPrefix) {
        startSimulatorWithoutQR(context, AppLog.getAppId(), urlPrefix);
    }

    /**
     * 无二维码模式启动
     *
     * @param appId 应用ID
     * @param urlPrefix URL前缀
     */
    public static void startSimulatorWithoutQR(
            @NonNull Context context, String appId, String urlPrefix) {
        Intent intent = new Intent(context, SimulateLaunchActivity.class);
        intent.putExtra(KEY_URL_PREFIX_NO_QR, urlPrefix);
        intent.putExtra(KEY_AID_NO_QR, appId);
        context.startActivity(intent);
    }

    /**
     * 联调 pc scheme
     * rangersapplog.byAx6uYt://rangersapplog/picker?qr_param=H4sIAAAAAAAA_wTASw7CIBCA4bv861mAgsDcBhA1Rkxa-lg0vXu_g4xifXLxgbCgIOwo5W_7LZY6jefcv6lun_WN0FDrgzPBuHsQBvrKv9HOCwAA__8BAAD__5KmqOFJAAAA&time=1574070437&aid=159486&type=bind_query
     * 小球scheme
     * rangersapplog.byAx6uYt://rangesapplog/picker?qr_param=yyyy&time=1573194678&aid=159486&type=bind_query
     */
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(@android.support.annotation.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.applog_activity_simulate);
        mTextTip = findViewById(R.id.text_tip);

        Intent inputIntent = getIntent();
        Uri uri = inputIntent.getData();

        if (inputIntent.hasExtra(KEY_URL_PREFIX_NO_QR) && inputIntent.hasExtra(KEY_AID_NO_QR)) {
            // 无二维码方式
            mCurrentMode = Mode.NO_QR;
            final String urlPrefix = inputIntent.getStringExtra(KEY_URL_PREFIX_NO_QR);
            String appId = inputIntent.getStringExtra(KEY_AID_NO_QR);
            AppLogInstance appLogInstance = AppLogHelper.getInstanceByAppId(appId);
            if (null != appLogInstance) {
                if (!appLogInstance.hasStarted()) {
                    if (BuildConfig.IS_I18N) {
                        mTextTip.setText(
                                "Launch failed, please make sure AppLog has been "
                                        + "initialized and started.");
                    } else {
                        mTextTip.setText("启动失败,请按电脑提示检查原因然后重新扫码(AppLog未初始化)");
                    }
                    return;
                }
                appLogInstance.getApi().setSchemeHost(urlPrefix);
                fillExtraInfos(appLogInstance);
                mSimulateLoginTask = new SimulateLoginTask(appLogInstance);
                mSimulateLoginTask.execute();
            }
        } else if (uri != null) {
            // 使用二维码进到这里
            mCurrentMode = Mode.QR;
            String aid = uri.getQueryParameter("aid");
            AppLogInstance appLogInstance = AppLogHelper.getInstanceByAppId(aid);
            if (null != appLogInstance) {
                if (!appLogInstance.hasStarted()) {
                    if (BuildConfig.IS_I18N) {
                        mTextTip.setText(
                                "Launch failed: please make sure the AppLog has been "
                                        + "initialized and started.");
                    } else {
                        mTextTip.setText("启动失败：请按电脑提示检查原因然后重新扫码(AppLog未初始化)");
                    }
                    return;
                }
                mType = uri.getQueryParameter("type");
                if (!DEBUG_LOG.equals(mType) && !"picker".equals(BuildConfig.FLAVOR_function)) {
                    if (BuildConfig.IS_I18N) {
                        mTextTip.setText("Launch failed: type parameter mismatch.");
                    } else {
                        mTextTip.setText("启动失败：type参数错误");
                    }
                    return;
                }
                String urlPrefix = uri.getQueryParameter(KEY_URL_PREFIX);
                TLog.d("urlPrefix=" + urlPrefix);
                if (TextUtils.isEmpty(urlPrefix)) {
                    if (BuildConfig.IS_I18N) {
                        mTextTip.setText("Launch failed: url_prefix parameter not provided.");
                    } else {
                        mTextTip.setText("启动失败：无url_prefix参数");
                    }
                    return;
                }
                appLogInstance.getApi().setSchemeHost(urlPrefix);
                mQrParam = uri.getQueryParameter(KEY_QR_PARAM);
                fillExtraInfos(appLogInstance);
                mSimulateLoginTask = new SimulateLoginTask(appLogInstance);
                mSimulateLoginTask.execute();
            } else {
                if (BuildConfig.IS_I18N) {
                    mTextTip.setText(
                            "Launch failed: possible aid mismatch or the AppLog has not been initialized.");
                } else {
                    mTextTip.setText("启动失败：请按电脑提示检查原因然后重新扫码(aid错误或AppLog未初始化)");
                }
            }
        }
    }

    private void fillExtraInfos(AppLogInstance appLogInstance) {
        String resolution = appLogInstance.getHeaderValue(Api.KEY_RESOLUTION, null, String.class);
        if (!TextUtils.isEmpty(resolution)) {
            String[] hw = Objects.requireNonNull(resolution).split("x");
            mHeight = Integer.parseInt(hw[0]);
            mWidth = Integer.parseInt(hw[1]);
        }
        mAid = appLogInstance.getAppId();
        mDid = appLogInstance.getDid();
        String packageName = getApplicationInfo().packageName;
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(packageName, 0);
            mAppVersion = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            mAppVersion = "1.0.0";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mSimulateLoginTask != null) {
            mSimulateLoginTask.cancel(true);
            mSimulateLoginTask = null;
        }
    }

    @Override
    public JSONObject pageProperties() {
        try {
            return new JSONObject().put("class_name", "SimulateLaunchActivity");
        } catch (JSONException e) {
            TLog.ysnp(e);
        }
        return null;
    }

    @Override
    public String title() {
        return "圈选/埋点验证";
    }

    @Override
    public String path() {
        return "/simulateLaunch";
    }

    private class SimulateLoginTask extends AsyncTask<Void, Void, JSONObject> {

        private final AppLogInstance appLogInstance;

        public SimulateLoginTask(AppLogInstance instance) {
            this.appLogInstance = instance;
        }

        @Override
        protected JSONObject doInBackground(Void... voids) {
            if (mCurrentMode == Mode.QR) {
                return appLogInstance
                        .getApi()
                        .simulateLogin(mAid, mAppVersion, mWidth, mHeight, mDid, mQrParam);
            } else {
                return appLogInstance
                        .getApi()
                        .simulateLoginWithoutQR(this, mAid, mAppVersion, mWidth, mHeight, mDid);
            }
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            if (jsonObject != null) {
                String message = jsonObject.optString("message");
                String cookie = jsonObject.optString(Api.KEY_SET_COOKIE);
                int status = jsonObject.optInt("status");
                if (cookie != null) {
                    int end = cookie.indexOf(";");
                    if (end >= 0) {
                        cookie = cookie.substring(0, end);
                    }
                }
                if (mCurrentMode == Mode.NO_QR) {
                    JSONObject data = jsonObject.optJSONObject("data");
                    if (data != null) {
                        mType = data.optString("mode", "").equals("log") ? DEBUG_LOG : BIND_QUERY;
                    }
                }
                if (DEBUG_LOG.equals(mType) && status == 0 && !TextUtils.isEmpty(cookie)) {
                    appLogInstance.setRangersEventVerifyEnable(true, cookie);
                    String packageName = getApplicationInfo().packageName;
                    PackageManager packageManager = getPackageManager();
                    Intent intent = packageManager.getLaunchIntentForPackage(packageName);
                    if (intent != null) {
                        intent.setPackage(null);
                        startActivity(intent);
                        finish();
                    }
                } else if ("OK".equals(message) && !TextUtils.isEmpty(cookie)) {
                    String packageName = getApplicationInfo().packageName;
                    PackageManager packageManager = getPackageManager();
                    Intent intent = packageManager.getLaunchIntentForPackage(packageName);
                    if (intent != null) {
                        intent.setPackage(null);
                        startActivity(intent);
                        IPicker picker = null;
                        if (appLogInstance.getInitConfig() != null
                                && appLogInstance.getInitConfig().getPicker() != null) {
                            picker = appLogInstance.getInitConfig().getPicker();
                        }
                        if (picker != null) {
                            picker.setMarqueeCookie(cookie);
                        }
                        appLogInstance.startSimulator(cookie);
                        finish();
                    }
                } else {
                    mTextTip.setText("启动失败,请按电脑提示检查原因然后重新扫码(" + jsonObject.toString() + ")");
                }
            } else {
                mTextTip.setText("启动失败,请按电脑提示检查原因然后重新扫码(response is null)");
            }
        }
    }

    enum Mode {
        QR,
        NO_QR
    }
}
