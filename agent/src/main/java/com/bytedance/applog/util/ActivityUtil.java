// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author shiyanlong
 */
public final class ActivityUtil {

    @Nullable
    static Activity findActivity(@NonNull Context context) {
        if (!(context instanceof ContextWrapper)) {
            return null;
        } else {
            ContextWrapper current;
            Context parent;
            for (current = (ContextWrapper) context;
                 !(current instanceof Activity);
                 current = (ContextWrapper) parent) {
                parent = current.getBaseContext();
                if (!(parent instanceof ContextWrapper)) {
                    return null;
                }
            }
            return (Activity) current;
        }
    }

    @Nullable
    public static Activity findActivity(@Nullable View view) {
        if (null == view) {
            return null;
        }
        return findActivity(view.getContext());
    }
}
