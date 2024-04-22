// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.engine;

import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.util.NetworkUtils;

import org.json.JSONException;

/**
 * @author shiyanlong
 * @date 2019/2/2
 */
public abstract class BaseWorker {

    private int mFailCount;

    private volatile boolean mImmediately;

    private long mLastTime;

    private volatile boolean mStop;

    protected final Engine mEngine;
    protected final AppLogInstance appLogInstance;

    public BaseWorker(final Engine engine) {
        mEngine = engine;
        appLogInstance = engine.getAppLog();
    }

    BaseWorker(final Engine engine, final long lastTime) {
        this(engine);
        mLastTime = lastTime;
    }

    public void setStop(boolean stop) {
        mStop = stop;
    }

    boolean isStop() {
        return mStop;
    }

    /**
     * 检查是否该干活了，如果到时间了，则干活。
     *
     * @return next time to check
     */
    final long checkToWork() {
        long nextTime = checkWorkTime();
        if (nextTime <= System.currentTimeMillis()) {
            nextTime = work();
        }
        return nextTime;
    }

    private long checkWorkTime() {
        long nextTime;
        if (needNet() && !NetworkUtils.isNetworkAvailableFast(mEngine.getContext(), mEngine.isResume())) {
            mEngine.getAppLog().getLogger().debug("Check work time is not net available.");
            nextTime = System.currentTimeMillis() + Engine.TIME_CHECK_INTERVAL;
        } else {
            long interval = 0;
            if (mImmediately) {
                mLastTime = 0;
                mImmediately = false;
            } else if (mFailCount > 0) {
                interval = getFailInterval(mFailCount - 1);
            } else {
                interval = nextInterval();
            }
            nextTime = mLastTime + interval;
        }
        return nextTime;
    }

    private long work() {
        boolean worked = false;
        mEngine.getAppLog().getLogger().debug("The worker:{} start to work...", this.getName());
        try {
            worked = doWork();
        } catch (Throwable e) {
            mEngine.getAppLog().getLogger().error("Work do failed.", e);
        } finally {
            mLastTime = System.currentTimeMillis();
            if (worked) {
                mFailCount = 0;
            } else {
                ++mFailCount;
            }
            mEngine.getAppLog()
                    .getLogger()
                    .debug(
                            "The worker:{} worked:{}.",
                            this.getName(),
                            worked ? "success" : "failed");
        }
        return checkWorkTime();
    }

    private long getFailInterval(int failCount) {
        long[] intervals = getRetryIntervals();
        return intervals[failCount % intervals.length];
    }

    /**
     * 是否需要网络
     *
     * @return 是否需要网络
     */
    protected abstract boolean needNet();

    /**
     * 返回应该工作的时间
     *
     * @return 应该执行的时间，必须大于{System.currentTimeMillis}
     */
    protected abstract long nextInterval();

    /**
     * 返回失败重试的间隔，长度必须大于0
     *
     * @return 失败重试的时间间隔
     */
    protected abstract long[] getRetryIntervals();

    /**
     * 干活
     *
     * @return 干好了么？如果false，则会重试。
     * @throws JSONException 允许抛出一些异常？
     */
    protected abstract boolean doWork() throws JSONException;

    /**
     * 返回worker的名称
     *
     * @return worker的名称
     */
    protected abstract String getName();

    /** 设置是否只执行一次 */
    <T extends BaseWorker> T setImmediately() {
        mImmediately = true;
        return (T) this;
    }
}
