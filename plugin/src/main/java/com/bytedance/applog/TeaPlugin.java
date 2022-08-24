// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import com.android.build.gradle.AppExtension;
import com.bytedance.applog.util.Log;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/** @author shiyanlong */
public class TeaPlugin implements Plugin<Project> {

    public static final String EXTENSION_NAME = "teaExtension";

    @Override
    public void apply(Project project) {
        Log.init(project, "apply: TeaPlugin");
        project.getExtensions().create(EXTENSION_NAME, TeaExtension.class);
        AppExtension extension = project.getExtensions().findByType(AppExtension.class);
        if (extension == null) {
            throw new GradleException(
                    " 「com.bytedance.std.tracker」插件需要在「App module」下引入，请检查配置或查阅接入文档https://datarangers.com.cn/help/doc?lid=1097&did=10942");
        }
        TeaTransform transform = new TeaTransform(project);
        extension.registerTransform(transform);
    }
}
