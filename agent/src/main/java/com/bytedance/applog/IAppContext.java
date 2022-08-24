// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import android.content.Context;

public interface IAppContext {
    Context getContext();

    /**
     * chinese name
     * @return
     */
    String getStringAppName();

    String getAppName();

    String getVersion();

    String getFeedbackAppKey();

    String getChannel();

    String getTweakedChannel();

    int getVersionCode();

    String getDeviceId();

    int getUpdateVersionCode();

    int getManifestVersionCode();

    String getManifestVersion();

    /** get app id */
    int getAid();

    String getAbClient();

    @Deprecated
    long getAbFlag();

    String getAbVersion();

    String getAbGroup();

    String getAbFeature();
}