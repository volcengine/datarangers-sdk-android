// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.manager;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;

import com.bytedance.applog.log.LoggerImpl;
import com.bytedance.applog.server.Api;
import com.bytedance.applog.util.SensitiveUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 对齐内部版,在header中添加appkey字段
 *
 * @author wuzhijun
 */
public class AppKeyLoader extends BaseLoader {

    private final Context mContext;

    AppKeyLoader(Context context) {
        super(true, false);
        mContext = context;
    }

    @Override
    protected boolean doLoad(JSONObject info) throws JSONException, SecurityException {
        String pkg = mContext.getPackageName();
        try {
            ApplicationInfo ai =
                    mContext.getPackageManager()
                            .getApplicationInfo(pkg, PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            String appKey = SensitiveUtils.CHANNEL_APP_KEY;
            if (bundle != null && !TextUtils.isEmpty(appKey)) {
                info.put(Api.KEY_APPKEY, bundle.getString(appKey));
            }
        } catch (Throwable e) {
            LoggerImpl.global().error("Load app key failed.", e);
        }
        return true;
    }

    @Override
    protected String getName() {
        return "AppKey";
    }
}
