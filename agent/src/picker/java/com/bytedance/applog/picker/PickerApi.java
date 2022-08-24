// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.picker;

import android.text.TextUtils;

import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.network.RangersHttpException;
import com.bytedance.applog.server.Api;
import com.bytedance.applog.util.TLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

/**
 * @author shiyanlong
 * @date 2019/2/27
 */
public class PickerApi extends Api {

    private static final String PATH_UPLOAD_DOM = "/simulator/mobile/layout";

    public PickerApi(AppLogInstance appLogInstance) {
        super(appLogInstance);
    }

    public JSONObject uploadDom(
            String aid,
            String appVersion,
            String cookie,
            List<DomSender.DomPageModel> domPageModelList) {
        JSONObject request = new JSONObject();
        try {
            JSONArray extra = new JSONArray();
            for (int i = 0; i < domPageModelList.size(); i++) {
                JSONObject jsonObject = new JSONObject();
                JSONObject header = new JSONObject();
                if (i == 0) {
                    header = getHeader(aid, appVersion);
                    request.put("header", header);
                    request.put("img", domPageModelList.get(i).shotBase64);
                    request.put("pages", domPageModelList.get(i).domPagerArray);
                } else {
                    jsonObject.put("header", header);
                    jsonObject.put("img", domPageModelList.get(i).shotBase64);
                    jsonObject.put("pages", domPageModelList.get(i).domPagerArray);
                    extra.put(jsonObject);
                }
                header.put("width", domPageModelList.get(i).width);
                header.put("height", domPageModelList.get(i).height);
            }
            request.put("extra", extra);
        } catch (JSONException e) {
            TLog.e(e);
        }
        HashMap<String, String> header = getHeaders();
        header.put(KEY_COOKIES, cookie);
        String resp = null;
        try {
            resp =
                    http(
                            METHOD_POST,
                            appLogInstance.getApi().getSchemeHost() + PATH_UPLOAD_DOM,
                            header,
                            appLogInstance
                                    .getApi()
                                    .getEncryptUtils()
                                    .transformStrToByte(request.toString()));
        } catch (RangersHttpException ignore) {
        }
        if (TextUtils.isEmpty(resp)) {
            return null;
        }
        try {
            return new JSONObject(resp);
        } catch (JSONException e) {
            TLog.ysnp(e);
        }
        return null;
    }

    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>(2);
        if (appLogInstance.getEncryptAndCompress()) {
            headers.put("Content-Type", "application/octet-stream;tt-data=a");
        } else {
            headers.put("Content-Type", "application/json; charset=utf-8");
        }
        return headers;
    }
}
