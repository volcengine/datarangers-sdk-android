// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.event;

/**
 * 全埋点事件类型
 *
 * @author luodong.seu
 */
public class AutoTrackEventType {

    /** 全部事件 */
    public static final int ALL = Integer.MAX_VALUE;

    /** 页面事件 */
    public static final int PAGE = 1 << 1;

    /** 点击事件 */
    public static final int CLICK = 1 << 2;

    /** 页面离开事件 */
    public static final int PAGE_LEAVE = 1 << 3;

    public static boolean hasEventType(int typeOptions, int eventType) {
        return (typeOptions & eventType) != 0;
    }
}
