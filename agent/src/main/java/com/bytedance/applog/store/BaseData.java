// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.store;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bytedance.applog.log.AbstractAppLogLogger;
import com.bytedance.applog.log.IAppLogLogger;
import com.bytedance.applog.log.LogInfo;
import com.bytedance.applog.log.LoggerImpl;
import com.bytedance.applog.server.Api;
import com.bytedance.applog.util.AbsSingleton;
import com.bytedance.applog.util.JsonUtils;
import com.bytedance.applog.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * @author shiyanlong
 * @date 2019/1/16
 */
public abstract class BaseData implements Cloneable {

    private static final SimpleDateFormat DATE_FORMAT_MS =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    static final String COL_ID = "_id";

    public static final String COL_PARAM = "params";

    static final String COL_TS = Api.KEY_LOCAL_TIME_MS;

    static final String COL_EID = Api.KEY_EVENT_INDEX;

    static final String COL_SID = Api.KEY_SESSION_ID;

    static final String COL_UID = Api.KEY_USER_ID;

    static final String COL_UUID = Api.KEY_USER_UNIQUE_ID;

    static final String COL_UUID_TYPE = Api.KEY_USER_UNIQUE_ID_TYPE;

    static final String COL_SSID = Api.KEY_SSID;

    static final String COL_NT = "nt";

    static final String COL_AB = Api.KEY_AB_SDK_VERSION;

    static final String COL_EVENT_TYPE = "event_type";

    static final String COL_APP_ID = "_app_id";

    static final String COL_PROPERTIES = "properties";

    static final String COL_LOCAL_EVENT_ID = "local_event_id";

    /** 格式化的时间字符串。不存DB，可以从TS计算而来。 */
    static final String COL_DATE_TIME = Api.KEY_DATETIME;

    /** logger标签 */
    protected List<String> loggerTags;

    long dbId;

    public long ts;

    public long eid; // event id

    /** sid/uid/uuid/abVersion/abSdkVersion，仅主进程读写，无需ipc */
    public String sid;

    public long uid;

    public String uuid;

    public String uuidType;

    public String ssid;

    public String abSdkVersion;

    public int nt;

    public int eventType;

    /** 应用ID */
    protected String appId;

    /** 格式化的时间字符串。不存DB，在toPack时可以从TS计算而来。 */
    protected String mDT;

    /** 额外的属性 */
    public JSONObject properties;

    /** 本地事件ID */
    public String localEventId;

    /** 每一个json字符串中，都有一个k_cls字段，对后台来说是冗余的 */
    private static final String KEY_CLASS = "k_cls";

    /** 慵懒加载方式初始化，防止死锁 */
    private static final AbsSingleton<HashMap<String, BaseData>> ZYGOTES =
            new AbsSingleton<HashMap<String, BaseData>>() {
                @Override
                protected HashMap<String, BaseData> create(Object... params) {
                    return getAllBaseDataObj();
                }
            };

    public BaseData() {
        setTs(0);
        loggerTags = Collections.singletonList(getTableName());

        // 本地的事件ID，保证唯一性
        localEventId = Utils.getUniqueEventId();
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public void setTs(long value) {
        if (value == 0) {
            value = System.currentTimeMillis();
        }
        ts = value;
    }

    /**
     * 获取所有BaseData的对象
     *
     * @return HashMap
     */
    public static HashMap<String, BaseData> getAllBaseDataObj() {
        HashMap<String, BaseData> map = new HashMap<>();
        registerZygote(map, new Page());
        registerZygote(map, new Launch());
        registerZygote(map, new Terminate());
        registerZygote(map, new PackV2());
        registerZygote(map, new EventV3());
        registerZygote(map, new CustomEvent());
        registerZygote(map, new Profile(null, null));
        return map;
    }

    private static void registerZygote(HashMap<String, BaseData> map, BaseData data) {
        map.put(data.getTableName(), data);
    }

    /**
     * 返回DB表的定义
     *
     * @return DB定义
     */
    protected List<String> getColumnDef() {
        return Arrays.asList(
                COL_ID,
                "integer primary key autoincrement",
                COL_TS,
                "integer",
                COL_EID,
                "integer",
                COL_NT,
                "integer",
                COL_UID,
                "integer",
                COL_SID,
                "varchar",
                COL_UUID,
                "varchar",
                COL_UUID_TYPE,
                "varchar",
                COL_SSID,
                "varchar",
                COL_AB,
                "varchar",
                COL_EVENT_TYPE,
                "integer",
                COL_APP_ID,
                "varchar",
                COL_PROPERTIES,
                "varchar",
                COL_LOCAL_EVENT_ID,
                "varchar");
    }

    /**
     * 从Cursor中读取各字段
     *
     * @param c cursor
     * @return nextIndex
     */
    public int readDb(@NonNull final Cursor c) {
        int i = 0;
        dbId = c.getLong(i++);
        ts = c.getLong(i++);
        eid = c.getLong(i++);
        nt = c.getInt(i++);
        uid = c.getLong(i++);
        sid = c.getString(i++);
        uuid = c.getString(i++);
        uuidType = c.getString(i++);
        ssid = c.getString(i++);
        abSdkVersion = c.getString(i++);
        eventType = c.getInt(i++);
        appId = c.getString(i++);
        String props = c.getString(i++);
        localEventId = c.getString(i++);
        properties = new JSONObject();
        if (!TextUtils.isEmpty(props)) {
            try {
                properties = new JSONObject(props);
            } catch (Exception ignored) {

            }
        }
        return i;
    }

    /**
     * 将各字段写入到cv中
     *
     * @param cv cv
     */
    protected void writeDb(@NonNull final ContentValues cv) {
        cv.put(COL_TS, ts);
        cv.put(COL_EID, eid);
        cv.put(COL_NT, nt);
        cv.put(COL_UID, uid);
        cv.put(COL_SID, sid);
        cv.put(COL_UUID, Utils.toString(uuid));
        cv.put(COL_UUID_TYPE, uuidType);
        cv.put(COL_SSID, ssid);
        cv.put(COL_AB, abSdkVersion);
        cv.put(COL_EVENT_TYPE, eventType);
        cv.put(COL_APP_ID, appId);
        cv.put(COL_PROPERTIES, null != properties ? properties.toString() : "");
        cv.put(COL_LOCAL_EVENT_ID, localEventId);
    }

    public static String formatDateMS(final long ts) {
        return DATE_FORMAT_MS.format(new Date(ts));
    }

    /**
     * 将各字段写入到obj中。
     *
     * @param obj jsonObj
     * @throws JSONException 异常
     */
    protected void writeIpc(@NonNull final JSONObject obj) throws JSONException {
        obj.put(COL_TS, ts);
        obj.put(COL_APP_ID, appId);
        obj.put(COL_PROPERTIES, properties);
        obj.put(COL_LOCAL_EVENT_ID, localEventId);
    }

    /**
     * 按照上报协议的格式，填充json
     *
     * @throws JSONException 异常
     */
    protected abstract JSONObject writePack() throws JSONException;

    /**
     * 从obj读取各个字段
     *
     * @param obj jsonObj
     * @return 自己
     */
    protected BaseData readIpc(@NonNull final JSONObject obj) {
        ts = obj.optLong(COL_TS, 0L);
        dbId = 0;
        eid = 0;
        nt = 0;
        uid = 0;
        sid = null;
        uuid = null;
        uuidType = null;
        ssid = null;
        abSdkVersion = null;
        appId = obj.optString(COL_APP_ID);
        properties = obj.optJSONObject(COL_PROPERTIES);
        localEventId = obj.optString(COL_LOCAL_EVENT_ID, Utils.getUniqueEventId());
        return this;
    }

    final ContentValues toValues(@Nullable ContentValues cv) {
        if (cv == null) {
            cv = new ContentValues();
        } else {
            cv.clear();
        }
        writeDb(cv);
        return cv;
    }

    final String createTable() {
        String result = null;
        final List<String> columns = getColumnDef();
        if (columns != null) {
            StringBuilder builder = new StringBuilder(128);
            builder.append("create table if not exists ").append(getTableName()).append("(");
            for (int i = 0; i < columns.size(); i += 2) {
                builder.append(columns.get(i)).append(" ").append(columns.get(i + 1)).append(",");
            }
            builder.delete(builder.length() - 1, builder.length());
            builder.append(")");
            result = builder.toString();
        }
        return result;
    }

    /**
     * 获取表明
     *
     * @return 表明
     */
    @NonNull
    abstract String getTableName();

    @NonNull
    public final JSONObject toIpcJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put(KEY_CLASS, getTableName());
            writeIpc(obj);
        } catch (JSONException e) {
            getLogger().error(LogInfo.Category.EVENT, loggerTags, "JSON handle failed", e);
        }
        return obj;
    }

    @NonNull
    public final JSONObject toPackJson() {
        JSONObject obj = new JSONObject();
        try {
            mDT = formatDateMS(ts);
            obj = writePack();
            //            obj.put("$appId", appId);
        } catch (JSONException e) {
            getLogger().error(LogInfo.Category.EVENT, loggerTags, "JSON handle failed", e);
        }
        return obj;
    }

    public static BaseData fromIpc(String string) {
        try {
            JSONObject obj = new JSONObject(string);
            BaseData data = ZYGOTES.get().get(obj.optString(KEY_CLASS, "")).clone();
            return data.readIpc(obj);
        } catch (Throwable e) {
            LoggerImpl.global().error(LogInfo.Category.EVENT, "JSON handle failed", e);
        }
        return null;
    }

    public static void fillEventAppVersion(BaseData data, String appVersion) {
        try {
            JSONObject properties = data.getProperties();
            if (properties == null) {
                properties = new JSONObject();
            }
            properties.put(Api.KEY_EVENT_APP_VERSION, appVersion);
            data.setProperties(properties);
        } catch (Throwable ignored) {}
    }

    @Override
    public BaseData clone() {
        try {
            BaseData data = (BaseData) super.clone();
            data.localEventId = Utils.getUniqueEventId();
            return data;
        } catch (CloneNotSupportedException e) {
            getLogger().error(LogInfo.Category.EVENT, loggerTags, "Clone data failed", e);
        }
        return null;
    }

    @NonNull
    @Override
    public String toString() {
        String name = getTableName();
        if (!getClass().getSimpleName().equalsIgnoreCase(name)) {
            name = name + ", " + getClass().getSimpleName();
        }

        String shortSid = sid;
        if (shortSid != null) {
            int firstBar = shortSid.indexOf("-");
            if (firstBar >= 0) {
                shortSid = shortSid.substring(0, firstBar);
            }
        } else {
            shortSid = "-";
        }

        return "{" + name + ", " + getDetail() + ", " + shortSid + ", " + ts + ", " + eid + ", " + sid + "}";
    }

    protected String getDetail() {
        return "sid:" + sid;
    }

    public JSONObject getProperties() {
        return this.properties;
    }

    public void setProperties(JSONObject props) {
        this.properties = props;
    }

    public String getParam() {
        return null;
    }

    /**
     * 合并自定义参数
     *
     * @param toObj 合并的对象
     */
    protected void mergePropsToParams(JSONObject toObj) {
        mergePropsToParams(toObj, "");
    }

    /**
     * 合并自定义参数
     *
     * @param params 原始的参数JSON字符串
     * @param toObj 合并的对象
     */
    protected void mergePropsToParams(JSONObject toObj, String params) {
        if (null == toObj) {
            return;
        }
        if (TextUtils.isEmpty(params)) {
            mergePropsToParams(toObj, new JSONObject());
            return;
        }
        try {
            JSONObject paramJson = new JSONObject(params);
            mergePropsToParams(toObj, paramJson);
        } catch (Throwable e) {
            getLogger().error(LogInfo.Category.EVENT, loggerTags, "Merge params failed", e);
        }
    }

    /**
     * 合并自定义参数
     *
     * @param toObj 合并的对象
     * @param params 自定义参数JSON
     */
    protected void mergePropsToParams(JSONObject toObj, JSONObject params) {
        if (null == toObj) {
            return;
        }
        JSONObject finalJson = new JSONObject();
        if (null != params && params.length() > 0) {
            JsonUtils.mergeJsonObject(params, finalJson);
        }
        if (null != properties && properties.length() > 0) {
            JsonUtils.mergeJsonObject(properties, finalJson);
        }
        try {
            toObj.put(COL_PARAM, finalJson);
        } catch (Throwable e) {
            getLogger().error(LogInfo.Category.EVENT, loggerTags, "Merge params failed", e);
        }
    }

    protected IAppLogLogger getLogger() {
        IAppLogLogger logger = AbstractAppLogLogger.getLogger(appId);
        if (null != logger) {
            return logger;
        }
        return LoggerImpl.global();
    }
}
