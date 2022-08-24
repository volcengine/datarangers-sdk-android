// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.store;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * @author shiyanlong
 * @date 2019/2/19
 */
public class Click extends EventV3 {
    public static final String EVENT_KEY = "bav2b_click";
    public String page;
    public String pageTitle;
    public String path;
    public String elementId;
    public String elementType;
    public ArrayList<String> contents;
    public ArrayList<String> positions;
    public int width;
    public int height;
    public int touchX;
    public int touchY;
    public boolean isHtml;
    public ArrayList<String> fuzzyPositions;

    Click() {
        super(null, EVENT_KEY, true, null);
    }

    public Click(
            final String page,
            final String pageTitle,
            final String path,
            final String elementId,
            final String elementType,
            final int width,
            final int height,
            final int touchX,
            final int touchY,
            ArrayList<String> contents,
            final ArrayList<String> positions) {
        this();
        this.page = page;
        this.pageTitle = pageTitle;
        this.path = path;
        this.elementId = elementId;
        this.elementType = elementType;
        this.contents = contents;
        this.positions = positions;
        this.width = width;
        this.height = height;
        this.touchX = touchX;
        this.touchY = touchY;
    }

    public Click(
            final String page,
            final String pageTitle,
            final String path,
            final String elementId,
            final String elementType,
            final int width,
            final int height,
            final int touchX,
            final int touchY,
            ArrayList<String> contents,
            final ArrayList<String> positions,
            final ArrayList<String> fuzzyPositions) {
        this();
        this.page = page;
        this.pageTitle = pageTitle;
        this.path = path;
        this.elementId = elementId;
        this.elementType = elementType;
        this.contents = contents;
        this.positions = positions;
        this.width = width;
        this.height = height;
        this.touchX = touchX;
        this.touchY = touchY;
        this.fuzzyPositions = fuzzyPositions;
    }

    @Override
    protected void fillParam() throws JSONException {
        if (param == null) {
            JSONObject obj = new JSONObject();
            obj.put("element_path", path);
            obj.put("page_key", page);
            if (positions != null && positions.size() > 0) {
                obj.put("positions", new JSONArray(positions));
            }
            if (contents != null && contents.size() > 0) {
                obj.put("texts", new JSONArray(contents));
            }
            obj.put("element_width", width);
            obj.put("element_height", height);
            obj.put("touch_x", touchX);
            obj.put("touch_y", touchY);
            obj.put("page_title", pageTitle);
            obj.put("element_id", elementId);
            obj.put("element_type", elementType);
            param = obj.toString();
        }
    }
}
