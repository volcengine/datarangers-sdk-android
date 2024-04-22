// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.picker;

import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.log.LoggerImpl;
import com.bytedance.applog.network.INetworkClient;
import com.bytedance.applog.network.RangersHttpException;
import com.bytedance.applog.server.Api;
import com.bytedance.applog.util.EncryptUtils;
import com.bytedance.applog.util.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
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
            String urlHost,
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
        } catch (Throwable e) {
            LoggerImpl.global()
                    .error(Collections.singletonList("PickerApi"), "Request handle failed", e);
        }
        HashMap<String, String> header = EncryptUtils.putContentTypeHeader(new HashMap<String, String>(2), appLogInstance);
        header.put(KEY_COOKIES, cookie);
        String resp = null;
        try {
            resp =
                    new String(
                            appLogInstance
                                    .getNetClient()
                                    .execute(
                                            INetworkClient.METHOD_POST,
                                            urlHost + PATH_UPLOAD_DOM,
                                            request,
                                            header,
                                            INetworkClient.RESPONSE_TYPE_STRING,
                                            true,
                                            Api.HTTP_SHORT_TIMEOUT));
        } catch (RangersHttpException ignore) {
        }
        if (Utils.isEmpty(resp)) {
            return null;
        }
        try {
            return new JSONObject(resp);
        } catch (JSONException e) {
            LoggerImpl.global()
                    .error(Collections.singletonList("PickerApi"), "JSON handle failed", e);
        }
        return null;
    }
}
