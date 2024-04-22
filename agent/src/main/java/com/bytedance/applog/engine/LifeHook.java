// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.engine;

import com.bytedance.applog.store.BaseData;

/**
 * 生命钩子
 *
 * @author luodong.seu
 */
public interface LifeHook {

    /**
     * 事件存储之前回调
     *
     * <pre>
     *     可以修改事件属性等
     * </pre>
     *
     * @param baseData BaseData
     */
    void beforeEventSave(BaseData baseData);
}
