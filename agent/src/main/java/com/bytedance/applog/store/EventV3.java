// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.store;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.server.Api;
import com.bytedance.applog.util.NetworkUtils;
import com.bytedance.applog.util.TLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author shiyanlong
 * @date 2019/1/16
 */
public class EventV3 extends BaseData {

    static final String COL_EVENT = "event";

    static final String COL_BAV = "is_bav";

    static final String TABLE = "eventv3";

    protected String param;

    private boolean bav;

    protected String event;

    public EventV3() {}

    public EventV3(final String event) {
        this.event = event;
    }

    public EventV3(final String event, JSONObject properties) {
        this.event = event;
        setProperties(properties);
    }

    public EventV3(final String appId, final String event, final boolean bav, final String param) {
        this.appId = appId;
        this.event = event;
        this.bav = bav;
        this.param = param;
        this.eventType = AppLogInstance.DEFAULT_EVENT;
    }

    public EventV3(
            final String appId,
            final String event,
            final boolean bav,
            final String param,
            final int eventType) {
        this.appId = appId;
        this.event = event;
        this.bav = bav;
        this.param = param;
        this.eventType = eventType;
    }

    @Override
    protected List<String> getColumnDef() {
        List<String> sd = super.getColumnDef();
        ArrayList<String> def = new ArrayList<>(sd.size());
        def.addAll(sd);
        def.addAll(
                Arrays.asList(
                        COL_EVENT, "varchar",
                        COL_PARAM, "varchar",
                        COL_BAV, "integer"));
        return def;
    }

    @Override
    public int readDb(@NonNull final Cursor c) {
        int i = super.readDb(c);
        event = c.getString(i++);
        param = c.getString(i++);
        bav = c.getInt(i++) == 1;
        return i;
    }

    @Override
    protected void writeDb(@NonNull final ContentValues cv) {
        super.writeDb(cv);
        cv.put(COL_EVENT, event);
        if (bav && param == null) {
            try {
                fillParam();
            } catch (Throwable e) {
                TLog.ysnp(e);
            }
        }
        cv.put(COL_PARAM, param);
        cv.put(COL_BAV, bav ? 1 : 0);
    }

    @Override
    protected void writeIpc(@NonNull final JSONObject obj) throws JSONException {
        super.writeIpc(obj);
        obj.put(COL_EVENT, event);
        if (bav && param == null) {
            fillParam();
        }
        obj.put(COL_PARAM, param);
        obj.put(COL_BAV, bav);
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
        if (bav) {
            obj.put(COL_BAV, 1);
        }
        if (bav && param == null) {
            fillParam();
        }
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
        bav = obj.optBoolean(COL_BAV, false);
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
