// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.store;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.bytedance.applog.util.NetworkUtils;
import com.bytedance.applog.util.TLog;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 监控信息
 *
 * @author luodong.seu
 */
public class Trace extends BaseData {

    public static final String TABLE = "trace";

    /** 公共的参数 */
    private static final JSONObject COMMON_PARAMS = new JSONObject();

    static {
        try {
            COMMON_PARAMS.put("_staging_flag", 1);
            COMMON_PARAMS.put("params_for_special", "applog_trace");
        } catch (Throwable e) {
            TLog.e(e);
        }
    }

    @Override
    protected JSONObject writePack() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put(COL_TS, ts);
        obj.put(COL_EID, eid);
        obj.put(COL_SID, sid);
        if (uid > 0L) {
            obj.put(COL_UID, uid);
        }
        obj.put(COL_UUID, TextUtils.isEmpty(uuid) ? JSONObject.NULL : uuid);
        if (!TextUtils.isEmpty(ssid)) {
            obj.put(COL_SSID, ssid);
        }
        obj.put(EventV3.COL_EVENT, "applog_trace");
        mergePropsToParams(obj, COMMON_PARAMS);
        if (nt != NetworkUtils.NetworkType.UNKNOWN.getValue()) {
            obj.put(COL_NT, nt);
        }
        obj.put(COL_DATE_TIME, mDT);
        return obj;
    }

    @NonNull
    @Override
    String getTableName() {
        return TABLE;
    }
}
