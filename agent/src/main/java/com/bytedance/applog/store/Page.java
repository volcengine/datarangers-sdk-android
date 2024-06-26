// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.store;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.bytedance.applog.server.Api;
import com.bytedance.applog.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author shiyanlong
 * @date 2019/1/18
 */
public class Page extends BaseData {

    public static final String EVENT_KEY = "bav2b_page";
    public static final String COL_NAME = "page_key";
    public static final String COL_FROM = "refer_page_key";
    public static final String COL_TITLE = "page_title";
    public static final String COL_TITLE_FROM = "refer_page_title";
    public static final String COL_PATH = "page_path";
    public static final String COL_PATH_FROM = "referrer_page_path";
    public static final String COL_DURATION = "duration";
    public static final String COL_BACK = "is_back";
    public static final String COL_LAST_SESSION = "last_session";
    public static final String COL_IS_CUSTOM = "is_custom";
    public static final String COL_IS_FRAGMENT = "is_fragment";
    public static final String COL_RESUME_AT = "resume_at";
    public static final String TABLE = "page";
    public long duration;
    public String last;
    public String name;

    // 全埋点
    public String title;
    public String referTitle;
    public String path;
    public String referPath;
    public long resumeAt;

    public int back;
    public String lastSession;

    // 是否为自定义的页面
    public boolean isCustom;
    // 是否为fragment
    public boolean isFragment;

    /** 类型，用于过滤 */
    public Class<?> clazz;

    @Override
    protected List<String> getColumnDef() {
        List<String> sd = super.getColumnDef();
        ArrayList<String> def = new ArrayList<>(sd.size());
        def.addAll(sd);
        def.addAll(
                Arrays.asList(
                        COL_NAME,
                        "varchar",
                        COL_FROM,
                        "varchar",
                        COL_DURATION,
                        "integer",
                        COL_BACK,
                        "integer",
                        COL_LAST_SESSION,
                        "varchar",
                        COL_TITLE,
                        "varchar",
                        COL_TITLE_FROM,
                        "varchar",
                        COL_PATH,
                        "varchar",
                        COL_PATH_FROM,
                        "varchar",
                        COL_IS_CUSTOM,
                        "integer",
                        COL_IS_FRAGMENT,
                        "integer",
                        COL_RESUME_AT,
                        "integer"));
        return def;
    }

    @Override
    public int readDb(@NonNull final Cursor c) {
        int i = super.readDb(c);
        name = c.getString(i++);
        last = c.getString(i++);
        duration = c.getLong(i++);
        back = c.getInt(i++);
        lastSession = c.getString(i++);
        title = c.getString(i++);
        referTitle = c.getString(i++);
        path = c.getString(i++);
        referPath = c.getString(i++);
        isCustom = c.getInt(i++) == 1;
        isFragment = c.getInt(i++) == 1;
        resumeAt = c.getLong(i++);
        return i;
    }

    @Override
    protected void writeDb(@NonNull final ContentValues cv) {
        super.writeDb(cv);
        cv.put(COL_NAME, getName());
        cv.put(COL_FROM, last);
        cv.put(COL_DURATION, duration);
        cv.put(COL_BACK, back);
        cv.put(COL_LAST_SESSION, lastSession);
        cv.put(COL_TITLE, title);
        cv.put(COL_TITLE_FROM, referTitle);
        cv.put(COL_PATH, path);
        cv.put(COL_PATH_FROM, referPath);
        cv.put(COL_IS_CUSTOM, isCustom ? 1 : 0);
        cv.put(COL_IS_FRAGMENT, isFragment ? 1 : 0);
        cv.put(COL_RESUME_AT, resumeAt > 0 ? resumeAt : ts);
    }

    @Override
    protected void writeIpc(@NonNull final JSONObject obj) throws JSONException {
        super.writeIpc(obj);
        obj.put(COL_NAME, getName());
        obj.put(COL_FROM, last);
        obj.put(COL_DURATION, duration);
        obj.put(COL_BACK, back);
        obj.put(COL_TITLE, title);
        obj.put(COL_TITLE_FROM, referTitle);
        obj.put(COL_PATH, path);
        obj.put(COL_PATH_FROM, referPath);
        obj.put(COL_IS_CUSTOM, isCustom);
        obj.put(COL_IS_FRAGMENT, isFragment);
        obj.put(COL_RESUME_AT, resumeAt);
    }

    @Override
    protected BaseData readIpc(@NonNull final JSONObject obj) {
        super.readIpc(obj);
        name = obj.optString(COL_NAME, "");
        last = obj.optString(COL_FROM, null);
        duration = obj.optLong(COL_DURATION, 0L);
        back = obj.optInt(COL_BACK, 0);
        title = obj.optString(COL_TITLE, "");
        referTitle = obj.optString(COL_TITLE_FROM, null);
        path = obj.optString(COL_PATH, null);
        referPath = obj.optString(COL_PATH_FROM, null);
        isCustom = obj.optBoolean(COL_IS_CUSTOM, false);
        isFragment = obj.optBoolean(COL_IS_FRAGMENT, false);
        resumeAt = obj.optLong(COL_RESUME_AT, 0L);
        return this;
    }

    @Override
    protected JSONObject writePack() throws JSONException {
        JSONObject obj = new JSONObject();
        long pageTs = resumeAt > 0 ? resumeAt : ts;
        obj.put(COL_TS, pageTs);
        obj.put(COL_DATE_TIME, formatDateMS(pageTs));
        obj.put(COL_EID, eid);
        obj.put(COL_SID, sid);
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
        obj.put(EventV3.COL_EVENT, EVENT_KEY);
        obj.put(EventV3.COL_BAV, 1);
        mergePropsToParams(obj, fillParam());

        return obj;
    }

    private JSONObject fillParam() throws JSONException {
        JSONObject p = new JSONObject();
        p.put(COL_NAME, getName());
        p.put(COL_FROM, last);
        p.put(COL_BACK, back);
        p.put(COL_DURATION, duration);
        p.put(COL_TITLE, title);
        p.put(COL_TITLE_FROM, referTitle);
        p.put(COL_PATH, path);
        p.put(COL_PATH_FROM, referPath);
        return p;
    }

    @NonNull
    @Override
    String getTableName() {
        return TABLE;
    }

    /**
     * onResume和onPause，都会生成一个page事件。 resume时mDuration是-1; pause时mDuration是正数 resume事件不需要存储
     *
     * @return 是否是resume事件
     */
    public boolean isResumeEvent() {
        return duration == -1L;
    }

    public boolean isActivity() {
        return !isFragment;
    }

    @Override
    protected String getDetail() {
        return getName() + ", " + duration;
    }

    private String getName() {
        return Utils.toString(name);
    }
}
