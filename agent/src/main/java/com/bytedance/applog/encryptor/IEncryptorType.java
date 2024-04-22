// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.encryptor;

import androidx.annotation.Keep;

@Keep
public interface IEncryptorType {
    String DEFAULT_ENCRYPTOR = "a";

    String encryptorType();
}
