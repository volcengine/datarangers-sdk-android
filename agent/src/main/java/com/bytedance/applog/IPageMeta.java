// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import org.json.JSONObject;

/**
 * 页面元数据的属性接口，支持Activity和Fragment
 *
 * <p>Usage: <code>
 *     class TestActivity implements IPageMeta {
 *
 *      @Override
 *      public String path() {
 *          return "/activity/test"
 *      }
 *
 *      @Override
 *      public String title() {
 *          return "test-page";
 *      }
 *
 *      @Override
 *      public JSONObject properties() {
 *             JSONObject json = new JSONObject();
 *             json.putString("key", "value");
 *             return json;
 *         }
 *     }
 *
 * </code>
 *
 * @author luodong.seu
 */
public interface IPageMeta {

    /**
     * 页面路径
     *
     * <p>same as @PageMeta(path="") but priority higher than @PageMeta
     */
    String path();

    /**
     * 页面标题
     *
     * <p>same as @PageMeta(title="") but priority higher than @PageMeta
     */
    String title();

    /**
     * 自定义的页面属性
     *
     * @return JSONObject
     */
    JSONObject pageProperties();
}
