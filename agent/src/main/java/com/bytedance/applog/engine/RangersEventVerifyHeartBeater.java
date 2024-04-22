// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.engine;

public class RangersEventVerifyHeartBeater extends BaseWorker {

    private static final long INTERVAL_EVENT_HEART_BEAT = 1000L;
    private static final long[] DEFAULT_RETRY_INTERVALS = new long[] {INTERVAL_EVENT_HEART_BEAT};

    private final String mCookie;

    private int mRetryCount = 0;

    public RangersEventVerifyHeartBeater(final Engine engine, final String cookie) {
        super(engine);
        mCookie = cookie;
    }

    public String getCookie() {
        return mCookie;
    }

    @Override
    protected boolean needNet() {
        return true;
    }

    @Override
    protected long nextInterval() {
        return INTERVAL_EVENT_HEART_BEAT;
    }

    @Override
    protected long[] getRetryIntervals() {
        return DEFAULT_RETRY_INTERVALS;
    }

    @Override
    protected boolean doWork() {
        boolean isSuccess = appLogInstance.getApi().sendToRangersEventVerify(null, mCookie);
        mRetryCount = isSuccess ? 0 : mRetryCount + 1;
        if (mRetryCount > 3) {
            appLogInstance.setRangersEventVerifyEnable(false, mCookie);
        }
        return true;
    }

    @Override
    protected String getName() {
        return "RangersEventVerify";
    }
}
