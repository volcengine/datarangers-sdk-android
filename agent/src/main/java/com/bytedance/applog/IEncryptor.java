// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

public interface IEncryptor {

    byte[] encrypt(final byte[] data, final int size);
}
