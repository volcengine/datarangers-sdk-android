// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.server;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.IAppContext;
import com.bytedance.applog.IExtraParams;
import com.bytedance.applog.Level;
import com.bytedance.applog.UriConfig;
import com.bytedance.applog.engine.Engine;
import com.bytedance.applog.log.LogInfo;
import com.bytedance.applog.util.EncryptUtils;
import com.bytedance.applog.util.NetworkUtils;
import com.bytedance.applog.util.PrivateAgreement;
import com.bytedance.applog.util.SensitiveUtils;
import com.bytedance.applog.util.UIUtils;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author linguoqing
 */
public final class ApiParamsUtil {

    /** 额外参数设置接口 仅影响API的url params，对齐内部applog功能接口 */
    private volatile IExtraParams sExtraParams;

    private static final String SEND_TAIL_ENCRYPT = "?tt_data=a";

    private static class TripletParam {
        private final String fromKey;
        private final String toKey;
        private final Class paramClass;

        public TripletParam(String fromKey, String toKey, Class paramClass) {
            this.fromKey = fromKey;
            this.toKey = toKey;
            this.paramClass = paramClass;
        }

        public String getFromKey() {
            return fromKey;
        }

        public String getToKey() {
            return toKey;
        }

        public Class getParamClass() {
            return paramClass;
        }
    }

    private static final TripletParam[] NET_PARAM_KEYS_TRIPLET_ARRAY =
            new TripletParam[] {
                new TripletParam(Api.KEY_AID, Api.KEY_AID, String.class),
                new TripletParam(Api.KEY_GOOGLE_AID, Api.KEY_GOOGLE_AID, String.class),
                new TripletParam(Api.KEY_CARRIER, Api.KEY_CARRIER, String.class),
                new TripletParam(Api.KEY_MCC_MNC, Api.KEY_MCC_MNC, String.class),
                new TripletParam(Api.KEY_SIM_REGION, Api.KEY_SIM_REGION, String.class),
                new TripletParam(Api.KEY_DEVICE_ID, Api.KEY_DEVICE_ID, String.class),
                new TripletParam(Api.KEY_BD_DID, Api.KEY_BD_DID, String.class),
                new TripletParam(Api.KEY_INSTALL_ID, EncryptUtils.KEY_IID, String.class),
                new TripletParam(Api.KEY_C_UDID, Api.KEY_C_UDID, String.class),
                new TripletParam(Api.KEY_APP_NAME, Api.KEY_APP_NAME, String.class),
                new TripletParam(Api.KEY_APP_VERSION, "version_name", String.class),
                new TripletParam(Api.KEY_VERSION_CODE, Api.KEY_VERSION_CODE, Integer.class),
                new TripletParam(
                        Api.KEY_MANIFEST_VERSION_CODE,
                        Api.KEY_MANIFEST_VERSION_CODE,
                        Integer.class),
                new TripletParam(
                        Api.KEY_UPDATE_VERSION_CODE, Api.KEY_UPDATE_VERSION_CODE, Integer.class),
                new TripletParam(Api.KEY_SDK_VERSION_CODE, Api.KEY_SDK_VERSION_CODE, Integer.class)
            };

    private final AppLogInstance appLogInstance;

    public ApiParamsUtil(final AppLogInstance appLogInstance) {
        this.appLogInstance = appLogInstance;
    }

    public void setsExtraParams(IExtraParams params) {
        sExtraParams = params;
    }

    public String appendNetParams(JSONObject header, String url, boolean isApi, Level level) {
        if (appLogInstance.getContext() == null || TextUtils.isEmpty(url)) {
            return url;
        }
        Uri uri = Uri.parse(url);
        Set<String> keys = uri.getQueryParameterNames();
        Uri.Builder builder = uri.buildUpon();
        final Map<String, String> params = new HashMap<>();
        appendParamsToMap(header, isApi, params, level);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (!keys.contains(key) && !TextUtils.isEmpty(value)) {
                builder.appendQueryParameter(key, entry.getValue());
            }
        }
        return builder.build().toString();
    }

    public void appendParamsToMap(
            JSONObject header, boolean isApi, Map<String, String> params, Level level) {
        Context context = appLogInstance.getContext();
        if (context == null || params == null || level == null) {
            return;
        }
        params.put("_rticket", String.valueOf(System.currentTimeMillis()));
        params.put(EncryptUtils.KEY_DEVICE_PLATFORM, "android");
        if (isApi) {
            params.put("ssmix", "a");
        }
        String resolution = UIUtils.getScreenResolution(context);
        if (!TextUtils.isEmpty(resolution)) {
            params.put(Api.KEY_RESOLUTION, resolution);
        }
        int dpi = UIUtils.getDpi(context);
        if (dpi > 0) {
            params.put("dpi", String.valueOf(dpi));
        }
        params.put("device_type", Build.MODEL);
        params.put(Api.KEY_DEVICE_BRAND, Build.BRAND);
        params.put(
                Api.KEY_LANGUAGE, context.getResources().getConfiguration().locale.getLanguage());
        params.put(Api.KEY_OS_API, String.valueOf(Build.VERSION.SDK_INT));
        String ov = Build.VERSION.RELEASE;
        if (ov != null && ov.length() > 10) {
            ov = ov.substring(0, 10);
        }
        params.put(Api.KEY_OS_VERSION, ov);
        // net不从header取，直接取当前值，对齐内部版
        String access = NetworkUtils.getNetworkAccessType(context, false);
        if (!TextUtils.isEmpty(access)) {
            params.put("ac", access);
        }

        // 先处理header中的数据
        for (int i = 0; i < NET_PARAM_KEYS_TRIPLET_ARRAY.length; i++) {
            TripletParam tripletParam = NET_PARAM_KEYS_TRIPLET_ARRAY[i];
            Object value =
                    getValue(header, tripletParam.getFromKey(), null, tripletParam.getParamClass());
            if (value != null) {
                params.put(tripletParam.getToKey(), value.toString());
            }
        }
        String channel = getValue(header, Api.KEY_TWEAKED_CHANNEL, "", String.class);
        if (TextUtils.isEmpty(channel)) {
            channel = getValue(header, Api.KEY_CHANNEL, "", String.class);
        }
        if (!TextUtils.isEmpty(channel)) {
            params.put(Api.KEY_CHANNEL, channel);
        }
        boolean hasAcceptAgreement = PrivateAgreement.hasAccept(context);
        SensitiveUtils.appendSensitiveParams(this, header, params, hasAcceptAgreement, level);
        if (level == Level.L0) {
            String openudid = getValue(header, Api.KEY_OPEN_UDID, null, String.class);
            if (!TextUtils.isEmpty(openudid)) {
                params.put(Api.KEY_OPEN_UDID, openudid);
            }
        }

        IAppContext appContext = appLogInstance.getAppContext();
        if (appContext != null) {
            params.put(Api.KEY_AID, String.valueOf(appContext.getAid()));
            channel = appContext.getTweakedChannel();
            if (TextUtils.isEmpty(channel)) {
                channel = appContext.getChannel();
            }
            if (!TextUtils.isEmpty(channel)) {
                params.put(Api.KEY_CHANNEL, channel);
            }

            String appName = appContext.getAppName();
            if (!TextUtils.isEmpty(appName)) {
                params.put(Api.KEY_APP_NAME, appName);
            }
            params.put(Api.KEY_VERSION_CODE, String.valueOf(appContext.getVersionCode()));
            String version = appContext.getVersion();
            if (!TextUtils.isEmpty(version)) {
                params.put("version_name", version);
            }
            params.put(
                    Api.KEY_MANIFEST_VERSION_CODE,
                    String.valueOf(appContext.getManifestVersionCode()));
            params.put(
                    Api.KEY_UPDATE_VERSION_CODE, String.valueOf(appContext.getUpdateVersionCode()));

            String abVersion = appContext.getAbVersion();
            if (!TextUtils.isEmpty(abVersion)) {
                params.put(Api.KEY_AB_VERSION, abVersion);
            }
            String abClient = appContext.getAbClient();
            if (!TextUtils.isEmpty(abClient)) {
                params.put("ab_client", abClient);
            }
            String abGroup = appContext.getAbGroup();
            if (!TextUtils.isEmpty(abGroup)) {
                params.put("ab_group", abGroup);
            }
            String abFeature = appContext.getAbFeature();
            if (!TextUtils.isEmpty(abFeature)) {
                params.put("ab_feature", abFeature);
            }
            long abflag = appContext.getAbFlag();
            if (abflag > 0) {
                params.put("abflag", String.valueOf(abflag));
            }
        }

        try {
            final HashMap<String, String> extraParams =
                    sExtraParams == null ? null : sExtraParams.getExtraParams(level);
            if (extraParams != null && !extraParams.isEmpty()) {
                for (Map.Entry<String, String> entry : extraParams.entrySet()) {
                    if (entry != null) {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        if (!TextUtils.isEmpty(key)
                                && !TextUtils.isEmpty(value)
                                && !params.containsKey(key)) {
                            params.put(key, value);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            appLogInstance
                    .getLogger()
                    .error(LogInfo.Category.REQUEST, "Add extra params failed.", e);
        }
    }

    public String[] getSendLogUris(Engine engine, JSONObject header, int eventType) {
        UriConfig config = engine.getUriConfig();
        String[] sendUris;
        switch (eventType) {
            case AppLogInstance.DEFAULT_EVENT:
                sendUris = config.getSendUris();
                break;
            case AppLogInstance.BUSINESS_EVENT:
                if (!TextUtils.isEmpty(config.getBusinessUri())) {
                    sendUris = new String[] {config.getBusinessUri()};
                } else {
                    sendUris = config.getSendUris();
                }
                break;
            default:
                sendUris = new String[0];
        }
        final int count = sendUris.length;
        String[] uris = new String[count];

        boolean encrypt = appLogInstance.getEncryptAndCompress();
        for (int i = 0; i < count; i++) {
            uris[i] = sendUris[i];
            if (encrypt) {
                uris[i] = uris[i] + SEND_TAIL_ENCRYPT;
            }
            uris[i] = appendNetParams(header, uris[i], true, Level.L1);
            uris[i] = Api.filterQuery(uris[i], EncryptUtils.KEYS_REPORT_QUERY);
        }
        return uris;
    }

    @Nullable
    public <T> T getValue(JSONObject jsonObject, String key, T fallbackValue, Class<T> type) {
        if (jsonObject == null) {
            return appLogInstance.getHeaderValue(key, fallbackValue, type);
        }
        T value = null;
        Object ret = jsonObject.opt(key);
        if (ret != null && type != null) {
            try {
                value = type.cast(ret);
            } catch (Throwable t) {
                appLogInstance.getLogger().error(LogInfo.Category.REQUEST, "Cast type failed.", t);
            }
        }
        if (value == null) {
            value = fallbackValue;
        }
        return value;
    }
}
