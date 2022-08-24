// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.event;

import android.os.SystemClock;

import com.bytedance.applog.util.TLog;

/**
 * 时长事件对象
 *
 * @author luodong.seu
 */
public class DurationEvent {

    /** 事件名 */
    private final String event;

    /** 开始时间 */
    private long startTime = -1L;

    /** 时长 */
    private long duration = 0L;

    public DurationEvent(String event) {
        this.event = event;
    }

    /** 开始计时 */
    public void start(long time) {
        startTime = time;
        TLog.d("[DurationEvent:{}] Start at:{}", event, time);
    }

    /** 继续计时 */
    public void resume(long time) {
        if (time > 0 && startTime < 0) {
            start(time);
            TLog.d("[DurationEvent:{}] Resume at:{}", event, time);
        }
    }

    /** 暂停计时 */
    public void pause(long time) {
        if (time > 0 && startTime > 0) {
            TLog.d("[DurationEvent:{}] Pause at:{}", event, time);
            duration += (time > startTime ? time : SystemClock.elapsedRealtime()) - startTime;
            startTime = -1L;
        }
    }

    /**
     * 结束计时
     *
     * @return 时长
     */
    public long end(long time) {
        if (time <= 0) {
            TLog.w("End at illegal time: " + time);
            return 0;
        }
        pause(time);
        TLog.d("[DurationEvent:{}] End[ at:{} and duration is {}ms.", event, time, duration);
        return duration;
    }
}
