// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.manager;

import android.accounts.Account;
import android.content.Context;
import android.text.TextUtils;

import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.store.AccountCacheHelper;
import com.bytedance.applog.util.DeviceParamsProvider;
import com.bytedance.applog.util.IDeviceRegisterParameter;

/**
 * @author linguoqing
 * @date 30/10/2017
 */
public class DeviceRegisterParameterFactory {
    private volatile IDeviceRegisterParameter sDeviceRegisterParameterProvider;
    private String sChannel;
    private Account sDeviceAccount;

    private AccountCacheHelper mAccountCache;
    private ConfigManager config;

    public DeviceRegisterParameterFactory() {}

    public IDeviceRegisterParameter getProvider(
            AppLogInstance appLogInstance, Context context, ConfigManager config)
            throws IllegalArgumentException {
        if (sDeviceRegisterParameterProvider == null) {
            synchronized (DeviceRegisterParameterFactory.class) {
                if (sDeviceRegisterParameterProvider == null) {
                    if (context == null) {
                        throw new IllegalArgumentException("context == null");
                    }
                    this.config = config;
                    if (mAccountCache == null) {
                        mAccountCache = new AccountCacheHelper(appLogInstance, context);
                    }
                    if (sDeviceRegisterParameterProvider == null) {
                        sDeviceRegisterParameterProvider =
                                new DeviceParamsProvider(
                                        appLogInstance, context, config, mAccountCache);
                        if (sDeviceAccount != null) {
                            ((DeviceParamsProvider) sDeviceRegisterParameterProvider)
                                    .setAccount(sDeviceAccount);
                        }
                    }
                }
            }
        }
        return sDeviceRegisterParameterProvider;
    }

    public String getChannel() {
        if (TextUtils.isEmpty(sChannel) && config != null) {
            sChannel = config.getChannel();
        }
        return sChannel;
    }

    public void setAccount(Account account) {
        if (sDeviceRegisterParameterProvider instanceof DeviceParamsProvider) {
            ((DeviceParamsProvider) sDeviceRegisterParameterProvider).setAccount(account);
        } else {
            sDeviceAccount = account;
        }
    }
}
