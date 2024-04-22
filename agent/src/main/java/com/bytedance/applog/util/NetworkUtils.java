// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

import com.bytedance.applog.log.LoggerImpl;
import com.bytedance.applog.network.NetworkConnectChangeReceiver;

/**
 * fork from com.bytedance.component.silk.road:mohist-standard-tools:0.0.19
 *
 * <p>解决安全合规问题，删除了不必要的代码
 *
 * @author luodong.seu
 */
public class NetworkUtils {

    // 每2s请求一下系统接口，防止网络类型缓存有问题
    @SuppressWarnings("MagicNumber")
    private static volatile long sInterval = 2000L;

    private static long lastAdjustTime = 0L;
    private static NetworkType sNetworkType = NetworkType.UNKNOWN;
    private static boolean sIsReceiverRegisted = false;

    /** 网络类型 */
    public enum NetworkType {
        // 初始状态
        UNKNOWN(-1),
        NONE(0),
        MOBILE(1),
        MOBILE_2G(2),
        MOBILE_3G(3),
        WIFI(4),
        MOBILE_4G(5),
        MOBILE_5G(6),
        WIFI_24GHZ(7),
        WIFI_5GHZ(8),
        MOBILE_3G_H(9),
        MOBILE_3G_HP(10);

        NetworkType(int ni) {
            nativeInt = ni;
        }

        public int getValue() {
            return nativeInt;
        }

        final int nativeInt;

        public boolean is2G() {
            return this == MOBILE || this == MOBILE_2G;
        }

        public boolean isWifi() {
            return this == WIFI;
        }

        /** 判断是否是4G或者高于4G */
        public boolean is4GOrHigher() {
            return this == MOBILE_4G || this == MOBILE_5G;
        }

        /** 判断网络类型是否高于3G */
        @SuppressWarnings("BooleanExpressionComplexity")
        public boolean is3GOrHigher() {
            return this == MOBILE_3G
                    || this == MOBILE_3G_H
                    || this == MOBILE_3G_HP
                    || this == MOBILE_4G
                    || this == MOBILE_5G;
        }

        public boolean isAvailable() {
            return this != UNKNOWN && this != NONE;
        }
    }

    public static void setNetworkType(NetworkType type) {
        sNetworkType = type;
    }

    public static String getNetworkAccessType(Context context) {
        return getNetworkAccessType(context, true);
    }

    public static String getNetworkAccessType(Context context, boolean isResume) {
        return getNetworkAccessType(getNetworkTypeFast(context, isResume));
    }

    /**
     * get network access type
     *
     * @param nt NetworkType
     * @return String
     */
    public static String getNetworkAccessType(NetworkType nt) {
        String access = "";

        if (nt == NetworkType.WIFI) {
            access = "wifi";
        } else if (nt == NetworkType.WIFI_24GHZ) {
            // wifi 2.4G
            access = "wifi24ghz";
        } else if (nt == NetworkType.WIFI_5GHZ) {
            // wifi 5G
            access = "wifi5ghz";
        } else if (nt == NetworkType.MOBILE_2G) {
            access = "2g";
        } else if (nt == NetworkType.MOBILE_3G) {
            access = "3g";
        } else if (nt == NetworkType.MOBILE_3G_H) {
            // 3.5G
            access = "3gh";
        } else if (nt == NetworkType.MOBILE_3G_HP) {
            // 3.75G
            access = "3ghp";
        } else if (nt == NetworkType.MOBILE_4G) {
            access = "4g";
        } else if (nt == NetworkType.MOBILE_5G) {
            access = "5g";
        } else if (nt == NetworkType.MOBILE) {
            access = "mobile";
        }

        return access;
    }

    public static NetworkType getNetworkType(Context context) {
        try {
            ConnectivityManager manager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = manager.getActiveNetworkInfo();
            if (info == null || !info.isAvailable()) {
                return NetworkType.NONE;
            }
            int type = info.getType();
            if (ConnectivityManager.TYPE_WIFI == type) {
                return NetworkType.WIFI;
            } else if (ConnectivityManager.TYPE_MOBILE == type) {
                TelephonyManager mgr =
                        (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (mgr == null) {
                    return NetworkType.NONE;
                }
                switch (mgr.getNetworkType()) {
                    case TelephonyManager.NETWORK_TYPE_UMTS:
                    case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    case TelephonyManager.NETWORK_TYPE_HSDPA:
                    case TelephonyManager.NETWORK_TYPE_HSUPA:
                    case TelephonyManager.NETWORK_TYPE_HSPA:
                    case TelephonyManager.NETWORK_TYPE_EVDO_B:
                    case TelephonyManager.NETWORK_TYPE_EHRPD:
                    case TelephonyManager.NETWORK_TYPE_HSPAP:
                        return NetworkType.MOBILE_3G;
                    case TelephonyManager.NETWORK_TYPE_LTE:
                        return NetworkType.MOBILE_4G;
                    case TelephonyManager.NETWORK_TYPE_NR:
                        return NetworkType.MOBILE_5G;
                    default:
                        return NetworkType.MOBILE;
                }
            } else {
                return NetworkType.MOBILE;
            }
        } catch (Throwable e) {
            return NetworkType.MOBILE;
        }
    }

    public static NetworkType getNetworkTypeFast(Context context) {
        return getNetworkTypeFast(context, true);
    }

    private static NetworkType getNetworkTypeFast(Context context, boolean isResume) {
        checkNetworkTypeInit(context);
        if (isResume) {
            adjustNetwork(context);
        }
        return sNetworkType;
    }

    public static boolean isNetworkAvailableFast(Context context, boolean isResume) {
        return getNetworkTypeFast(context, isResume).isAvailable();
    }

    public static boolean isNetworkAvailable(Context context) {
        try {
            ConnectivityManager manager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = manager.getActiveNetworkInfo();
            return (info != null && info.isConnected());
        } catch (Throwable e) {
            // ignore
        }
        return false;
    }

    private static void checkNetworkTypeInit(Context context) {
        registerReceiver(context);
        if (sNetworkType == NetworkType.UNKNOWN) {
            sNetworkType = getNetworkType(context);
        }
    }

    private static void registerReceiver(Context context) {
        if (!sIsReceiverRegisted && context != null) {
            try {
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
                filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
                filter.addAction("android.net.wifi.STATE_CHANGE");
                context.getApplicationContext()
                        .registerReceiver(new NetworkConnectChangeReceiver(), filter);
            } catch (Throwable e) {
                LoggerImpl.global().debug("registerReceiver failed, because: " + e.getMessage());
            } finally {
                sIsReceiverRegisted = true;
            }
        }
    }

    private static void adjustNetwork(Context context) {
        if ((System.currentTimeMillis() - lastAdjustTime) > sInterval) {
            sNetworkType = getNetworkType(context);
            lastAdjustTime = System.currentTimeMillis();
        }
    }
}
