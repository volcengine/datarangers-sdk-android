// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import com.bytedance.applog.BuildConfig;
import com.bytedance.applog.ILogger;

/**
 * @author shiyanlong
 * @date 2019/1/16
 */
public class TLog {

    public static final String SDK_VERSION_NAME = BuildConfig.VERSION_NAME;
    public static boolean DEBUG = BuildConfig.DEBUG;
    public static final int SDK_VERSION = BuildConfig.VERSION_CODE;
    public static final String TAG = "AppLog";

    private static ILogger sLogger;
    private static boolean sLogEnable;
    private static final String USNP = "U SHALL NOT PASS!";
    private static final char CHAR_PLACEHOLDER = ' ';

    @SuppressWarnings("PointlessArithmeticExpression")
    public static final int SDK_VERSION_CODE;

    static {
        // 4.0.0以上才设置bddid,且保持自增
        char firstNum = String.valueOf(SDK_VERSION).charAt(0);
        if (firstNum >= '4') {
            SDK_VERSION_CODE = 9999999 + (SDK_VERSION - 400);
        } else {
            SDK_VERSION_CODE = SDK_VERSION;
        }
    }

    /** 添加日志接口，优化日志打印，无须打印时不执行函数 */
    public interface LogGetter {
        String log();
    }

    public static void setLogger(final Context context, ILogger logger, boolean logEnable) {
        try {
            DEBUG = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (Throwable t) {
            DEBUG = true;
        }
        sLogger = logger;
        sLogEnable = logEnable;
    }

    public static void d(final String msg) {
        if (DEBUG && sLogEnable) {
            if (sLogger != null) {
                sLogger.log(msg, null);
            } else {
                Log.d(TAG, msg, null);
            }
        }
    }

    /** 支持占位符变量写法：TLog.d("xxx {} xxx", obj); */
    public static void d(final String msgPattern, Object... args) {
        if (DEBUG && sLogEnable && null != msgPattern) {
            String msg = format(msgPattern, args);
            if (sLogger != null) {
                sLogger.log(msg, null);
            } else {
                Log.d(TAG, msg, null);
            }
        }
    }

    public static void d(final LogGetter msg) {
        if (DEBUG && sLogEnable && null != msg) {
            if (sLogger != null) {
                sLogger.log(msg.log(), null);
            } else {
                Log.d(TAG, msg.log(), null);
            }
        }
    }

    public static void w(final String msg) {
        w(msg, null);
    }

    public static void w(final String msg, final Throwable t) {
        if (sLogEnable) {
            if (sLogger != null) {
                sLogger.log(msg, t);
            } else if (DEBUG) {
                Log.w(TAG, msg, t);
            }
        }
    }

    public static void e(final Throwable t) {
        if (sLogEnable) {
            if (sLogger != null) {
                sLogger.log("", t);
            } else if (DEBUG) {
                Log.e(TAG, "", t);
            }
        }
    }

    public static boolean e(final String msg, final Throwable t) {
        if (sLogEnable) {
            if (sLogger != null) {
                sLogger.log(msg, t);
            } else if (DEBUG) {
                Log.e(TAG, msg, t);
            }
            return true;
        }
        return false;
    }

    public static void i(final String msg) {
        if (sLogEnable) {
            if (sLogger != null) {
                sLogger.log(msg, null);
            } else {
                Log.i(TAG, msg, null);
            }
        }
    }

    /** 支持占位符写法： TLog.i("xxx {} xxx", obj); */
    public static void i(final String msgPattern, Object... args) {
        if (sLogEnable) {
            String msg = format(msgPattern, args);
            if (sLogger != null) {
                sLogger.log(msg, null);
            } else if (DEBUG) {
                Log.i(TAG, msg, null);
            }
        }
    }

    public static void i(final LogGetter msg) {
        i(msg, null);
    }

    public static void i(final LogGetter msg, final Throwable t) {
        if (sLogEnable && null != msg) {
            if (sLogger != null) {
                sLogger.log(msg.log(), t);
            } else if (DEBUG) {
                Log.i(TAG, msg.log(), t);
            }
        }
    }

    public static void ysnp(String msg, Throwable t) {
        e(msg, t);
        if (BuildConfig.DEBUG) {
            if (t != null) {
                throw new RuntimeException(msg, t);
            } else {
                throw new RuntimeException(msg);
            }
        }
    }

    public static void ysnp(Throwable t) {
        ysnp(USNP, t);
    }

    /**
     * 格式化消息体
     *
     * @param textWithPlaceholders 带placeholder:{}的文本
     * @param args 一个placeholder对应一个对象
     * @return args String化填充的文本
     *
     * <code>
     * ```
     * format("This is a test for object:{} and {} format output.", true, new JSONObject("{\"k\":1}"));
     *
     * // output: This is a test for object:true and {"k":1} format output
     * ```
     */
    public static String format(final String textWithPlaceholders, final Object... args) {
        try {
            StringBuilder toStringBuilder = new StringBuilder();
            if (null == args || args.length == 0 || !textWithPlaceholders.contains("{}")) {
                toStringBuilder.append(textWithPlaceholders);
            } else {
                int argsIndex = 0;
                int length = textWithPlaceholders.length();
                for (int i = 0; i < length; i++) {
                    char first = textWithPlaceholders.charAt(i);
                    char next =
                            i < length - 1 ? textWithPlaceholders.charAt(i + 1) : CHAR_PLACEHOLDER;
                    if (first == '{' && next == '}') {
                        if (argsIndex < args.length) {
                            toStringBuilder.append(Utils.toString(args[argsIndex++]));
                        }
                        i++;
                    } else {
                        toStringBuilder.append(first);
                    }
                }
            }
            return toStringBuilder.toString();
        } catch (Throwable e) {
            return textWithPlaceholders;
        }
    }
}
