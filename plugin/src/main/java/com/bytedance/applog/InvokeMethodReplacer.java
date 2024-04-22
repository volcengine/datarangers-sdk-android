package com.bytedance.applog;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Objects;

/**
 * Replace method invoke to another
 *
 * <p>To change method invoke to Tracker method
 *
 * @author luodong.seu
 */
public class InvokeMethodReplacer {

    /** opcode、owner、name、descriptor、tracker_descriptor */
    private static final String[][] METHODS =
            new String[][] {
                {
                    String.valueOf(Opcodes.INVOKEVIRTUAL),
                    "android/webkit/WebView",
                    "loadUrl",
                    "(Ljava/lang/String;)V",
                    "(Ljava/lang/Object;Ljava/lang/String;)V"
                }, // webview.loadUrl("")
                {
                    String.valueOf(Opcodes.INVOKEVIRTUAL),
                    "android/webkit/WebView",
                    "loadUrl",
                    "(Ljava/lang/String;Ljava/util/Map;)V",
                    "(Ljava/lang/Object;Ljava/lang/String;Ljava/util/Map;)V"
                }, // webview.loadUrl("", headers)
                {
                    String.valueOf(Opcodes.INVOKEVIRTUAL),
                    "android/webkit/WebView",
                    "loadData",
                    "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                    "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"
                }, // webview.loadData(String data, String mimeType, String
                //                 encoding)
                {
                    String.valueOf(Opcodes.INVOKEVIRTUAL),
                    "android/webkit/WebView",
                    "loadDataWithBaseURL",
                    "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                    "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"
                }, // webview.loadDataWithBaseURL(String baseUrl, String data,
            };

    public static void replace(
            MethodVisitor mv,
            String clz,
            int opcode,
            String owner,
            String name,
            String descriptor,
            boolean isInterface) {
        replace(mv::visitMethodInsn, clz, opcode, owner, name, descriptor, isInterface);
    }

    /**
     * Replace function call
     *
     * @param callback VisitMethodInsnCallback
     */
    public static void replace(
            VisitMethodInsnCallback callback,
            String clz,
            int opcode,
            String owner,
            String name,
            String descriptor,
            boolean isInterface) {
        if (TeaTransform.AUTO_INJECT_WEBVIEW_BRIDGE) {
            final String targetClass = MethodChanger.TEA_AGENT_CLASS;
            for (String[] method : METHODS) {
                // 签名一致
                boolean needAddCallback =
                        TeaClassVisitor.isClzIncluded(clz)
                                && !Objects.equals(clz, targetClass)
                                && String.valueOf(opcode).equals(method[0])
                                && name.equals(method[2])
                                && descriptor.equals(method[3]);
                if (needAddCallback) {
                    // 插入调用前
                    callback.visitMethodInsn(
                            Opcodes.INVOKESTATIC, targetClass, name, method[4], false);
                    return;
                }
            }
        }

        // 未匹配
        callback.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    /** A callback interface to do visitMethodInsn by caller */
    interface VisitMethodInsnCallback {
        void visitMethodInsn(
                final int opcode,
                final String owner,
                final String name,
                final String descriptor,
                final boolean isInterface);
    }
}
