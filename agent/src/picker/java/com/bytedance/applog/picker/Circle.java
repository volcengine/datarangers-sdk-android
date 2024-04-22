// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.picker;

import com.bytedance.applog.store.Click;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author shiyanlong
 * @date 2019/2/27
 */
public class Circle extends Click {

    int[] location;
    int width;
    int height;
    int level;
    boolean ignore;
    List<Circle> childrenCircle = new ArrayList<>();

    public Circle(final Click info) {
        super(
                info.page,
                info.pageTitle,
                info.path,
                info.elementId,
                info.elementType,
                info.width,
                info.height,
                info.touchX,
                info.touchY,
                info.contents,
                info.positions,
                info.fuzzyPositions);
        this.page = info.page;
        this.path = info.path;
        this.positions = info.positions;
        this.contents = info.contents;
    }

    public JSONObject getFrameJson() {
        try {
            JSONObject object = new JSONObject();
            if (location != null && location.length > 1) {
                object.put("x", location[0]);
                object.put("y", location[1]);
            }
            object.put("width", width);
            object.put("height", height);
            return object;
        } catch (JSONException e) {
            getLogger().error(loggerTags, "JSON handle failed", e);
        }
        return null;
    }
}
