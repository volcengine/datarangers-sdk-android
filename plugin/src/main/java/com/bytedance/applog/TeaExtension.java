package com.bytedance.applog;

/**
 * @author shiyanlong
 * @date 2019/1/11
 */
public class TeaExtension {
    public Iterable<String> blackList;

    /** 埋点采集的黑名单 */
    public Iterable<String> trackBlackList;

    /** 自动注入webview桥接器开关 */
    public Boolean autoInjectWebViewBridge;

    /* 关闭接口/类自动跟踪功能 */
    public Boolean disableAutoTrack;
}