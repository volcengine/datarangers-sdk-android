// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.manager;

import android.content.Context;
import com.bytedance.applog.server.Api;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author shiyanlong
 * @date 2019/2/2
 **/
class LocaleLoader extends BaseLoader {

    private final Context mApp;

    LocaleLoader(Context ctx) {
        super(true, true);
        mApp = ctx;
    }

    @Override
    protected boolean doLoad(final JSONObject info) throws JSONException {
        String lang = mApp.getResources().getConfiguration().locale.getLanguage();
        DeviceManager.putString(info, Api.KEY_LANGUAGE, lang);

        TimeZone tz = TimeZone.getDefault();
        int offset = tz.getRawOffset() / (3600 * 1000);
        if (offset < -12) {
            offset = -12;
        }
        if (offset > 12) {
            offset = 12;
        }
        info.put(Api.KEY_TIMEZONE, offset);

        DeviceManager.putString(info, Api.KEY_REGION, Locale.getDefault().getCountry());

        final TimeZone zone = Calendar.getInstance().getTimeZone();
        DeviceManager.putString(info, Api.KEY_TZ_NAME, zone.getID());

        int tzOffset = zone.getOffset(System.currentTimeMillis()) / 1000;
        info.put(Api.KEY_TZ_OFFSET, tzOffset);
        return true;
    }
}
