// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.store;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.bytedance.applog.server.Api;
import com.bytedance.applog.util.NetworkUtils;
import com.bytedance.applog.util.TLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author: liujunlin
 * @date: 2021/3/4
 */
public class Profile extends BaseData {
    static final String COL_EVENT = "event";
    static final String COL_PARAM = "params";
    static final String TABLE = "profile";
    protected String param;
    protected String event;

    public Profile() {}

    public Profile(final String event, final String param) {
        this.event = event;
        this.param = param;
    }

    @Override
    protected List<String> getColumnDef() {
        List<String> sd = super.getColumnDef();
        ArrayList<String> def = new ArrayList<>(sd.size());
        def.addAll(sd);
        def.addAll(
                Arrays.asList(
                        COL_EVENT, "varchar",
                        COL_PARAM, "varchar"));
        return def;
    }

    @Override
    public int readDb(@NonNull final Cursor c) {
        int i = super.readDb(c);
        event = c.getString(i++);
        param = c.getString(i++);
        return i;
    }

    @Override
    protected void writeDb(@NonNull final ContentValues cv) {
        super.writeDb(cv);
        cv.put(COL_EVENT, event);
        if (param == null) {
            try {
                fillParam();
            } catch (JSONException e) {
                TLog.ysnp(e);
            }
        }
        cv.put(COL_PARAM, param);
    }

    @Override
    protected void writeIpc(@NonNull final JSONObject obj) throws JSONException {
        super.writeIpc(obj);
        obj.put(COL_EVENT, event);
        if (param == null) {
            fillParam();
        }
        obj.put(COL_PARAM, param);
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
        obj.put(COL_EVENT, event);
        mergePropsToParams(obj, param);
        if (nt != NetworkUtils.NetworkType.UNKNOWN.getValue()) {
            obj.put(COL_NT, nt);
        }
        obj.put(COL_DATE_TIME, mDT);

        if (!TextUtils.isEmpty(abSdkVersion)) {
            obj.put(Api.KEY_AB_SDK_VERSION, abSdkVersion);
        }
        return obj;
    }

    @Override
    protected BaseData readIpc(@NonNull final JSONObject obj) {
        super.readIpc(obj);
        event = obj.optString(COL_EVENT, null);
        param = obj.optString(COL_PARAM, null);
        return this;
    }

    /**
     * 填充param。
     *
     * @throws JSONException 外部统一处理这个异常。
     */
    protected void fillParam() throws JSONException {}

    @NonNull
    @Override
    String getTableName() {
        return TABLE;
    }

    @Override
    protected String getDetail() {
        return event;
    }

    public String getEvent() {
        return event;
    }

    @Override
    public String getParam() {
        return param;
    }
}
