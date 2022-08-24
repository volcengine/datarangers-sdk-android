// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** classVisitor for lambda method Created by lixiao on 2020/8/19. */
class LambdaClassVisitor extends ClassVisitor {

    private static final int ASM_API = Opcodes.ASM7;

    private String mClassName;
    // key is method + desc
    private final HashMap<String, MethodChanger> mNeedToHookForLambda;

    LambdaClassVisitor(
            final ClassWriter writer, HashMap<String, MethodChanger> needToHookForLambda) {
        super(ASM_API, writer);
        mNeedToHookForLambda = needToHookForLambda;
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
    }

    @Override
    public MethodVisitor visitMethod(
            final int access,
            final String name,
            final String desc,
            final String signature,
            final String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        String fullName = name + desc;
        MethodChanger methodChanger = mNeedToHookForLambda.get(fullName);
        if (methodChanger != null) {
            mv = new LambdaMethodChanger(methodChanger, "", mv, access, name, desc);
        }
        return mv;
    }

    private static class LambdaMethodChanger extends AdviceAdapter {

        private final MethodChanger mChanger;

        private final String mClass;
        private final String mDesc;
        private final int mAccess;

        LambdaMethodChanger(
                final MethodChanger changer,
                String clazz,
                final MethodVisitor mv,
                final int access,
                final String name,
                final String desc) {
            super(ASM_API, mv, access, name, desc);
            mChanger = changer;
            mClass = clazz;
            mDesc = desc;
            mAccess = access;
        }

        @Override
        protected void onMethodEnter() {
            mChanger.changeForLambda(this, mClass, mAccess, mDesc, getMethodParamList(mDesc));
        }

        private static Pattern allParamsPattern = Pattern.compile("(\\(.*?\\))");
        private static Pattern paramsPattern = Pattern.compile("(\\[?)(C|Z|S|I|J|F|D|(:?L[^;]+;))");

        /**
         * calculate parameters count from method description needed because target method
         * parameters is not equals to interface method
         *
         * @param methodRefType
         * @return
         */
        private static List<String> getMethodParamList(String methodRefType) {
            Matcher m = allParamsPattern.matcher(methodRefType);
            if (!m.find()) {
                throw new IllegalArgumentException("Method signature does not contain parameters");
            }
            String paramsDescriptor = m.group(1);
            Matcher mParam = paramsPattern.matcher(paramsDescriptor);
            List<String> paramsList = new ArrayList<>();
            while (mParam.find()) {
                paramsList.add(mParam.group());
            }
            return paramsList;
        }
    }
}
