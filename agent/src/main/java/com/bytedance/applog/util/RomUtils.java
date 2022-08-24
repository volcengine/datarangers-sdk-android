// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

import java.lang.reflect.Method;
import java.util.Locale;

public class RomUtils {
    private static final String SEPARATOR = "_";
    private static final String MIUI = "miui";
    private static final String EMUI = "emotionui";
    private static final String MAGIC_UI = "magicui";
    private static final String FLYME = "flyme";
    private static final String EUI = "eui";
    private static final String COLOROS = "coloros";
    private static final String HARMONY_UI = "harmony";

    private static final String RUNTIME_MIUI = "ro.miui.ui.version.name";
    private static final String RUNTIME_OPPO = "ro.build.version.opporom";
    private static final String MODEL_LETV = "ro.letv.release.version";
    private static final CharSequence SONY = "sony";
    private static final CharSequence AMIGO = "amigo";
    private static final CharSequence FUNTOUCHOS = "funtouch";
    private static final String FUNTOUCH_OS_VERSION = "ro.vivo.os.build.display.id";
    private static final String FOUTOUCH_OS_SOFTWARE_VERSION = "ro.vivo.product.version";
    private static final String KEY_360OS = "ro.build.uiversion";
    private static volatile Boolean IS_GMS_INSTALLED;

    public static String getEmuiInfo() {
        return getSystemProperty("ro.build.version.emui");
    }

    public static String getRomInfo() {
        // 小米
        if (isMiui()) {
            return getMIUIVersion();
        }
        //        //魅族
        if (isFlyme()) {
            return getFlymeVersion();
        }
        // oppo
        if (isColorOS()) {
            return getColorOsVersion();
        }
        // 华为
        String romVersion = getEMUVersion();
        if (!TextUtils.isEmpty(romVersion)) {
            return romVersion;
        }
        // vivo
        if (isFunTouchOS()) {
            return getFuntouchOSVersion();
        }

        // 金立
        if (isAmigo()) {
            return getAmigoVersion();
        }
        // 360
        if (is360OS()) {
            return get360OSVersion();
        }

        // 乐视
        romVersion = getEUIVersion();
        if (!TextUtils.isEmpty(romVersion)) {
            return romVersion;
        }

        return Build.DISPLAY;
    }

    public static String get360OSVersion() {
        return getSystemProperty(KEY_360OS) + SEPARATOR + Build.DISPLAY;
    }

    public static boolean is360OS() {
        String prop = (Build.MANUFACTURER + Build.BRAND);
        if (TextUtils.isEmpty(prop)) return false;
        prop = prop.toLowerCase();
        return prop.contains("360") || prop.contains("qiku");
    }

    public static String getFuntouchOSVersion() {
        return getSystemProperty(FUNTOUCH_OS_VERSION)
                + SEPARATOR
                + getSystemProperty(FOUTOUCH_OS_SOFTWARE_VERSION);
    }

    public static boolean isHwOrHonor(String emuiInfo) {
        if (TextUtils.isEmpty(emuiInfo)) {
            emuiInfo = getEmuiInfo();
        }
        if (!TextUtils.isEmpty(emuiInfo)
                && (emuiInfo.toLowerCase().contains(EMUI)
                        || emuiInfo.toLowerCase().contains(MAGIC_UI))) {
            return true;
        }
        return isHuaweiDevice() || isHonorDevice();
    }

    public static boolean isFlyme() {
        return !TextUtils.isEmpty(Build.DISPLAY) && Build.DISPLAY.contains("Flyme")
                || "flyme".equals(Build.USER);
    }

    public static boolean isFunTouchOS() {
        String funtouchOs = getSystemProperty(FUNTOUCH_OS_VERSION);
        return !TextUtils.isEmpty(funtouchOs) && funtouchOs.toLowerCase().contains(FUNTOUCHOS);
    }

    public static boolean isAmigo() {
        return !TextUtils.isEmpty(Build.DISPLAY) && Build.DISPLAY.toLowerCase().contains(AMIGO);
    }

    public static String getAmigoVersion() {
        return Build.DISPLAY + SEPARATOR + getSystemProperty("ro.gn.sv.version");
    }

    public static String getEUIVersion() {
        if (isEUI()) {
            return EUI + SEPARATOR + getSystemProperty(MODEL_LETV) + SEPARATOR + Build.DISPLAY;
        }
        return "";
    }

    public static boolean isEUI() {
        return !TextUtils.isEmpty(getSystemProperty(MODEL_LETV));
    }

    public static boolean isHuaweiDevice() {
        return !TextUtils.isEmpty(Build.BRAND) && Build.BRAND.toLowerCase().startsWith("huawei")
                || !TextUtils.isEmpty(Build.MANUFACTURER)
                        && Build.MANUFACTURER.toLowerCase().startsWith("huawei");
    }

    public static boolean isHonorDevice() {
        return !TextUtils.isEmpty(Build.BRAND) && Build.BRAND.toLowerCase().startsWith("honor")
                || !TextUtils.isEmpty(Build.MANUFACTURER)
                        && Build.MANUFACTURER.toLowerCase().startsWith("honor");
    }

    public static boolean isMiui() {
        try {
            Class<?> clz = Class.forName("miui.os.Build");
            return clz.getName().length() > 0;
        } catch (Throwable e) {
            // ignore
        }
        return false;
    }

    public static String getMIUIVersion() {
        if (isMiui()) {
            return MIUI
                    + SEPARATOR
                    + getSystemProperty(RUNTIME_MIUI)
                    + SEPARATOR
                    + Build.VERSION.INCREMENTAL;
        }
        return "";
    }

    public static boolean isSony() {
        String rom = Build.BRAND + Build.MANUFACTURER;
        return !TextUtils.isEmpty(rom) || rom.toLowerCase().contains(SONY);
    }

    /**
     * @return emu version
     */
    public static String getEMUVersion() {
        String emuiInfo = getEmuiInfo();
        if (emuiInfo != null
                && (emuiInfo.toLowerCase().contains(EMUI)
                        || emuiInfo.toLowerCase().contains(MAGIC_UI))) {
            return emuiInfo + SEPARATOR + Build.DISPLAY;
        }
        return "";
    }

    public static String getFlymeVersion() {
        String display = Build.DISPLAY;
        if (display != null && display.toLowerCase().contains(FLYME)) {
            return display;
        }
        return "";
    }

    public static boolean isColorOS() {
        String manufacturer = Build.MANUFACTURER;
        if (!TextUtils.isEmpty(manufacturer)) {
            return manufacturer.toLowerCase().contains("oppo")
                    || manufacturer.toLowerCase().contains("realme");
        }
        return false;
    }

    public static boolean isXiaomi() {
        // 小米手机、红米手机
        return Build.MANUFACTURER.equalsIgnoreCase("XIAOMI")
                || Build.BRAND.equalsIgnoreCase("XIAOMI")
                || Build.BRAND.equalsIgnoreCase("REDMI");
    }

    public static String getColorOsVersion() {
        if (isColorOS()) {
            return COLOROS
                    + SEPARATOR
                    + getSystemProperty(RUNTIME_OPPO)
                    + SEPARATOR
                    + Build.DISPLAY;
        }
        return "";
    }

    private static String getSystemProperty(String propName) {
        String value = SystemPropertiesWithCache.get(propName);
        if (!TextUtils.isEmpty(value)) {
            return value;
        }
        return Utils.getSysPropByExec(propName);
    }

    private static boolean isInstalledApp(Context context, final String packageName) {
        boolean installed = false;
        if (null != context && !TextUtils.isEmpty(packageName)) {
            PackageManager pm = context.getPackageManager();
            try {
                if (pm.getPackageInfo(packageName, 0) != null) installed = true;
            } catch (Throwable e) {
                // do nothing.
            }
        }
        return installed;
    }

    public static boolean isMeizu() {
        String brand = Build.BRAND;
        if (brand == null) {
            return false;
        }

        return brand.toLowerCase(Locale.ENGLISH).contains("meizu");
    }

    public static boolean isOnePlus() {
        return "OnePlus".equalsIgnoreCase(Build.MANUFACTURER);
    }

    public static boolean isSamsung() {
        if ("samsung".equalsIgnoreCase(Build.BRAND)
                || "samsung".equalsIgnoreCase(Build.MANUFACTURER)) {
            return true;
        }

        return false;
    }

    public static boolean isZTE() {
        return getManufacturer().toUpperCase().contains("ZTE");
    }

    private static String getManufacturer() {
        return (Build.MANUFACTURER) == null ? "" : (Build.MANUFACTURER).trim();
    }

    public static boolean isLenovo() {
        String fingerPrint = Build.FINGERPRINT;
        if (!TextUtils.isEmpty(fingerPrint)) {
            return fingerPrint.contains("VIBEUI_V2");
        }
        String a = getSystemProperty("ro.build.version.incremental");
        return !TextUtils.isEmpty(a) && a.contains("VIBEUI_V2");
    }

    public static boolean isNubia() {
        return getManufacturer().toUpperCase().contains("NUBIA");
    }

    public static boolean isASUS() {
        return getManufacturer().toUpperCase().contains("ASUS");
    }

    public static boolean isHuawei(Context context) {
        return getManufacturer().toUpperCase().contains("HUAWEI");
    }

    public static boolean isHarmonyUI() {
        return sIsHarmony.get();
    }

    private static final AbsSingleton<Boolean> sIsHarmony =
            new AbsSingleton<Boolean>() {
                @Override
                protected Boolean create(Object... params) {
                    try {
                        Class<?> clz = Class.forName("com.huawei.system.BuildEx");
                        Method method = clz.getMethod("getOsBrand");
                        return HARMONY_UI.equals(method.invoke(clz));
                    } catch (Throwable ignored) {

                    }
                    return false;
                }
            };
}
