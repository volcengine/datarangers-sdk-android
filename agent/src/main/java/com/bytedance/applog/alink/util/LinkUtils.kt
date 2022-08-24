// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.alink.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.util.Base64
import com.bytedance.applog.alink.ALinkManager
import org.json.JSONObject

object LinkUtils {

    /**
     * 获取Uri中携带的参数
     * 若没有?形式的query，返回最后一段path
     */
    fun getParamFromLink(uri: Uri?): JSONObject? {
        return try {
            val params = JSONObject()
            uri?.run {
                val scheme: String? = scheme
                if (scheme == "http" || scheme == "https") {
                    params.put(ALinkManager.TR_TOKEN, lastPathSegment)
                }
                for (queryName in queryParameterNames) {
                    params.put(queryName, getQueryParameter(queryName))
                }
            }
            params
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * 从剪贴板获取参数
     */
    fun getParamFromClipboard(context: Context?): JSONObject? {
        try {
            val clipboardManager =
                context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            var clipData = clipboardManager.primaryClip
            if (clipData != null) {
                val tracerPrefix = "datatracer:"
                val str = clipData.getItemAt(0).text.toString()
                if (str.startsWith(tracerPrefix)) {
                    clipData = ClipData.newPlainText("", "")
                    clipboardManager.setPrimaryClip(clipData)
                    val data = Base64.decode(
                        str.substring(tracerPrefix.length).toByteArray(),
                        Base64.NO_WRAP
                    )
                    return getParamFromLink(Uri.parse("?" + String(data)))
                }
            }
        } catch (t: Throwable) {
        }
        return null
    }

    /**
     * 获取本机ip
     */
//    @MainThread
//    fun getIpAddressString(): String? {
//        return try {
//            NetworkInterface.getNetworkInterfaces().asSequence().flatMap { networkInterfaces ->
//                networkInterfaces.inetAddresses.asSequence()
//            }.find { inetAddress ->
//                inetAddress is Inet4Address && !inetAddress.isLoopbackAddress()
//            }?.hostAddress
//        } catch (t: Throwable) {
//            null
//        }
//    }
}