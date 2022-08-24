// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.picker;

import com.bytedance.applog.util.TLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class WebInfoModel {

    private String page;
    private List<InfoModel> info;
    private FrameModel frame;
    private String webViewElementPath;

    public String getWebViewElementPath() {
        return webViewElementPath;
    }

    public void setWebViewElementPath(String webViewElementPath) {
        this.webViewElementPath = webViewElementPath;
    }

    public FrameModel getFrame() {
        return frame;
    }

    public void setFrame(FrameModel frame) {
        this.frame = frame;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public List<InfoModel> getInfo() {
        return info;
    }

    public void setInfo(List<InfoModel> info) {
        this.info = info;
    }

    @Override
    public String toString() {
        return "WebInfoModel{" + "page='" + page + '\'' + ", info=" + info + '}';
    }

    public static class InfoModel {
        /**
         * "nodeName": "a", "frame": { "x": 5, "y": 49, "width": 54, "height": 26 },
         * "_element_path": "/body/div/div/div/div/div/a", "element_path":
         * "/body/div/div/div/div/div/a/*", "positions": [ "0", "2", "0", "0", "0", "1", "0", "*" ],
         * "zIndex": 10000, "texts": [ "推荐" ], "href": "javascript:void(0)", "_checkList": true,
         * "fuzzy_positions": [ "0", "2", "0", "0", "0", "1", "*", "*" ]
         */
        String nodeName;

        FrameModel frameModel;
        String elementPath;
        String elementPathV2;
        List<String> positions;
        int zIndex;
        List<String> texts;
        List<InfoModel> children;
        String href;
        boolean checkList;
        List<String> fuzzyPositions;

        public InfoModel(
                String nodeName,
                FrameModel frameModel,
                String elementPath,
                String elementPathV2,
                List<String> positions,
                int zIndex,
                List<String> texts,
                List<InfoModel> children,
                String href,
                boolean checkList,
                List<String> fuzzyPositions) {
            this.nodeName = nodeName;
            this.frameModel = frameModel;
            this.elementPath = elementPath;
            this.elementPathV2 = elementPathV2;
            this.positions = positions;
            this.zIndex = zIndex;
            this.texts = texts;
            this.children = children;
            this.href = href;
            this.checkList = checkList;
            this.fuzzyPositions = fuzzyPositions;
        }

        public String getNodeName() {
            return nodeName;
        }

        public void setNodeName(String nodeName) {
            this.nodeName = nodeName;
        }

        public FrameModel getFrameModel() {
            return frameModel;
        }

        public void setFrameModel(FrameModel frameModel) {
            this.frameModel = frameModel;
        }

        public String getElementPath() {
            return elementPath;
        }

        public void setElementPath(String elementPath) {
            this.elementPath = elementPath;
        }

        public String getElementPathV2() {
            return elementPathV2;
        }

        public void setElementPathV2(String elementPathV2) {
            this.elementPathV2 = elementPathV2;
        }

        public int getzIndex() {
            return zIndex;
        }

        public void setzIndex(int zIndex) {
            this.zIndex = zIndex;
        }

        public List<String> getPositions() {
            return positions;
        }

        public void setPositions(List<String> positions) {
            this.positions = positions;
        }

        public List<String> getTexts() {
            return texts;
        }

        public void setTexts(List<String> texts) {
            this.texts = texts;
        }

        public List<InfoModel> getChildren() {
            return children;
        }

        public void setChildren(List<InfoModel> children) {
            this.children = children;
        }

        public String getHref() {
            return href;
        }

        public void setHref(String href) {
            this.href = href;
        }

        public boolean isCheckList() {
            return checkList;
        }

        public void setCheckList(boolean checkList) {
            this.checkList = checkList;
        }

        public List<String> getFuzzyPositions() {
            return fuzzyPositions;
        }

        public void setFuzzyPositions(List<String> fuzzyPositions) {
            this.fuzzyPositions = fuzzyPositions;
        }

        @Override
        public String toString() {
            return "InfoModel{"
                    + "nodeName='"
                    + nodeName
                    + '\''
                    + ", frameModel="
                    + frameModel
                    + ", elementPath='"
                    + elementPath
                    + '\''
                    + ", elementPathV2='"
                    + elementPathV2
                    + '\''
                    + ", positions="
                    + positions
                    + ", zIndex="
                    + zIndex
                    + ", texts="
                    + texts
                    + ", children="
                    + children
                    + ", href='"
                    + href
                    + '\''
                    + ", checkList="
                    + checkList
                    + ", fuzzyPositions="
                    + fuzzyPositions
                    + '}';
        }
    }

    public static class FrameModel {
        int x;
        int y;
        int width;
        int height;

        public FrameModel(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        @Override
        public String toString() {
            return "FrameModel{"
                    + "x="
                    + x
                    + ", y="
                    + y
                    + ", width="
                    + width
                    + ", height="
                    + height
                    + '}';
        }

        public JSONObject toJson() {
            try {
                JSONObject object = new JSONObject();
                object.put("x", x);
                object.put("y", y);
                object.put("width", width);
                object.put("height", height);
                return object;
            } catch (JSONException e) {
                TLog.e(e);
            }
            return null;
        }
    }
}
