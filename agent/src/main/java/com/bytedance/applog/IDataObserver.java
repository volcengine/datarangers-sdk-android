// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import org.json.JSONObject;

/**
 * register，config，abconfig相关数据本地加载和server加载的回调通知
 * @author linguoqing
 */
public interface IDataObserver {
    /**
     * 本地的id数据加载结果通知
     * @param did device id
     * @param iid install id
     * @param ssid 数说id
     */
    void onIdLoaded(String did, String iid, String ssid);

    /**
     * 通知注册结果，以及id变化情况
     * 仅主进程会被调用
     * @param changed 是否和本地缓存有所不同
     * @param did server返回新的device id
     * @param iid server返回新install id
     * @param ssid server返回新数说id
     */
    void onRemoteIdGet(boolean changed, String oldDid, String newDid, String oldIid, String newIid, String oldSsid, String newSsid);

    /**
     * Config拉取数据，和本地数据对比有变化的通知
     * 仅主进程会被调用
     * @param changed 是否和本地缓存有所不同
     * @param config server返回新config内容
     */
    void onRemoteConfigGet(boolean changed, JSONObject config);

    /**
     * server拉取AbConfig数据，和本地数据对比有变化的通知
     * 仅主进程会被调用
     * @param changed 是否和本地缓存有所不同
     * @param abConfig server返回新abConfig内容
     */
    void onRemoteAbConfigGet(boolean changed, JSONObject abConfig);

    /**
     * 客户端ab实验曝光vid集合发生变化的通知
     * @param vids 客户端ab实验曝光vid集合
     * @param extVids 外部设置的external vid集合
     */
    void onAbVidsChange(String vids, String extVids);
}
