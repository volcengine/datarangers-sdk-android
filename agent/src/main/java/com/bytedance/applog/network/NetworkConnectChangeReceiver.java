// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import com.bytedance.applog.util.NetworkUtils;

/**
 * fork from com.bytedance.component.silk.road:mohist-standard-tools:0.0.19
 *
 * <p>监听网络变化
 *
 * @author luodong.seu
 */
public class NetworkConnectChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())
                || "android.net.wifi.WIFI_STATE_CHANGED".equals(intent.getAction())
                || "android.net.wifi.STATE_CHANGE".equals(intent.getAction())) {
            NetworkUtils.setNetworkType(NetworkUtils.getNetworkType(context));
        }
    }
}
