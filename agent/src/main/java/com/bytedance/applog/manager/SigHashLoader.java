// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.manager;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;

import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.server.Api;
import com.bytedance.applog.util.DigestUtils;
import com.bytedance.applog.util.PackageUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author shiyanlong
 * @date 2019/2/2
 */
class SigHashLoader extends BaseLoader {

    private final Context mApp;
    private final AppLogInstance appLogInstance;

    SigHashLoader(AppLogInstance appLogInstance, Context ctx) {
        super(true, false);
        this.appLogInstance = appLogInstance;
        mApp = ctx;
    }

    @Override
    protected boolean doLoad(final JSONObject info) throws JSONException {
        String sigHash = null;
        PackageInfo pkg = null;
        try {
            pkg = PackageUtils.getPackageSignature(mApp, mApp.getPackageName());
        } catch (Throwable e) {
            appLogInstance.getLogger().error("Get package info failed", e);
        }
        if (pkg != null && pkg.signatures != null && pkg.signatures.length > 0) {
            Signature sig = pkg.signatures[0];
            if (sig != null) {
                byte[] data = sig.toByteArray();
                sigHash = DigestUtils.md5Hex(data);
            }
        }
        if (sigHash != null) {
            info.put(Api.KEY_SIG_HASH, sigHash);
        }
        return true;
    }

    @Override
    protected String getName() {
        return "SigHash";
    }
}
