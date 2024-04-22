// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.engine;

import android.content.res.Configuration;

import com.bytedance.applog.bean.GpsLocationInfo;
import com.bytedance.applog.log.LogInfo;
import com.bytedance.applog.server.Api;
import com.bytedance.applog.store.BaseData;
import com.bytedance.applog.util.HardwareUtils;

import org.json.JSONObject;

import java.util.Collections;

/**
 * 生命周期钩子实现类
 *
 * @author luodong.seu
 */
public class LifeHookImpl implements LifeHook {

    private final Engine engine;

    public LifeHookImpl(Engine engine) {
        this.engine = engine;
    }

    @Override
    public void beforeEventSave(BaseData baseData) {
        try {
            JSONObject properties =
                    null != baseData.getProperties() ? baseData.getProperties() : new JSONObject();

            // 加载屏幕方向
            if (engine.getConfig().isScreenOrientationEnabled()) {
                int ori = HardwareUtils.getScreenOrientation(engine.getAppLog().getContext());
                properties.put(
                        Api.KEY_SCREEN_ORIENTATION,
                        ori == Configuration.ORIENTATION_LANDSCAPE ? "landscape" : "portrait");
            }

            // 添加GPS信息
            GpsLocationInfo location = engine.getAppLog().getGpsLocation();
            if (null != location) {
                properties.put(Api.KEY_GPS_LONGITUDE, location.getLongitude());
                properties.put(Api.KEY_GPS_LATITUDE, location.getLatitude());
                properties.put(Api.KEY_GPS_GCS, location.getGeoCoordinateSystem());
            }

            if (properties.length() > 0) {
                baseData.setProperties(properties);
            }
        } catch (Throwable e) {
            engine.getAppLog()
                    .getLogger()
                    .error(
                            LogInfo.Category.EVENT,
                            Collections.singletonList("LifeHook"),
                            "Do beforeEventSave failed",
                            e);
        }
    }
}
