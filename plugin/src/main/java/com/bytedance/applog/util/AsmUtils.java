// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import org.objectweb.asm.Opcodes;

import java.lang.reflect.Field;

/**
 * Created by luodong.seu on 2022/10/12
 *
 * @author luodong.seu@bytedance.com
 */
public class AsmUtils {
    private static int maxApi = -1;

    /**
     * 获取最大的API版本
     *
     * @return Opcodes.ASMX
     */
    public static synchronized int getMaxApi() {
        if (maxApi > 0) {
            return maxApi;
        }
        for (int i = 9; i >= 4; i--) {
            try {
                Field field = Opcodes.class.getDeclaredField("ASM" + i);
                field.setAccessible(true);
                maxApi = field.getInt(Opcodes.class);
                Log.i("AsmUtils: Max api is ASM" + i);
                return maxApi;
            } catch (Throwable ignored) {

            }
        }
        return 0;
    }
}
