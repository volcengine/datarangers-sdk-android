// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.store;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.util.TLog;
import com.bytedance.applog.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * 自定义的事件
 *
 * @author luodong.seu
 */
public class CustomEvent extends BaseData {

    static final String COL_CATEGORY = "category";
    static final String COL_PARAM = "params";
    static final String TABLE = "custom_event";

    private String category = null;
    private String param = null;

    public CustomEvent() {}

    public CustomEvent(@NonNull final String category, JSONObject params) {
        this(category, params, AppLogInstance.DEFAULT_EVENT);
    }

    public CustomEvent(@NonNull final String category, JSONObject params, final int eventType) {
        this.category = category;
        if (null != params) {
            this.param = params.toString();
        }
        this.eventType = eventType;
    }

    @Override
    protected List<String> getColumnDef() {
        List<String> sd = super.getColumnDef();
        ArrayList<String> def = new ArrayList<>(sd.size());
        def.addAll(sd);
        def.addAll(
                Arrays.asList(
                        COL_PARAM, "varchar",
                        COL_CATEGORY, "varchar"));
        return def;
    }

    @Override
    public int readDb(@NonNull final Cursor c) {
        int i = super.readDb(c);
        param = c.getString(i++);
        category = c.getString(i++);
        return i;
    }

    @Override
    protected void writeDb(@NonNull final ContentValues cv) {
        super.writeDb(cv);
        cv.put(COL_PARAM, param);
        cv.put(COL_CATEGORY, category);
    }

    @Override
    protected void writeIpc(@NonNull final JSONObject obj) throws JSONException {
        super.writeIpc(obj);
        obj.put(COL_PARAM, param);
        obj.put(COL_CATEGORY, category);
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
        if (Utils.isNotEmpty(param)) {
            try {
                JSONObject jsonObject = new JSONObject(param);
                Iterator<String> iterator = jsonObject.keys();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    Object value = jsonObject.get(key);
                    if (obj.opt(key) != null) {
                        TLog.w("自定义事件存在重复的key");
                    }
                    obj.put(key, value);
                }
            } catch (Exception e) {
                TLog.e("解析事件参数失败", e);
            }
        }
        return obj;
    }

    @Override
    protected BaseData readIpc(@NonNull final JSONObject obj) {
        super.readIpc(obj);
        param = obj.optString(COL_PARAM, null);
        category = obj.optString(COL_CATEGORY, null);
        return this;
    }

    @NonNull
    @Override
    String getTableName() {
        return TABLE;
    }

    @Override
    protected String getDetail() {
        return "param:" + param + " category:" + category;
    }

    protected String getCategory() {
        return category;
    }
}
