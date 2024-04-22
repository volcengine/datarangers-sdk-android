// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ARETURN;

import com.bytedance.applog.util.AsmUtils;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

/**
 * 过滤黑名单的函数变更器
 *
 * <p>将在黑名单的信息对象的函数修改为返回默认值
 *
 * <pre>
 * 黑名单：
 * 1> MAC_ADDRESS
 * 2> IMEI
 * 3> OAID
 * 4> ANDROIDID
 * 5> OPERATOR 运营商信息
 * </pre>
 *
 * @author luodong.seu
 */
public class BlackMethodChanger extends MethodVisitor {

    private enum ReturnType {
        NULL, // 返回null值
        EMPTY_STRING, // 空字符串
        DEFAULT_MAC_ADDRESS_STRING, //
    }

    private static final String DEFAULT_MAC_ADDRESS = "02:00:00:00:00:00";

    /** [0] 返回值类型 [1] 函数黑名单的配置名 [2] 类路径 [3] 函数名 [4] 签名 */
    private static final String[][] BLACK_METHODS =
            new String[][] {
                {
                    ReturnType.DEFAULT_MAC_ADDRESS_STRING.name(),
                    "MAC_ADDRESS",
                    "com/bytedance/applog/util/SensitiveUtils",
                    "getMacAddressFromSystem",
                    "(Landroid/content/Context;)Ljava/lang/String;",
                }, // String SensitiveUtils.getMacAddressFromSystem(context)
                {
                    ReturnType.NULL.name(),
                    "IMEI_MEID",
                    "com/bytedance/applog/util/SensitiveUtils",
                    "getMultiImeiFromSystem",
                    "(Landroid/content/Context;)Lorg/json/JSONArray;",
                }, // SensitiveUtils.getMultiImeiFromSystem(context)
                {
                    ReturnType.NULL.name(),
                    "OAID",
                    "com/bytedance/dr/OaidFactory",
                    "createOaidImpl",
                    "(Landroid/content/Context;)Lcom/bytedance/dr/OaidApi;",
                }, // OaidFactory.createOaidImpl(context)
                {
                    ReturnType.EMPTY_STRING.name(),
                    "ANDROIDID",
                    "com/bytedance/applog/util/HardwareUtils",
                    "getSecureAndroidId",
                    "(Landroid/content/Context;)Ljava/lang/String;",
                }, // HardwareUtils.createOaidImpl(context)
                {
                    ReturnType.EMPTY_STRING.name(),
                    "OPERATOR",
                    "com/bytedance/applog/util/HardwareUtils",
                    "getOperatorName",
                    "(Landroid/content/Context;)Ljava/lang/String;",
                }, // HardwareUtils.getOperatorName(context)
                {
                    ReturnType.EMPTY_STRING.name(),
                    "OPERATOR",
                    "com/bytedance/applog/util/HardwareUtils",
                    "getOperatorMccMnc",
                    "(Landroid/content/Context;)Ljava/lang/String;",
                }, // HardwareUtils.getOperatorMccMnc(context)
                {
                     ReturnType.NULL.name(),
                     "CLIPBOARD",
                     "com/bytedance/applog/alink/util/LinkUtils",
                     "getParamFromClipboard",
                     "(Landroid/content/Context;)Lorg/json/JSONObject;"
                }, // LinkUtils.getParamFromClipboard(context)
            };

    /** 返回值类型 */
    private final ReturnType returnType;

    public BlackMethodChanger(MethodVisitor mv, ReturnType returnType) {
        super(AsmUtils.getMaxApi(), mv);
        this.returnType = returnType;
    }

    @Override
    public void visitCode() {
        mv.visitCode();
        Label label0 = new Label();
        mv.visitLabel(label0);
        switch (returnType) {
            case NULL:
                mv.visitInsn(ACONST_NULL);
                break;
            case EMPTY_STRING:
                mv.visitLdcInsn("");
                break;
            case DEFAULT_MAC_ADDRESS_STRING:
                mv.visitLdcInsn(DEFAULT_MAC_ADDRESS);
                break;
        }
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 2);
        mv.visitEnd();
    }

    /**
     * 如果匹配上了黑名单，则返回一个BlackMethodChanger对象，否则返回null
     *
     * @param name 函数名
     * @param desc 描述
     * @return BlackMethodChanger | null
     */
    public static BlackMethodChanger change(
            MethodVisitor mv, String className, String name, String desc) {
        for (String[] method : BLACK_METHODS) {
            if (method[2].equals(className)
                    && method[3].equals(name)
                    && method[4].equals(desc)
                    && TeaTransform.isInTrackBlackList(method[1])) {
                return new BlackMethodChanger(mv, ReturnType.valueOf(method[0]));
            }
        }
        return null;
    }
}
