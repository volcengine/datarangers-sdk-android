// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.event;

/**
 * 事件类型
 *
 * @author luodong.seu
 */
public class EventType {

    /** 所有事件 */
    public static final int EVENT_ALL = Integer.MAX_VALUE;

    /** 用户自定义埋点事件 */
    public static final int USER_EVENT = 1;

    /** Profile事件 */
    public static final int EVENT_PROFILE = 1 << 1;

    /** 页面事件 */
    public static final int EVENT_PAGE = 1 << 2;

    /** 点击事件 */
    public static final int EVENT_CLICK = 1 << 3;

    /** Launch事件 */
    //    public static final int EVENT_LAUNCH = 1 << 4;
    //
    //    /** Terminate事件 */
    //    public static final int EVENT_TERMINATE = 1 << 5;

    /**
     * 判断是否包含某个事件类型
     *
     * @param typeOptions 事件的可选项
     * @param eventType 指定的事件
     * @return true: 包含
     */
    public static boolean hasEventType(int typeOptions, int eventType) {
        return (typeOptions & eventType) != 0;
    }
}
