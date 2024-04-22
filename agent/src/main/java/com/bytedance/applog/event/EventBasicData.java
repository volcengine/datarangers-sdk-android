// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.event;

import androidx.annotation.Keep;

import com.bytedance.applog.store.BaseData;

@Keep
public class EventBasicData {
    private final long eventIndex;
    private final long eventCreateTime;

    private final String sessionId;

    private final String uuid;
    private final String uuidType;
    private final String ssid;

    private final String abSdkVersion;

    EventBasicData(BaseData event) {
        this.eventIndex = event.eid;
        this.eventCreateTime = event.ts;
        this.sessionId = event.sid;
        this.uuid = event.uuid;
        this.uuidType = event.uuidType;
        this.ssid = event.ssid;
        this.abSdkVersion = event.abSdkVersion;
    }


    public long getEventIndex() {
        return eventIndex;
    }

    public long getEventCreateTime() {
        return eventCreateTime;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUuid() {
        return uuid;
    }

    public String getUuidType() {
        return uuidType;
    }

    public String getSsid() {
        return ssid;
    }

    public String getAbSdkVersion() {
        return abSdkVersion;
    }

    @Override
    public String toString() {
        return "EventBasisData{" +
                "eventIndex=" + eventIndex +
                ", eventCreateTime=" + eventCreateTime +
                ", sessionId='" + sessionId + '\'' +
                ", uuid='" + uuid + '\'' +
                ", uuidType='" + uuidType + '\'' +
                ", ssid='" + ssid + '\'' +
                ", abSdkVersion='" + abSdkVersion + '\'' +
                '}';
    }
}
