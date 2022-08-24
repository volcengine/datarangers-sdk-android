// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.manager;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 需要加载的项目太多。拆分出多个加载类，好处是：
 * 1. 代码隔离，互不影响，且各自清晰。
 * 2. 扩展性好些：新增Loader，不影响已有实现。
 * 3. 统一处理"重试"、"可选"逻辑。
 */
abstract class BaseLoader {

    private static final String UTF_8 = "UTF-8";

    boolean mReady;

    boolean mOptional;

    boolean mShouldUpdate;

    /**
     * 子进程是否需要定期load更新数值
     */
    boolean syncFromSub;

    BaseLoader(final boolean optional, final boolean shouldUpdate) {
        mOptional = optional;
        mShouldUpdate = shouldUpdate;
        syncFromSub = false;
    }

    BaseLoader(final boolean optional, final boolean shouldUpdate, final boolean needSyncFromSub) {
        mOptional = optional;
        mShouldUpdate = shouldUpdate;
        syncFromSub = needSyncFromSub;
    }

    /**
     * 加载信息，填充到info中
     *
     * @param info 填充的目标
     * @return 是否加载成功
     * @throws JSONException     不处理的异常
     * @throws SecurityException 需要用户授权。外部仅在首次登录时处理，最多等1次。
     */
    protected abstract boolean doLoad(JSONObject info) throws JSONException, SecurityException;

}
