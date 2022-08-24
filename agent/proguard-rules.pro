# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
# -keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
# -renamesourcefileattribute SourceFile
# -keepparameternames
-keep class com.bytedance.applog.AppLog { public *; }
-keep public interface com.bytedance.applog.IAppLogInstance { public *; }
-keep public interface com.bytedance.applog.IDataObserver { *; }
-keep public interface com.bytedance.applog.IExtraParams { *; }
-keep public interface com.bytedance.applog.IPicker { *; }
-keep public interface com.bytedance.applog.IOaidObserver { *; }
-keep public interface com.bytedance.applog.IEventObserver { *; }
-keep public interface com.bytedance.applog.IDataObserver { *; }
-keep public interface com.bytedance.applog.ISessionObserver { *; }
-keep public interface com.bytedance.applog.ILogger { *; }
-keep public interface com.bytedance.applog.IHeaderCustomTimelyCallback { *; }
-keep public interface com.bytedance.applog.ISensitiveInfoProvider { *; }
-keep public interface com.bytedance.applog.alink.IALinkListener { *; }
-keep public interface com.bytedance.applog.IPageMeta { *; }
-keep public interface com.bytedance.applog.IActiveCustomParamsCallback { *; }
-keep public interface com.bytedance.applog.InitConfig$IpcDataChecker { *; }
-keep public interface com.bytedance.applog.event.IEventHandler { *; }
-keep public interface com.bytedance.applog.profile.UserProfileCallback { *; }
-keep public interface com.bytedance.applog.network.INetworkClient { *; }
-keep public interface com.bytedance.applog.IEncryptor { *; }
-keep public interface com.bytedance.dr.OaidApi { *; }

-keep @interface com.bytedance.applog.annotation.PageMeta { *; }
-keep public enum com.bytedance.applog.event.EventPolicy { *; }

-keep class com.bytedance.applog.IOaidObserver$Oaid { *; }
-keep class com.bytedance.applog.game.GameReportHelper { public *; }
-keep class com.bytedance.applog.game.WhalerGameHelper { *; }
-keep class com.bytedance.applog.game.WhalerGameHelper$Result { *; }
-keep class com.bytedance.applog.game.OhayooGameHelper { *; }
-keep class com.bytedance.applog.onekit.AnalyticsComponentRegistrar { *; }
-keep class com.bytedance.applog.onekit.DeviceComponentRegistrar { *; }
-keep class com.bytedance.applog.InitConfig { public *; }
-keep class com.bytedance.applog.UriConfig { public *; }
-keep class com.bytedance.applog.UriConfig$Builder { public *; }
-keep class com.bytedance.applog.tracker.Tracker { public *; }
-keep class com.bytedance.applog.picker.DomSender { public *; }
-keep class com.bytedance.applog.picker.Picker { public *; }
-keep class com.bytedance.applog.util.WebViewJsUtil { *; }
-keep class com.bytedance.applog.util.UriConstants { public *; }
-keep class com.bytedance.applog.util.SensitiveUtils { public *; }
-keep class com.bytedance.applog.util.HardwareUtils { public *; }
-keep class com.bytedance.applog.util.SimulateLaunchActivity { public *; }
-keep class com.bytedance.applog.util.GeoCoordinateSystemConst { public *; }
-keep class com.bytedance.applog.game.UnityPlugin { public *; }
-keep class com.bytedance.applog.network.RangersHttpException { *; }
-keep class com.bytedance.applog.encryptor.EncryptorUtil { public *; }
-keep class com.bytedance.applog.Level { public *; }
-keep class com.bytedance.applog.event.EventType {
    public static final <fields>;
}
-keep class com.bytedance.applog.exposure.ViewExposureManager { public *; }
-keep class com.bytedance.applog.exposure.ViewExposureData { public *; }
-keep class com.bytedance.applog.exposure.ViewExposureConfig { public *; }
-keep class com.bytedance.dr.OaidFactory { public *; }

# fataar不支持资源merge，这里忽略utility中的资源代。（agent没用DataUtils)。
-dontwarn com.bytedance.common.utility.R*

-repackageclasses com.bytedance.bdtracker

-keeppackagenames com.bytedance.dr.impl
-keeppackagenames com.bytedance.dr.aidl

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
