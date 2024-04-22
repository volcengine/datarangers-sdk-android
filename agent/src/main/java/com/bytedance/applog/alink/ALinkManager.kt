// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.alink

import android.app.Application
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import androidx.annotation.WorkerThread
import com.bytedance.applog.AppLogHelper
import com.bytedance.applog.IDataObserver
import com.bytedance.applog.alink.ALinkCache.Companion.NO_EXPIRE
import com.bytedance.applog.alink.model.ALinkData
import com.bytedance.applog.alink.model.ALinkQueryParam
import com.bytedance.applog.alink.model.ApiResponse
import com.bytedance.applog.alink.model.AttributionData
import com.bytedance.applog.alink.model.AttributionRequest
import com.bytedance.applog.alink.model.BaseData
import com.bytedance.applog.alink.network.ApiService
import com.bytedance.applog.alink.util.LinkUtils
import com.bytedance.applog.engine.Engine
import com.bytedance.applog.log.LogInfo
import com.bytedance.applog.manager.DeviceManager
import com.bytedance.applog.server.Api
import com.bytedance.applog.store.EventV3
import org.json.JSONObject

private const val MSG_START_DDL = 0
private const val MSG_START_DL = 1

/**
 * ALink主要逻辑
 * @author chenguanzhong
 * @date 2021/1/28
 **/
class ALinkManager(engine: Engine) : Handler.Callback, IDataObserver {

    companion object {
        const val DEEP_LINK_CACHE = "deep_link"
        const val DEFERRED_LINK_CACHE = "deferred_deep_link"
        const val APP_CACHE = "app_cache"
        const val WEB_SSID_KEY = "tr_web_ssid"
        const val MONTH_MILLS: Long = 1000L * 60 * 60 * 24 * 30
        const val YEAR_MILLS: Long = 1000L * 60 * 60 * 24 * 365
        const val TR_TOKEN = "tr_token"
        const val TR_WEB_SSID = "\$tr_web_ssid"
    }

    private var mClipboardEnable = false
    private val mHandler: Handler by lazy {
        val handlerThread = HandlerThread("bd_tracker_alink")
        handlerThread.start()
        Handler(handlerThread.looper, this)
    }
    private var mEngine: Engine = engine
    private var cache: ALinkCache
    private var mDeepLinkRetryCount: Int = 0
    private var apiService: ApiService
    private var maxDeepLinkRetryCount = 10
    var deepLinkUrl: String? = null

    private val UTM_ATTRS = listOf(
        "utm_campaign",
        "utm_source",
        "utm_term",
        "utm_medium",
        "utm_content"
    )
    private val TRACE_DATA_ATTRS = listOf(
        "tr_shareuser",
        "tr_admaster",
        "tr_param1",
        "tr_param2",
        "tr_param3",
        "tr_param4",
        "reengagement_window",
        "reengagement_time",
        "is_retargeting"
    )

    init {
        val spName = AppLogHelper.getInstanceSpName(engine.appLog, ALinkCache.SP_NAME)
        cache = ALinkCache(
            engine.context as Application,
            spName
        )
        apiService = ApiService(engine.appLog)
    }

    /**
     * 生成归因请求参数
     */
    @WorkerThread
    private fun fillAttributionRequest(exitsAppCache: Boolean): AttributionRequest {
        val request = AttributionRequest()
        mEngine.dm?.apply {
            request.aid = aid
            request.os = "android"
            request.installId = iid
            request.androidId = openUdid
            request.imei = udid
            request.bdDid = bdDid
            request.googleAid = getHeaderValue(Api.KEY_GOOGLE_AID, null, String::class.java)
            request.ua = getHeaderValue(Api.KEY_USER_AGENT, null, String::class.java)
            request.deviceModel = getHeaderValue(Api.KEY_DEVICE_MODEL, null, String::class.java)
            request.osVersion = getHeaderValue(Api.KEY_OS_VERSION, null, String::class.java)
            request.isNewUser = isNewUser
            request.existAppCache = exitsAppCache
            request.appVersion = versionName
            request.channel = getHeaderValue(Api.KEY_CHANNEL, null, String::class.java)
        }
        return request
    }

    @WorkerThread
    private fun fillALinkQueryParams(queryParam: ALinkQueryParam) {
        with(queryParam) {
            os = "android"
            aid = mEngine.appLog.appId
            bdDid = mEngine.appLog.did
            ssid = mEngine.appLog.ssid
            userUniqueId = mEngine.appLog.userUniqueID
            androidId = mEngine.dm?.openUdid
            imei = mEngine.dm?.udid
            deviceModel = mEngine.dm?.getHeaderValue(Api.KEY_DEVICE_MODEL, null, String::class.java)
            osVersion = mEngine.dm?.getHeaderValue(Api.KEY_OS_VERSION, null, String::class.java)
            googleAid = mEngine.dm?.getHeaderValue(Api.KEY_GOOGLE_AID, null, String::class.java)
        }
    }

    fun mergeTracerData() {
        val traceDataAttrs = JSONObject()
        val utmAttrs = JSONObject()
        val linkData =
            cache.getData(DEEP_LINK_CACHE, ALinkData::class.java)
                ?.toJson()
        linkData?.run {
            for (key in UTM_ATTRS) {
                utmAttrs.put(key, optString(key, null))
            }
            for (key in TRACE_DATA_ATTRS) {
                if (key == "is_retargeting") {
                    traceDataAttrs.put(key, if (optBoolean(key)) 1 else 0)
                } else {
                    traceDataAttrs.put(key, optString(key, null))
                }
            }
            mEngine.dm?.setTracerData(traceDataAttrs)
            mEngine.dm?.setALinkUtmAttr(utmAttrs)
        }
        val webSsid: String? = cache.getString(WEB_SSID_KEY)
        if (!webSsid.isNullOrEmpty()) {
            mEngine.appLog.setHeaderInfo(TR_WEB_SSID, webSsid)
        }
    }

    /**
     * 设置剪贴板开关，默认关闭
     */
    fun setClipboardEnabled(clipboardEnable: Boolean) {
        mClipboardEnable = clipboardEnable
    }

    fun onDeepLinked(uri: Uri?) {
        mergeTracerData()
        uri?.run {
            deepLinkUrl = toString()
        }
        logger().debug(
            LogInfo.Category.ALINK,
            "Activate deep link with url: {}...",
            deepLinkUrl
        )
        mHandler.run {
            val params: JSONObject? = LinkUtils.getParamFromLink(uri)
            val queryParam = BaseData.fromJson(params, ALinkQueryParam::class.java)
            if (!queryParam?.token.isNullOrEmpty()) {
                mDeepLinkRetryCount = 0;
                val msg = obtainMessage(MSG_START_DL, queryParam)
                sendMessage(msg)
            }
        }
    }

    /**
     *  深度链接：用户主动触发
     */
    private fun doDeepLinked(queryParam: ALinkQueryParam?) {
        val token = queryParam?.token
        if (token.isNullOrEmpty()) {
            return
        }
        fillALinkQueryParams(queryParam)
        val response: ApiResponse<ALinkData>? = mEngine.uriConfig.alinkQueryUri?.run {
            apiService.queryALinkData(this, queryParam)
        }
        response?.data?.run {
            this.token = token
            cache.putData(DEEP_LINK_CACHE, this, MONTH_MILLS)
            JSONObject().run {
                put("\$link_type", "direct")
                put("\$deeplink_url", deepLinkUrl)
                mEngine.appLog.receive(EventV3("\$invoke", this))
            }
            mergeTracerData()
            mEngine.appLog.aLinkListener?.onALinkData(toMap(), null)
        }
    }

    /**
     * 延迟深度链接: 第一次设备注册成功后回调
     */
    private fun doDeferDeepLink(queryParam: ALinkQueryParam, exitsAppCache: Boolean) {
        with(queryParam) {
            aid = mEngine.appLog.appId
            bdDid = mEngine.appLog.did
            ssid = mEngine.appLog.ssid
            userUniqueId = mEngine.appLog.userUniqueID
            if (!abVersion.isNullOrEmpty()) {
                mEngine.appLog.setExternalAbVersion(abVersion ?: "")
            }
            if (!webSsid.isNullOrEmpty()) {
                cache.putString(
                    WEB_SSID_KEY,
                    webSsid,
                    YEAR_MILLS
                )
            }
        }
        val response: ApiResponse<AttributionData>? = mEngine.uriConfig.alinkAttributionUri?.run {
            apiService.queryAttributionData(
                this,
                fillAttributionRequest(exitsAppCache),
                queryParam
            )
        }
        when (val attributionData = response?.data) {
            null -> {
                fun deal(message: String?): String {
                    return when (message) {
                        null -> "DDL failed"
                        "success" -> "DDL response data empty"
                        else -> message
                    }
                }
                mEngine.appLog.aLinkListener?.onAttributionFailedCallback(
                    IllegalStateException(deal(response?.message))
                )
            }

            else -> {
                attributionData.run {
                    if (isFirstLaunch) {
                        isFirstLaunch = false
                        cache.putData(DEFERRED_LINK_CACHE, this, NO_EXPIRE)
                        JSONObject().run {
                            put("\$link_type", "deferred")
                            mEngine.appLog.receive(EventV3("\$invoke", this))
                        }
                        mEngine.appLog.aLinkListener?.onAttributionData(toMap(), null)
                    } else {
                        mEngine.appLog.aLinkListener?.onAttributionFailedCallback(
                            IllegalStateException("DDL has data but not firstLaunch")
                        )
                    }
                }
            }
        }
    }

    override fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            MSG_START_DL -> {
                if (mEngine.dm?.registerState != DeviceManager.STATE_EMPTY) {
                    val params = msg.obj as ALinkQueryParam
                    doDeepLinked(params)
                    return true
                }
                if (mDeepLinkRetryCount < maxDeepLinkRetryCount) {
                    mDeepLinkRetryCount++
                    logger().debug(
                        LogInfo.Category.ALINK,
                        "Retry do deep link delay for the {} times...",
                        mDeepLinkRetryCount
                    )
                    mHandler.run {
                        sendMessageDelayed(obtainMessage(msg.what, msg.obj), 500)
                    }
                } else {
                    logger().warn(
                        LogInfo.Category.ALINK,
                        "Retried max times to do deep link until AppLog ready"
                    )
                }
            }

            MSG_START_DDL -> {
                val clipData =
                    if (mClipboardEnable) LinkUtils.getParamFromClipboard(mEngine.context)
                    else JSONObject()
                logger().debug(
                    LogInfo.Category.ALINK,
                    "Start to do defer deeplink with data:{}...", clipData
                )
                val queryParam =
                    BaseData.fromJson(clipData ?: JSONObject(), ALinkQueryParam::class.java)
                queryParam?.run {
                    doDeferDeepLink(this, msg.obj as Boolean)
                }
            }
        }
        return true
    }

    fun destroy() {
        mHandler.run {
            removeCallbacksAndMessages(null)
            looper.quit()
        }
    }

    override fun onIdLoaded(did: String, iid: String, ssid: String) {
    }

    override fun onRemoteIdGet(
        changed: Boolean,
        oldDid: String?,
        newDid: String,
        oldIid: String,
        newIid: String,
        oldSsid: String,
        newSsid: String
    ) {
        // 设备注册成功后回调延迟深度链接
        mergeTracerData()
        val appCache: String? = cache.getString(APP_CACHE)
        val appCacheExists: Boolean = !appCache.isNullOrEmpty()

        // 首次安装：立即保存Cache状态，防止下次重复调用
        if (!appCacheExists) {
            cache.putString(APP_CACHE, APP_CACHE, NO_EXPIRE)
        }
        if (!appCacheExists || mEngine.isVersionCodeOrChannelUpdate) {
            mHandler.run {
                sendMessage(obtainMessage(MSG_START_DDL, appCacheExists))
            }
        }

        // 移除监听
        mEngine.appLog.removeDataObserver(this);
    }

    override fun onRemoteConfigGet(changed: Boolean, config: JSONObject?) {
    }

    override fun onRemoteAbConfigGet(changed: Boolean, abConfig: JSONObject) {
    }

    override fun onAbVidsChange(vids: String, extVids: String) {
    }

    private fun logger() = mEngine.appLog.logger
}