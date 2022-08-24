// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.store;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.bytedance.applog.AppLogHelper;
import com.bytedance.applog.IAppLogInstance;
import com.bytedance.applog.server.Api;
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
public class Launch extends BaseData {

    static final String TABLE = "launch";

    /** 是否后台启动。不存db，pack时从page数量计算而来。 */
    private static final String COL_BG = "is_background";

    private static final String COL_VER_NAME = "ver_name";

    private static final String COL_VER_CODE = "ver_code";

    private static final String COL_LAST_SESSION = "last_session";

    private static final String COL_IS_FIRST_TIME = "is_first_time";

    private static final String COL_PAGE_TITLE = "page_title";

    private static final String COL_PAGE_KEY = "page_key";

    private static final String COL_RESUME_FROM_BACKGROUND = "resume_from_background";

    public int verCode;

    public String verName;

    public boolean mBg;

    public String lastSession;

    public int isFirstTime;

    public String pageTitle;

    public String pageKey;

    public boolean resumeFromBackground;

    @Override
    protected List<String> getColumnDef() {
        List<String> sd = super.getColumnDef();
        ArrayList<String> def = new ArrayList<>(sd.size());
        def.addAll(sd);
        def.addAll(
                Arrays.asList(
                        COL_VER_NAME, "varchar",
                        COL_VER_CODE, "integer",
                        COL_LAST_SESSION, "varchar",
                        COL_IS_FIRST_TIME, "integer",
                        COL_PAGE_TITLE, "varchar",
                        COL_PAGE_KEY, "varchar",
                        COL_RESUME_FROM_BACKGROUND, "integer"));
        return def;
    }

    @Override
    public int readDb(@NonNull final Cursor c) {
        int i = super.readDb(c);
        verName = c.getString(i++);
        verCode = c.getInt(i++);
        lastSession = c.getString(i++);
        isFirstTime = c.getInt(i++);
        pageTitle = c.getString(i++);
        pageKey = c.getString(i++);
        resumeFromBackground = c.getInt(i++) == 1;
        return i;
    }

    @Override
    protected void writeDb(@NonNull final ContentValues cv) {
        super.writeDb(cv);
        cv.put(COL_VER_NAME, verName);
        cv.put(COL_VER_CODE, verCode);
        cv.put(COL_LAST_SESSION, lastSession);
        cv.put(COL_IS_FIRST_TIME, isFirstTime);
        cv.put(COL_PAGE_TITLE, pageTitle);
        cv.put(COL_PAGE_KEY, pageKey);
        cv.put(COL_RESUME_FROM_BACKGROUND, resumeFromBackground ? 1 : 0);
    }

    @Override
    protected void writeIpc(@NonNull final JSONObject obj) {
        TLog.ysnp(null);
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
        if (mBg) {
            obj.put(COL_BG, mBg);
        }
        obj.put(COL_DATE_TIME, mDT);

        if (!TextUtils.isEmpty(abSdkVersion)) {
            obj.put(Api.KEY_AB_SDK_VERSION, abSdkVersion);
        }
        IAppLogInstance appLogInstance = AppLogHelper.getInstanceByAppId(appId);
        if (null != appLogInstance) {
            String deepLinkUrl = appLogInstance.getDeepLinkUrl();
            if (!TextUtils.isEmpty(deepLinkUrl)) {
                obj.put("$deeplink_url", deepLinkUrl);
            }
        }
        if (!TextUtils.isEmpty(lastSession)) {
            obj.put("uuid_changed", true);
            obj.put("original_session_id", lastSession);
        }

        if (isFirstTime == 1) {
            obj.put("$is_first_time", "true");
        }

        // 预置事件补充
        obj.put("$page_title", TextUtils.isEmpty(pageTitle) ? "" : pageTitle);
        obj.put("$page_key", TextUtils.isEmpty(pageKey) ? "" : pageKey);
        obj.put("$resume_from_background", resumeFromBackground ? "true" : "false");

        // 添加params字段
        mergePropsToParams(obj);

        return obj;
    }

    @Override
    protected BaseData readIpc(@NonNull final JSONObject obj) {
        TLog.ysnp(null);
        return null;
    }

    @NonNull
    @Override
    String getTableName() {
        return TABLE;
    }

    @Override
    protected String getDetail() {
        return mBg ? "bg" : "fg";
    }
}
