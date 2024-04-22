// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.simulate;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.ArrayMap;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bytedance.applog.AppLog;
import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.AppLogManager;
import com.bytedance.applog.BuildConfig;
import com.bytedance.applog.IAppLogInstance;
import com.bytedance.applog.IPageMeta;
import com.bytedance.applog.R;
import com.bytedance.applog.log.IAppLogLogger;
import com.bytedance.applog.log.LoggerImpl;
import com.bytedance.applog.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 用于接收scheme的Activity, 模拟器圈选或埋点验证
 *
 * @author wuzhijun
 */
public class SimulateLaunchActivity extends Activity implements IPageMeta {
    private static final String TAG = "SimulateLaunchActivity";
    public static final String KEY_URL_PREFIX = "url_prefix";
    public static final String KEY_URL_PREFIX_NO_QR = "url_prefix_no_qr";
    public static final String DEBUG_LOG = "debug_log";
    public static final String BIND_QUERY = "bind_query";

    private static final String KEY_AID_NO_QR = "aid_no_qr";
    public static final int MODE_QR = 0;
    public static final int MODE_NO_QR = 1;
    private TextView mTextTip;

    /** 入口的APPID */
    public static String entryAppId = "";

    /** 入口的mode */
    public static int entryMode = MODE_QR;

    /** 入口的urlPrefix */
    public static String entryUrlPrefix = "";

    /** 入口的参数 */
    public static String entryQrParam = "";

    /** 入口类型 */
    public static String entryType = "";

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
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.applog_activity_simulate);
        mTextTip = findViewById(R.id.text_tip);

        Intent inputIntent = getIntent();
        Uri uri = inputIntent.getData();

        if (inputIntent.hasExtra(KEY_URL_PREFIX_NO_QR) && inputIntent.hasExtra(KEY_AID_NO_QR)) {
            // 无二维码方式
            entryMode = MODE_NO_QR;
            entryUrlPrefix = inputIntent.getStringExtra(KEY_URL_PREFIX_NO_QR);
            entryAppId = inputIntent.getStringExtra(KEY_AID_NO_QR);
        } else if (uri != null) {
            // 使用二维码进到这里
            entryMode = MODE_QR;
            entryAppId = uri.getQueryParameter("aid");
            entryQrParam = uri.getQueryParameter("qr_param");
            entryUrlPrefix = uri.getQueryParameter(KEY_URL_PREFIX);
            entryType = uri.getQueryParameter("type");
            //noinspection ConstantConditions
            if (!DEBUG_LOG.equals(entryType) && !"picker".equals(BuildConfig.FLAVOR_function)) {
                if (BuildConfig.IS_I18N) {
                    mTextTip.setText("Launch failed: type parameter mismatch");
                } else {
                    mTextTip.setText("启动失败：type参数错误");
                }
                return;
            }
            if (Utils.isEmpty(entryUrlPrefix)) {
                if (BuildConfig.IS_I18N) {
                    mTextTip.setText("Launch failed: url_prefix parameter not provided");
                } else {
                    mTextTip.setText("启动失败：缺少url_prefix参数");
                }
                return;
            }
        }

        // 判断是否已经初始化了AppLog，如果已经初始化则主动启动任务
        IAppLogInstance instance = AppLogManager.getInstance(entryAppId);
        if (null != instance && instance.hasStarted()) {
            getLogger()
                    .debug(
                            Collections.singletonList("SimulateLaunchActivity"),
                            "AppLog has started with appId:{}",
                            entryAppId);
            SimulateLoginTask.start((AppLogInstance) instance);
        }

        boolean existsActivity = hasActivity();
        getLogger()
                .debug(
                        Collections.singletonList("SimulateLaunchActivity"),
                        "Simulator onCreate appId: {}, urlPrefix: {}, mode: {}, params: {}, "
                                + "activity exists: {}",
                        entryAppId,
                        entryUrlPrefix,
                        entryMode,
                        entryQrParam,
                        existsActivity);

        // 跳转到应用默认首页
        if (!existsActivity) {
            String packageName = getApplicationInfo().packageName;
            PackageManager packageManager = getPackageManager();
            Intent intent = packageManager.getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.setPackage(null);
                startActivity(intent);
            }
        }

        // 关闭simulate activity
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public JSONObject pageProperties() {
        try {
            return new JSONObject().put("class_name", "SimulateLaunchActivity");
        } catch (JSONException e) {
            getLogger()
                    .debug(
                            Collections.singletonList("SimulateLaunchActivity"),
                            "JSON handle failed",
                            e);
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

    private IAppLogLogger getLogger() {
        IAppLogLogger logLogger = LoggerImpl.getLogger(entryAppId);
        return null != logLogger ? logLogger : LoggerImpl.global();
    }

    /**
     * 是否已有Activity
     *
     * @return true:存在Activity
     */
    private boolean hasActivity() {
        try {
            @SuppressLint("PrivateApi")
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread =
                    activityThreadClass.getMethod("currentActivityThread").invoke(null);
            @SuppressLint("DiscouragedPrivateApi")
            Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);
            Map<Object, Object> activities;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                activities = (HashMap<Object, Object>) activitiesField.get(activityThread);
            } else {
                activities = (ArrayMap<Object, Object>) activitiesField.get(activityThread);
            }
            return null != activities && activities.size() > 0;
        } catch (Throwable e) {
            getLogger()
                    .debug(
                            Collections.singletonList("SimulateLaunchActivity"),
                            "Check has activity failed",
                            e);
        }
        return false;
    }
}
