// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.bean;

/**
 * 位置信息
 *
 * @author luodong
 */
public class GpsLocationInfo {
    private float longitude;
    private float latitude;
    private String geoCoordinateSystem;

    public GpsLocationInfo(float longitude, float latitude, String geoCoordinateSystem) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.geoCoordinateSystem = geoCoordinateSystem;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public String getGeoCoordinateSystem() {
        return geoCoordinateSystem;
    }

    public void setGeoCoordinateSystem(String geoCoordinateSystem) {
        this.geoCoordinateSystem = geoCoordinateSystem;
    }
}
