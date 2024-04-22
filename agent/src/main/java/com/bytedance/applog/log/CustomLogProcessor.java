// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.log;

import com.bytedance.applog.ILogger;

/**
 * 自定义的日志处理器
 *
 * @author luodong.seu
 */
public class CustomLogProcessor implements ILogProcessor {

    private final ILogger logger;

    public CustomLogProcessor(ILogger logger) {
        this.logger = logger;
    }

    @Override
    public void onLog(LogInfo log) {
        if (null != logger) {
            logger.log(log.getMessage(), log.getThrowable());
        }
    }
}
