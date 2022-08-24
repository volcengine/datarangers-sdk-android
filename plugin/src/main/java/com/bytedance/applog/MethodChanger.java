// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import com.bytedance.applog.util.Log;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author shiyanlong
 * @date 2019/1/13
 *     <p>
 *     <p>Changed by leeyoung
 * @date 2019/1/18
 */
class MethodChanger {

    public static final String TEA_AGENT_CLASS = "com/bytedance/applog/tracker/Tracker";

    private final String mBaseClass;

    private final String mInterface;

    /** 方法名+方法desc的拼接字符串，唯一确定一个方法 */
    private final String mName;

    private final String mOriginDesc;

    private final String mTargetDesc;

    /** 目标方法和原方法的args，碰巧相同 */
    private final int mArgs;

    private static final int ARGS_ALOAD = 0;
    private static final int ARGS_ALOAD_ILOAD = 1;
    private static final int ARGS_ALOAD_FLOAD_ILOAD = 2;
    private static final int ARGS_ALOAD_ALOAD_ILOAD_LLOAD = 3;
    private static final int ARGS_ALOAD_ALOAD_ILOAD_ILOAD_LLOAD = 4;
    private static final int ARGS_ALOAD_ALOAD_ALOAD = 5;
    private static final int ARGS_ALOAD_ALOAD_ALOAD_ALOAD = 6;
    private static final int ARGS_ALOAD_ALOAD = 7;
    private static final int ARGS_ALOAD_ALOAD_ILOAD = 8;

    private static final int[][] ARGS_GROUP_MAP = {
        {Opcodes.ALOAD},
        {Opcodes.ALOAD, Opcodes.ILOAD},
        {Opcodes.ALOAD, Opcodes.FLOAD, Opcodes.ILOAD},
        {Opcodes.ALOAD, Opcodes.ALOAD, Opcodes.ILOAD, Opcodes.LLOAD},
        {Opcodes.ALOAD, Opcodes.ALOAD, Opcodes.ILOAD, Opcodes.ILOAD, Opcodes.LLOAD},
        {Opcodes.ALOAD, Opcodes.ALOAD, Opcodes.ALOAD},
        {Opcodes.ALOAD, Opcodes.ALOAD, Opcodes.ALOAD, Opcodes.ALOAD},
        {Opcodes.ALOAD, Opcodes.ALOAD},
        {Opcodes.ALOAD, Opcodes.ALOAD, Opcodes.ILOAD},
    };

    /** 接口的方法 */
    private static final String[][] I_METHODS = {
        {"android/view/View$OnClickListener", "(Landroid/view/View;)V", "onClick"},
        {
            "android/content/DialogInterface$OnClickListener",
            "(Landroid/content/DialogInterface;I)V",
            "onClick"
        },
        {
            "android/widget/AdapterView$OnItemClickListener",
            "(Landroid/widget/AdapterView;Landroid/view/View;IJ)V",
            "onItemClick"
        },
        {
            "android/widget/ExpandableListView$OnGroupClickListener",
            "(Landroid/widget/ExpandableListView;Landroid/view/View;IJ)Z",
            "onGroupClick"
        },
        {
            "android/widget/ExpandableListView$OnChildClickListener",
            "(Landroid/widget/ExpandableListView;Landroid/view/View;IIJ)Z",
            "onChildClick"
        },
        {
            "android/widget/RatingBar$OnRatingBarChangeListener",
            "(Landroid/widget/RatingBar;FZ)V",
            "onRatingChanged"
        },
        {
            "android/widget/CompoundButton$OnCheckedChangeListener",
            "(Landroid/widget/CompoundButton;Z)V",
            "onCheckedChanged"
        },
        {
            "android/widget/RadioGroup$OnCheckedChangeListener",
            "(Landroid/widget/RadioGroup;I)V",
            "onCheckedChanged"
        },
        {
            "android/widget/AdapterView$OnItemSelectedListener",
            "(Landroid/widget/AdapterView;Landroid/view/View;IJ)V",
            "onItemSelected"
        },
        {
            "android/widget/SeekBar$OnSeekBarChangeListener",
            "(Landroid/widget/SeekBar;)V",
            "onStopTrackingTouch"
        },
        {"android/view/View$OnFocusChangeListener", "(Landroid/view/View;Z)V", "onFocusChange"},
        {
            "android/view/MenuItem$OnMenuItemClickListener",
            "(Landroid/view/MenuItem;)Z",
            "onMenuItemClick"
        },
        //            {"android/view/View$OnLongClickListener", "(Landroid/view/View;)Z",
        // "onLongClick"},
        //            {"android/widget/AdapterView$OnItemLongClickListener",
        //                    "(Landroid/widget/AdapterView;Landroid/view/View;IJ)Z",
        // "onItemLongClick"},

        //            {"android/location/LocationListener", "(Landroid/location/Location;)V",
        // "onLocationChanged"},

    };

    private static final int[] I_ARGS = {
        ARGS_ALOAD,
        ARGS_ALOAD_ILOAD,
        ARGS_ALOAD_ALOAD_ILOAD_LLOAD,
        ARGS_ALOAD_ALOAD_ILOAD_LLOAD,
        ARGS_ALOAD_ALOAD_ILOAD_ILOAD_LLOAD,
        ARGS_ALOAD_FLOAD_ILOAD,
        ARGS_ALOAD_ILOAD,
        ARGS_ALOAD_ILOAD,
        ARGS_ALOAD_ALOAD_ILOAD_LLOAD,
        ARGS_ALOAD,
        ARGS_ALOAD_ILOAD,
        ARGS_ALOAD,
        //            ARGS_ALOAD,
        //            ARGS_ALOAD_ALOAD_ILOAD_LLOAD,
        //            ARGS_ALOAD,
    };

    private static final String[][] C_METHODS =
            new String[][] {
                {
                    "android/support/v4/app/Fragment",
                    "()V",
                    "onResume",
                    "(Landroid/support/v4/app/Fragment;)V"
                },
                {
                    "android/support/v4/app/Fragment",
                    "()V",
                    "onPause",
                    "(Landroid/support/v4/app/Fragment;)V"
                },
                {
                    "android/support/v4/app/Fragment",
                    "(Z)V",
                    "onHiddenChanged",
                    "(Landroid/support/v4/app/Fragment;Z)V"
                },
                {
                    "android/support/v4/app/Fragment",
                    "(Z)V",
                    "setUserVisibleHint",
                    "(Landroid/support/v4/app/Fragment;Z)V"
                },
                {"android/app/Fragment", "()V", "onResume", "(Landroid/app/Fragment;)V"},
                {"android/app/Fragment", "()V", "onPause", "(Landroid/app/Fragment;)V"},
                {"android/app/Fragment", "(Z)V", "onHiddenChanged", "(Landroid/app/Fragment;Z)V"},
                {
                    "android/app/Fragment",
                    "(Z)V",
                    "setUserVisibleHint",
                    "(Landroid/app/Fragment;Z)V"
                },
                {
                    "android/webkit/WebViewFragment",
                    "()V",
                    "onResume",
                    "(Landroid/webkit/WebViewFragment;)V"
                },
                {
                    "android/webkit/WebViewFragment",
                    "()V",
                    "onPause",
                    "(Landroid/webkit/WebViewFragment;)V"
                },
                {
                    "android/webkit/WebViewFragment",
                    "(Z)V",
                    "onHiddenChanged",
                    "(Landroid/webkit/WebViewFragment;Z)V"
                },
                {
                    "android/webkit/WebViewFragment",
                    "(Z)V",
                    "setUserVisibleHint",
                    "(Landroid/webkit/WebViewFragment;Z)V"
                },
                {
                    "android/preference/PreferenceFragment",
                    "()V",
                    "onResume",
                    "(Landroid/preference/PreferenceFragment;)V"
                },
                {
                    "android/preference/PreferenceFragment",
                    "()V",
                    "onPause",
                    "(Landroid/preference/PreferenceFragment;)V"
                },
                {
                    "android/preference/PreferenceFragment",
                    "(Z)V",
                    "onHiddenChanged",
                    "(Landroid/preference/PreferenceFragment;Z)V"
                },
                {
                    "android/preference/PreferenceFragment",
                    "(Z)V",
                    "setUserVisibleHint",
                    "(Landroid/preference/PreferenceFragment;Z)V"
                },
                {"android/app/ListFragment", "()V", "onResume", "(Landroid/app/ListFragment;)V"},
                {"android/app/ListFragment", "()V", "onPause", "(Landroid/app/ListFragment;)V"},
                {
                    "android/app/ListFragment",
                    "(Z)V",
                    "onHiddenChanged",
                    "(Landroid/app/ListFragment;Z)V"
                },
                {
                    "android/app/ListFragment",
                    "(Z)V",
                    "setUserVisibleHint",
                    "(Landroid/app/ListFragment;Z)V"
                },

                // AndroidX
                {
                    "androidx/fragment/app/Fragment",
                    "()V",
                    "onResume",
                    "(Landroidx/fragment/app/Fragment;)V"
                },
                {
                    "androidx/fragment/app/Fragment",
                    "()V",
                    "onPause",
                    "(Landroidx/fragment/app/Fragment;)V"
                },
                {
                    "androidx/fragment/app/Fragment",
                    "(Z)V",
                    "onHiddenChanged",
                    "(Landroidx/fragment/app/Fragment;Z)V"
                },
                {
                    "androidx/fragment/app/Fragment",
                    "(Z)V",
                    "setUserVisibleHint",
                    "(Landroidx/fragment/app/Fragment;Z)V"
                },

                // TODO: 2019/1/22 像AlertDialog这样的系统Dialog，一般没有派生类，要hook调用函数，而不是hook实现函数
                //            {"android/app/Dialog", "()V", "show", "(Landroid/app/Dialog;)V"},
                //
                //            {"android/app/Dialog", "()V", "dismiss", "(Landroid/app/Dialog;)V"},
                //
                //            {"android/app/Dialog", "()V", "hide", "(Landroid/app/Dialog;)V"},
                {
                    "android/webkit/WebChromeClient",
                    "(Landroid/webkit/WebView;I)V",
                    "onProgressChanged",
                    "(Ljava/lang/Object;Landroid/view/View;I)V"
                },
                {
                    "com/tencent/smtt/sdk/WebChromeClient",
                    "(Lcom/tencent/smtt/sdk/WebView;I)V",
                    "onProgressChanged",
                    "(Ljava/lang/Object;Landroid/view/View;I)V"
                },
                {
                    "android/app/Activity",
                    "(Landroid/view/MotionEvent;)Z",
                    "dispatchTouchEvent",
                    "(Landroid/view/MotionEvent;)V"
                },
                {"android/app/Presentation", "()V", "onStart", "(Landroid/app/Presentation;)V"},
                {"android/app/Presentation", "()V", "onStop", "(Landroid/app/Presentation;)V"},
            };

    private static final int[] C_ORIGIN_ARGS =
            new int[] {
                ARGS_ALOAD,
                ARGS_ALOAD,
                ARGS_ALOAD_ILOAD,
                ARGS_ALOAD_ILOAD,
                ARGS_ALOAD,
                ARGS_ALOAD,
                ARGS_ALOAD_ILOAD,
                ARGS_ALOAD_ILOAD,
                ARGS_ALOAD,
                ARGS_ALOAD,
                ARGS_ALOAD_ILOAD,
                ARGS_ALOAD_ILOAD,
                ARGS_ALOAD,
                ARGS_ALOAD,
                ARGS_ALOAD_ILOAD,
                ARGS_ALOAD_ILOAD,
                ARGS_ALOAD,
                ARGS_ALOAD,
                ARGS_ALOAD_ILOAD,
                ARGS_ALOAD_ILOAD,
                ARGS_ALOAD,
                ARGS_ALOAD,
                ARGS_ALOAD_ILOAD,
                ARGS_ALOAD_ILOAD,
                //            ARGS_ALOAD,
                //            ARGS_ALOAD,
                //            ARGS_ALOAD,
                ARGS_ALOAD_ALOAD_ILOAD,
                ARGS_ALOAD_ALOAD_ILOAD,
                ARGS_ALOAD_ALOAD,
                ARGS_ALOAD,
                ARGS_ALOAD,
            };

    void addMethod(final ClassVisitor classVisitor, final String className) {
        //        Log.i("addMethod: " + className + "." + mName + mOriginDesc);
        MethodVisitor mv =
                classVisitor.visitMethod(Opcodes.ACC_PUBLIC, mName, mOriginDesc, null, null);
        mv.visitCode();
        switch (mArgs) {
            case ARGS_ALOAD:
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                break;
            case ARGS_ALOAD_ILOAD:
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitVarInsn(Opcodes.ILOAD, 1);
                break;
            case ARGS_ALOAD_ALOAD_ALOAD:
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitVarInsn(Opcodes.ALOAD, 2);
                break;
            case ARGS_ALOAD_ALOAD_ALOAD_ALOAD:
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitVarInsn(Opcodes.ALOAD, 2);
                mv.visitVarInsn(Opcodes.ALOAD, 3);
                break;
            case ARGS_ALOAD_ALOAD:
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                break;
            case ARGS_ALOAD_ALOAD_ILOAD:
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitVarInsn(Opcodes.ILOAD, 2);
                break;
            default:
                throw new RuntimeException("oops");
        }
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, mBaseClass, mName, mOriginDesc, false);
        if (mOriginDesc.endsWith(")V")) {
            mv.visitInsn(Opcodes.RETURN);
        } else {
            mv.visitInsn(Opcodes.IRETURN);
        }

        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }

    void change(MethodVisitor mv, String className) {
        int offset = mBaseClass == null ? 1 : 0;
        int[] parameters = ARGS_GROUP_MAP[mArgs];
        for (int parameter : parameters) {
            mv.visitVarInsn(parameter, offset++);
        }

        mv.visitMethodInsn(Opcodes.INVOKESTATIC, TEA_AGENT_CLASS, mName, mTargetDesc, false);

        if (!mTargetDesc.endsWith(")V")) {
            mv.visitInsn(Opcodes.POP);
        }
    }

    /**
     * changer for lambda method, will do hook work lambda method should end with target method
     * desc, if not, drop it maybe this method is static/non-static, its parameters is not always
     * identical to interface
     */
    void changeForLambda(
            MethodVisitor mv,
            String className,
            int access,
            final String desc,
            final List<String> methodParamList) {
        //        Log.i("change: " + className + "." + mName + mOriginDesc + "-->" + mTargetDesc +
        // methodParamList);
        int offset = (access & Opcodes.ACC_STATIC) > 0 ? 0 : 1;
        int[] parameters = ARGS_GROUP_MAP[mArgs];
        if (desc.contains(mTargetDesc.substring(1))) {
            int offsetLength = methodParamList.size() - parameters.length;
            for (int i = 0; i < offsetLength; i++) {
                String paramType = methodParamList.get(i);
                if (paramType.equals("J") || paramType.equals("D")) {
                    offset += 2;
                } else {
                    offset++;
                }
            }
        } else {
            // lambda method doesn't end with target method desc, drop
            Log.i("error in change Lambda");
            return;
        }

        for (int parameter : parameters) {
            mv.visitVarInsn(parameter, offset++);
        }

        mv.visitMethodInsn(Opcodes.INVOKESTATIC, TEA_AGENT_CLASS, mName, mTargetDesc, false);

        if (!mTargetDesc.endsWith(")V")) {
            mv.visitInsn(Opcodes.POP);
        }
    }

    private static HashMap<String, ArrayList<MethodChanger>> sClassChangers;

    private static HashMap<String, ArrayList<MethodChanger>> sInterfaceChangers;

    static {
        final ArrayList<MethodChanger> allChangers = new ArrayList<>(32);
        for (int i = 0; i < I_METHODS.length; ++i) {
            allChangers.add(
                    new MethodChanger(
                            I_METHODS[i][0], I_METHODS[i][1], I_METHODS[i][2], I_ARGS[i]));
        }

        for (int i = 0; i < C_METHODS.length; ++i) {
            allChangers.add(
                    new MethodChanger(
                            C_METHODS[i][0],
                            C_METHODS[i][1],
                            C_METHODS[i][2],
                            C_ORIGIN_ARGS[i],
                            C_METHODS[i][3]));
        }

        sClassChangers = new HashMap<>(16);
        sInterfaceChangers = new HashMap<>(16);
        ArrayList<MethodChanger> changers;
        for (MethodChanger changer : allChangers) {
            if (changer.mBaseClass != null) {
                changers =
                        sClassChangers.computeIfAbsent(changer.mBaseClass, k -> new ArrayList<>(8));
                changers.add(changer);
            }
            if (changer.mInterface != null) {
                changers =
                        sInterfaceChangers.computeIfAbsent(
                                changer.mInterface, k -> new ArrayList<>(8));
                changers.add(changer);
            }
        }
    }

    private MethodChanger(String interName, String desc, String name, int args) {
        mBaseClass = null;
        mInterface = interName;
        mOriginDesc = desc;
        mName = name;
        mArgs = args;
        mTargetDesc = desc;
    }

    private MethodChanger(String clasz, String oDesc, String name, int oArgs, String tDesc) {
        mBaseClass = clasz;
        mInterface = null;
        mOriginDesc = oDesc;
        mName = name;
        mArgs = oArgs;
        mTargetDesc = tDesc;
    }

    static HashMap<String, MethodChanger> findChangersForClass(String superClass) {
        HashMap<String, MethodChanger> result = new HashMap<>(16);
        ArrayList<MethodChanger> changers = sClassChangers.get(superClass);
        if (changers != null) {
            for (MethodChanger changer : changers) {
                result.put(changer.mName + changer.mOriginDesc, changer);
            }
        }
        return result;
    }

    static HashMap<String, MethodChanger> findChangersForInterface(String[] interfaces) {
        HashMap<String, MethodChanger> result = null;
        for (String inter : interfaces) {
            ArrayList<MethodChanger> changers = sInterfaceChangers.get(inter);
            if (changers != null) {
                if (result == null) {
                    result = new HashMap<>(16);
                }
                for (MethodChanger changer : changers) {
                    result.put(changer.mName + changer.mOriginDesc, changer);
                }
            }
        }
        return result;
    }
}
