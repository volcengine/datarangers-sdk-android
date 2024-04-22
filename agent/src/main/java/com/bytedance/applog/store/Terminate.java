// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.store;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.bytedance.applog.log.LogInfo;
import com.bytedance.applog.server.Api;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * @author shiyanlong
 * @date 2019/1/16
 */
public class Terminate extends BaseData {

    static final String TABLE = "terminate";

    private static final String COL_DURATION = "duration";

    private static final String COL_STOP_TS = "stop_timestamp";

    long duration;

    long stopTs;

    String lastSession;

    @Override
    protected List<String> getColumnDef() {
        return null;
    }

    @Override
    public int readDb(@NonNull final Cursor c) {
        getLogger().error(LogInfo.Category.EVENT, loggerTags, "Not allowed");
        return 0;
    }

    @Override
    protected void writeDb(@NonNull final ContentValues cv) {
        getLogger().error(LogInfo.Category.EVENT, loggerTags, "Not allowed");
    }

    @Override
    protected void writeIpc(@NonNull final JSONObject obj) {
        getLogger().error(LogInfo.Category.EVENT, loggerTags, "Not allowed");
    }

    @Override
    protected JSONObject writePack() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put(COL_TS, ts);
        obj.put(COL_EID, eid);
        obj.put(COL_SID, sid);
        obj.put(COL_STOP_TS, stopTs / 1000);
        obj.put(COL_DURATION, duration / 1000);
        obj.put(COL_DATE_TIME, mDT);
        if (uid > 0L) {
            obj.put(COL_UID, uid);
        }
        obj.put(COL_UUID, TextUtils.isEmpty(uuid) ? JSONObject.NULL : uuid);
        if (!TextUtils.isEmpty(uuidType)) {
            obj.put(Api.KEY_USER_UNIQUE_ID_TYPE_NEW, uuidType);
        }
        if (!TextUtils.isEmpty(ssid)) {
            obj.put(COL_SSID, ssid);
        }
        if (!TextUtils.isEmpty(abSdkVersion)) {
            obj.put(Api.KEY_AB_SDK_VERSION, abSdkVersion);
        }
        if (!TextUtils.isEmpty(lastSession)) {
            obj.put("uuid_changed", true);
            if (!TextUtils.equals(lastSession, sid)) {
                obj.put("original_session_id", lastSession);
            }
        }

        // 添加params字段
        mergePropsToParams(obj);

        return obj;
    }

    @Override
    protected BaseData readIpc(@NonNull final JSONObject obj) {
        getLogger().error(LogInfo.Category.EVENT, loggerTags, "Not allowed");
        return this;
    }

    @NonNull
    @Override
    String getTableName() {
        return TABLE;
    }

    @Override
    protected String getDetail() {
        return String.valueOf(duration);
    }
}
