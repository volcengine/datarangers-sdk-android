// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.migrate;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;

import com.bytedance.applog.server.Api;
import com.bytedance.applog.store.SharedPreferenceCacheHelper;
import com.bytedance.applog.util.TLog;

/**
 * 检测App是否是「一健迁移」到另一个手机上 <br>
 */
public final class MigrateDetector {
    private static final int STATE_DISABLED = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
    private static final int STATE_ENABLED = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    private static final int STATE_DEFAULT = PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
    private static final int STATE_FLAG = PackageManager.DONT_KILL_APP;

    public static final String SP_FILE = "bdtracker_dr_migrate_detector";
    @VisibleForTesting public static final String KEY_COMPONENT_STATE = "component_state";

    private final PackageManager pm;
    private final ComponentName component;
    private final boolean migrate;
    private final SharedPreferences sp;

    @WorkerThread
    public MigrateDetector(Context context, String spName) {
        Context app = context.getApplicationContext();
        sp =
                SharedPreferenceCacheHelper.getSafeSharedPreferences(
                        app, spName, Context.MODE_PRIVATE);
        pm = app.getPackageManager();

        // Component的配置是和应用相关的，除非卸载重装，否则一直都在
        component = new ComponentName(context, MigrateDetectorActivity.class);
        migrate = isMigrateInternal();
        TLog.d("MigrateDetector#constructor migrate=" + migrate);
    }

    public void disableComponent() {
        TLog.d("MigrateDetector#disableComponent");
        pm.setComponentEnabledSetting(component, STATE_DISABLED, STATE_FLAG);
        sp.edit().putInt(KEY_COMPONENT_STATE, STATE_DISABLED).apply();
    }

    public boolean isMigrate() {
        return migrate;
    }

    private int getComponentEnabledSetting() {
        return pm.getComponentEnabledSetting(component);
    }

    private boolean isMigrateInternal() {
        int cs;
        try {
            cs = getComponentEnabledSetting();
        } catch (Exception e) {
            return false;
        }
        int ss = sp.getInt(KEY_COMPONENT_STATE, STATE_DEFAULT);
        TLog.d(
                "MigrateDetector#isMigrateInternal cs="
                        + getComponentState(cs)
                        + " ss="
                        + getComponentState(ss));
        if (cs == STATE_DEFAULT && ss == STATE_DISABLED) {
            return true;
        }
        return false;
    }

    private static String getComponentState(int state) {
        switch (state) {
            case STATE_ENABLED:
                return "STATE_ENABLED";
            case STATE_DISABLED:
                return "STATE_DISABLED";
            case STATE_DEFAULT:
                return "STATE_DEFAULT";
            default:
                return "UNKNOWN";
        }
    }

    public static void saveOldDid(SharedPreferences sp, String did, boolean isMigrate) {
        if (sp == null) {
            return;
        }
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(Api.KEY_OLD_DID, did);
        if (isMigrate) {
            editor.putBoolean("is_migrate", true);
        } else {
            editor.remove("is_migrate");
        }
        editor.apply();
    }

    @Nullable
    public static String getOldDid(SharedPreferences sp) {
        if (sp != null) {
            return sp.getString(Api.KEY_OLD_DID, null);
        }
        return null;
    }

    public static boolean isThisDeviceMigrate(SharedPreferences sp) {
        if (sp != null) {
            return sp.getBoolean("is_migrate", false);
        }
        return false;
    }
}
