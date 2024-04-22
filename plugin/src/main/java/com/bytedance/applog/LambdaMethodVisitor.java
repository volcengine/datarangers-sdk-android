// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import com.bytedance.applog.util.AsmUtils;

import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** recognize lambda invoke dynamic target Created by lixiao on 2020/8/19. */
public class LambdaMethodVisitor extends MethodVisitor {
    private final String mClass;
    private final HashMap<String, MethodChanger> mNeedToHookForLambda;

    public LambdaMethodVisitor(
            String clasz,
            final MethodVisitor mv,
            final int access,
            final String name,
            final String desc,
            final HashMap<String, MethodChanger> needToHookForLambda) {
        //        super(ASM_API, mv, access, name, desc);
        super(AsmUtils.getMaxApi(), mv);
        mClass = clasz;
        mNeedToHookForLambda = needToHookForLambda;
    }

    @Override
    public void visitMethodInsn(
            int opcode, String owner, String name, String descriptor, boolean isInterface) {
        InvokeMethodReplacer.replace(mv, mClass, opcode, owner, name, descriptor, isInterface);
    }

    /** recognize invoke-dynamic and add target method to queue if interested */
    @Override
    public void visitInvokeDynamicInsn(
            final String name, final String desc, final Handle bsm, final Object... bsmArgs) {
        super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        String interfaceName = getClassName(desc);
        HashMap<String, MethodChanger> inter =
                MethodChanger.findChangersForInterface(new String[] {interfaceName});

        if (inter != null && bsmArgs != null && bsmArgs.length > 2) {
            MethodChanger changer = inter.get(name + bsmArgs[0]);
            if (changer != null && bsmArgs[1] != null) {
                // remove extra information after classname
                String target = bsmArgs[1].toString().split(" ")[0];
                String[] targetClassSplit = target.split("\\.");
                String targetClass = "";
                String targetMethod = "";
                if (target.length() >= 2) {
                    targetClass = targetClassSplit[0];
                    targetMethod = targetClassSplit[1];
                }

                if (targetClass.equals(mClass)) {
                    mNeedToHookForLambda.put(targetMethod, changer);
                } else {
                    // type two, need add one more method
                }
            } else {
                System.out.println("error, not found " + interfaceName);
            }
        }
    }

    private final String mRegex = "\\)L([^;]*);";
    private final Pattern mPattern = Pattern.compile(mRegex, Pattern.MULTILINE);

    /**
     * @param desc: ()Landroid/widget/CompoundButton$OnCheckedChangeListener;
     * @return className: android/widget/CompoundButton$OnCheckedChangeListener
     */
    private String getClassName(String desc) {
        final Matcher matcher = mPattern.matcher(desc);
        if (matcher.find() && matcher.groupCount() > 0) {
            return matcher.group(1);
        } else {
            return null;
        }
    }
}
