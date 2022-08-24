// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.network;

import com.bytedance.applog.manager.ConfigManager;

/**
 * 限频&拥塞降级控制
 * Created by luoqiaoyou on 2020/1/14.
 */
public class CongestionController {

    /**
     * 留作扩展和测试，不同业务定义不同前缀
     */
    private String mPrefix;

    /**
     * 二维数组每一列含义：
     * col 0: 当前级别最大时间间隔
     * col 1: 当前级别升级所需要的最大连续发送成功次数
     * col 2: 当前级别最大时间间隔内允许的发送的次数
     */
    protected static final long[][] TABLE_INTERVAL_COUNT = {
            {2 * 60 * 1000L, 0, 12}, // 默认频率2分钟发送12次，平均10s间隔
            {2 * 60 * 1000L, 5, 1},
            {4 * 60 * 1000L, 5, 1},
            {8 * 60 * 1000L, 4, 1},
            {16 * 60 * 1000L, 2, 1}
    };

    private ConfigManager mConfig;

    private int mTableIndex;

    private int mHasSendCount;

    private int mContinueSuccSendCount;

    private long mLastSendTime;

    private long mLastGradeChangeTime;

    private static final long MAX_INTERVAL_UP_GRADE = 30 * 60 * 1000L;

    private static final long MAX_INTERVAL_DOWN_GRADE = 3 * 60 * 60 * 1000L;

    public CongestionController(String prefix, ConfigManager config) {
        mConfig = config;
        mPrefix = prefix;
        init();
    }

    protected void init() {
        mTableIndex = 0;
        long lastDowngradeTime = mConfig.getStatSp().getLong(mPrefix + "downgrade_time", 0L);
        // 上次降级策略保持3个小时有效，超过3个小时，重启后，恢复默认策略
        if (System.currentTimeMillis() - lastDowngradeTime < MAX_INTERVAL_DOWN_GRADE) {
            mTableIndex = mConfig.getStatSp().getInt(mPrefix + "downgrade_index", 0);
        } else {
            mConfig.getStatSp().edit().remove(mPrefix + "downgrade_time").remove(mPrefix + "downgrade_index").apply();
        }
    }

    public boolean isCanSend() {
        if (!enable()) {
            return true;
        }

        long curTime = System.currentTimeMillis();
        if ((curTime - mLastSendTime)
                >= TABLE_INTERVAL_COUNT[mTableIndex][0]) {
            // 达到或超出当前级别最大时间间隔，允许发送
            mHasSendCount = 1;
            mLastSendTime = curTime;
        } else if (mHasSendCount < TABLE_INTERVAL_COUNT[mTableIndex][2]) {
            // 未达到当前级别最大时间间隔内允许发送的最大次数，允许发送
            mHasSendCount++;
        } else {
            return false;
        }
        return true;
    }

    public void handleException() {
        if (!enable()) {
            return;
        }

        // 收到50x，快速降级
        if (mTableIndex < TABLE_INTERVAL_COUNT.length - 1) {
            downgrade();
        } else {
            mContinueSuccSendCount = 0;
        }
    }

    public void handleSuccess() {
        if (!enable()) {
            return;
        }

        long curTime = System.currentTimeMillis();
        if (mContinueSuccSendCount >= TABLE_INTERVAL_COUNT[mTableIndex][1]
                || (curTime - mLastGradeChangeTime) > MAX_INTERVAL_UP_GRADE) {
            // 当前降级策略，连续N次请求成功，或者距离上次降级时间间隔达到30分钟，升级当前策略，缩短间隔
            if (mTableIndex > 0) {
                upgrade();
            }
        } else {
            mContinueSuccSendCount++;
        }
    }

    private void upgrade() {
        long curTime = System.currentTimeMillis();
        mTableIndex--;
        mHasSendCount = 1;
        mContinueSuccSendCount = 1;
        mLastSendTime = curTime;
        mLastGradeChangeTime = curTime;
        mConfig.getStatSp().edit().putLong(mPrefix + "downgrade_time", curTime)
                .putInt(mPrefix + "downgrade_index", mTableIndex).apply();
    }

    private void downgrade() {
        long curTime = System.currentTimeMillis();
        mTableIndex++;
        mHasSendCount = 1;
        mContinueSuccSendCount = 0;
        mLastSendTime = curTime;
        mLastGradeChangeTime = curTime;
        mConfig.getStatSp().edit().putLong(mPrefix + "downgrade_time", curTime)
                .putInt(mPrefix + "downgrade_index", mTableIndex).apply();
    }

    private boolean enable() {
        return mConfig.getInitConfig().isCongestionControlEnable();
    }
}
