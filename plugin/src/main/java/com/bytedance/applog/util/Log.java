// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import org.gradle.api.Project;

import org.apache.tools.ant.util.StringUtils;

/**
 * @author shiyanlong
 * @date 2019/1/13
 **/
public class Log {

    private static Project sProject;

    private static final String LOG_TAG = "TEA ";

    public static void init(Project project, String log) {
        sProject = project;
        i("init with '" + log + "'");
    }

    public static void i(final String s) {
        sProject.getLogger().lifecycle(LOG_TAG + s);
    }

    public static void e(final Throwable t) {
        sProject.getLogger().lifecycle(LOG_TAG + StringUtils.getStackTrace(t));
        throw new RuntimeException(t);
    }
}