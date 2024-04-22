// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.encryptor;

import android.text.TextUtils;

public class CustomEncryptor implements IEncryptorType, IEncryptor {
    private final IEncryptor encryptor;
    private final String encryptorTypeStr;

    public CustomEncryptor(IEncryptor encryptor, String encryptorTypeStr) {
        this.encryptor = encryptor;
        this.encryptorTypeStr = encryptorTypeStr;
    }

    @Override
    public byte[] encrypt(byte[] data, int size) {
        return encryptor == null ? data : encryptor.encrypt(data, size);
    }

    @Override
    public String encryptorType() {
        return TextUtils.isEmpty(encryptorTypeStr) ? DEFAULT_ENCRYPTOR : encryptorTypeStr;
    }
}