// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import com.bytedance.applog.util.AsmUtils;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author shiyanlong
 * @date 2019/1/13
 */
class TeaClassVisitor extends ClassVisitor {

    /**
     * 默认过滤前缀数组
     */
    private static final List<String> DEFAULT_EXCLUDE_CLASS_PREFIX_ARRAY =
            Arrays.asList(
                    "com/bytedance/applog/",
                    "com/bytedance/bdtracker/",
                    "com/bytedance/dr/",
                    "com/tencent/smtt/sdk/",
                    "com/tencent/tinker/");

    private String mClassName;
    private HashMap<String, MethodChanger> mClassMethodChangers;
    private HashMap<String, MethodChanger> mInterfaceMethodChangers;

    // key is method + desc
    private final HashMap<String, MethodChanger> mNeedToHookForLambda = new HashMap<>();

    TeaClassVisitor(final ClassWriter writer) {
        super(AsmUtils.getMaxApi(), writer);
    }

    /**
     * 是否包含该类
     *
     * @param clz 类名descriptor
     * @return true 包含
     */
    public static boolean isClzIncluded(String clz) {
        if (null == clz) {
            return false;
        }
        if (isInBlackList(clz)) {
            return false;
        }
        for (String prefix : DEFAULT_EXCLUDE_CLASS_PREFIX_ARRAY) {
            if (clz.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void visit(
            final int version,
            final int access,
            final String name,
            final String signature,
            final String superName,
            final String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        mClassName = name;

        if (!isClzIncluded(name)) {
            return;
        }

        if (TeaTransform.DISABLE_AUTO_TRACK) {
            return;
        }

        mClassMethodChangers = MethodChanger.findChangersForClass(superName);
        mInterfaceMethodChangers = MethodChanger.findChangersForInterface(interfaces);
    }

    @Override
    public MethodVisitor visitMethod(
            final int access,
            final String name,
            final String desc,
            final String signature,
            final String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

        // 黑名单函数
        MethodVisitor blackMv = BlackMethodChanger.change(mv, mClassName, name, desc);
        if (null != blackMv) {
            return blackMv;
        }

        if (!isClzIncluded(mClassName)) {
            return mv;
        }

        if (TeaTransform.DISABLE_AUTO_TRACK) {
            return mv;
        }

        MethodChanger changer = null;
        if (mClassMethodChangers != null) {
            changer = mClassMethodChangers.remove(name + desc);
        }
        if (changer == null && mInterfaceMethodChangers != null) {
            changer = mInterfaceMethodChangers.remove(name + desc);
        }
        if (changer != null) {
            mv = new TeaMethodVisitor(changer, mClassName, mv, access, name, desc);
        } else {
            mv = new LambdaMethodVisitor(mClassName, mv, access, name, desc, mNeedToHookForLambda);
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        if (mClassMethodChangers != null && !mClassMethodChangers.isEmpty()) {
            MethodChanger[] changers = new MethodChanger[mClassMethodChangers.size()];
            mClassMethodChangers.values().toArray(changers);
            for (MethodChanger changer : changers) {
                changer.addMethod(this, mClassName);
            }
        }
        super.visitEnd();
    }

    public HashMap<String, MethodChanger> getNeedToHookForLambda() {
        return mNeedToHookForLambda;
    }

    private static class TeaMethodVisitor extends AdviceAdapter {

        private final MethodChanger mChanger;

        private final String mClass;

        TeaMethodVisitor(
                final MethodChanger changer,
                String clasz,
                final MethodVisitor mv,
                final int access,
                final String name,
                final String desc) {
            super(AsmUtils.getMaxApi(), mv, access, name, desc);
            mChanger = changer;
            mClass = clasz;
        }

        @Override
        protected void onMethodEnter() {
            mChanger.change(this, mClass);
        }

        @Override
        public void visitMethodInsn(
                int opcode, String owner, String name, String desc, boolean itf) {
            InvokeMethodReplacer.replace(
                    super::visitMethodInsn, mClass, opcode, owner, name, desc, itf);
        }
    }

    private static boolean isInBlackList(String packageName) {
        if (TeaTransform.BLACK_LIST == null) {
            return false;
        }
        for (String prefix : TeaTransform.BLACK_LIST) {
            if (packageName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
