// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.text.TextUtils;

import com.bytedance.applog.server.Api;

import java.io.File;
import java.util.ArrayList;

public class DeprecatedFileCleaner {
    private final ArrayList<String> mDeprecatedFileDir;

    DeprecatedFileCleaner() {
        mDeprecatedFileDir = new ArrayList<>();
    }

    void add(String path) {
        mDeprecatedFileDir.add(path);
    }

    void execute() {
        Runnable runnable =
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            for (int i = 0; i < mDeprecatedFileDir.size(); i++) {
                                String[] deprecatedFile =
                                        new String[] {
                                            Api.KEY_OPEN_UDID,
                                            Api.KEY_C_UDID,
                                            Api.KEY_SERIAL_NUMBER,
                                            Api.KEY_SIM_SERIAL_NUMBER,
                                            Api.KEY_UDID,
                                            Api.KEY_DEVICE_ID
                                        };
                                for (String file : deprecatedFile) {
                                    try {
                                        clearDeprecatedFile(mDeprecatedFileDir.get(i), file);
                                    } catch (Exception e) {
                                        TLog.e(e);
                                    }
                                }
                            }
                        } catch (Exception ignored) {

                        }
                    }
                };
        new Thread(runnable).start();
    }

    private void clearDeprecatedFile(String cacheDir, String key) {
        if (TextUtils.isEmpty(cacheDir)) return;
        String path = cacheDir + File.separator + key + ".dat";
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
    }
}
