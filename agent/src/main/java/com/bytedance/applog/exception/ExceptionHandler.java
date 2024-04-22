// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.exception;

import androidx.annotation.NonNull;

import com.bytedance.applog.AppLogHelper;
import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.collector.Navigator;
import com.bytedance.applog.log.LoggerImpl;
import com.bytedance.applog.store.EventV3;
import com.bytedance.applog.util.Utils;

import org.json.JSONObject;

import java.util.Collections;

/**
 * 异常处理类
 *
 * @author luodong.seu
 */
public final class ExceptionHandler implements Thread.UncaughtExceptionHandler {
    public static final String CRASH_EVENT = "$crash";

    private static volatile ExceptionHandler globalHandler;
    private final Thread.UncaughtExceptionHandler defaultHandler;

    /** 初始化，全局初始化一次 */
    public static synchronized void init() {
        if (null == globalHandler) {
            globalHandler = new ExceptionHandler();
        }
    }

    private ExceptionHandler() {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull final Throwable e) {
        final long time = System.currentTimeMillis();
        final AppLogHelper.AppLogInstanceMatcher matcher =
                new AppLogHelper.AppLogInstanceMatcher() {
                    @Override
                    public boolean match(AppLogInstance instance) {
                        return null != instance.getInitConfig()
                                && AppCrashType.hasJavaCrashType(
                                        instance.getInitConfig().getTrackCrashType());
                    }
                };
        boolean hasTrack = AppLogHelper.matchInstance(matcher);
        if (!hasTrack) {
            handleException(t, e);
            return;
        }

        // 采集$crash事件
        JSONObject eventProps = new JSONObject();
        try {
            eventProps.put("$is_backstage", !Navigator.isAppInFrontend());
            eventProps.put("$event_time", time);
            eventProps.put("$crash_thread", t.getName());
            eventProps.put("$crash_process", Utils.getProcessName());
            eventProps.put("$detailed_stack", getExceptionStackString(e));
        } catch (Throwable throwable) {
            LoggerImpl.global()
                    .error(
                            Collections.singletonList("ExceptionHandler"),
                            "Collect crash params failed",
                            throwable);
        }
        final EventV3 crashEvent = new EventV3(CRASH_EVENT, eventProps);
        AppLogHelper.handleAll(
                new AppLogHelper.AppLogInstanceHandler() {
                    @Override
                    public void handle(AppLogInstance instance) {
                        // SDK 异常采集
                        if (matcher.match(instance)) {
                            instance.receive(crashEvent);

                            // flush保证event采集入库
                            instance.flush();
                        }
                    }
                });

        handleException(t, e);
    }

    /**
     * 获取堆栈信息
     *
     * @param e Throwable
     * @return String
     */
    private String getExceptionStackString(Throwable e) {
        Throwable cause = e;
        StringBuilder stringBuilder = new StringBuilder();
        while (null != cause) {
            appendStackString(stringBuilder, cause);
            cause = cause.getCause();
        }
        return stringBuilder.toString();
    }

    /**
     * 追加cause的堆栈信息
     *
     * @param sb StringBuilder
     * @param cause Cause
     */
    private void appendStackString(StringBuilder sb, Throwable cause) {
        sb.append(cause.toString());
        StackTraceElement[] trace = cause.getStackTrace();
        for (StackTraceElement traceElement : trace) {
            sb.append("\n\tat ").append(traceElement);
        }
    }

    /** 处理exception */
    private void handleException(@NonNull Thread t, @NonNull Throwable e) {
        if (null != defaultHandler) {
            defaultHandler.uncaughtException(t, e);
        } else {
            // kill app
            try {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(10);
            } catch (Throwable ignored) {
            }
        }
    }
}
