// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.event;

/**
 * 事件策略
 *
 * @author luodong.seu
 */
public enum EventPolicy {
    /** 入库 */
    ACCEPT,

    /** 丢弃 */
    DENY,
}
