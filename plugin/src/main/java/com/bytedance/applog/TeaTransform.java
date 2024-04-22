// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.android.utils.FileUtils;
import com.bytedance.applog.util.Log;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.Project;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/**
 * @author shiyanlong
 * @date 2019/1/11
 */
class TeaTransform extends Transform {

    public static Iterable<String> BLACK_LIST;

    /** 埋点黑名单 */
    public static Iterable<String> TRACK_BLACK_LIST;

    public static boolean AUTO_INJECT_WEBVIEW_BRIDGE = true;

    public static boolean DISABLE_AUTO_TRACK = false;

    private final Project mProject;

    /** 忽略类的前缀列表 */
    private final List<String> ignoreClassPrefix = new ArrayList<>();

    @Override
    public String getName() {
        return "TeaTransform";
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    TeaTransform(Project project) {
        mProject = project;
    }

    @Override
    public void transform(TransformInvocation invocation) {
        try {
            Log.i("TeaTransform begin");
            TeaExtension extension = mProject.getExtensions().getByType(TeaExtension.class);
            BLACK_LIST = extension.blackList;
            if (null != BLACK_LIST && BLACK_LIST.iterator().hasNext()) {
                Log.i("Transform black list: " + String.join(",", BLACK_LIST));
            }
            TRACK_BLACK_LIST = extension.trackBlackList;
            if (null != TRACK_BLACK_LIST && TRACK_BLACK_LIST.iterator().hasNext()) {
                Log.i("Track black list: " + String.join(",", TRACK_BLACK_LIST));

                // 如果不采集OAID，则删除dr/aidl和dr/impl下的所有类
                if (isInTrackBlackList("OAID")) {
                    ignoreClassPrefix.add("com/bytedance/dr/aidl");
                    ignoreClassPrefix.add("com/bytedance/dr/impl");
                    ignoreClassPrefix.add("com/bytedance/dr/honor");
                }
            }
            if (null != extension.autoInjectWebViewBridge) {
                AUTO_INJECT_WEBVIEW_BRIDGE = extension.autoInjectWebViewBridge;
            }
            if (null != extension.disableAutoTrack) {
                DISABLE_AUTO_TRACK = extension.disableAutoTrack;
            }
            for (TransformInput input : invocation.getInputs()) {
                for (DirectoryInput direct : input.getDirectoryInputs()) {
                    File newDirect =
                            invocation
                                    .getOutputProvider()
                                    .getContentLocation(
                                            direct.getName(),
                                            direct.getContentTypes(),
                                            direct.getScopes(),
                                            Format.DIRECTORY);
                    processDirectory(direct.getFile(), newDirect);
                }
                for (JarInput jar : input.getJarInputs()) {
                    String jarName = DigestUtils.md5Hex(jar.getFile().getAbsolutePath());
                    File newJar =
                            invocation
                                    .getOutputProvider()
                                    .getContentLocation(
                                            jarName,
                                            jar.getContentTypes(),
                                            jar.getScopes(),
                                            Format.JAR);
                    processJar(jar.getFile(), newJar);
                }
            }
        } catch (Throwable e) {
            Log.e(e);
        } finally {
            Log.i("TeaTransform end");
        }
    }

    private void processDirectory(File directory, File newDirectory) throws IOException {
        FileUtils.mkdirs(newDirectory);
        File[] children = directory.listFiles();
        if (children != null) {
            for (File child : children) {
                String childName = child.getName();
                File newChild = new File(newDirectory, childName);
                if (child.isFile()) {
                    if (isSuspectClassName(childName)) {
                        if (ignoreClasses(childName)) {
                            continue;
                        }
                        HashMap<String, MethodChanger> lambdaMethodsMap;
                        try (BufferedInputStream bis =
                                        new BufferedInputStream(new FileInputStream(child));
                                BufferedOutputStream bos =
                                        new BufferedOutputStream(new FileOutputStream(newChild))) {
                            lambdaMethodsMap = processClass(bis, bos);
                        }
                        if (lambdaMethodsMap != null && lambdaMethodsMap.size() > 0) {
                            File newChild2 = new File(newDirectory, "middle" + childName);
                            FileUtils.copyFile(newChild, newChild2);
                            try (BufferedInputStream bis =
                                            new BufferedInputStream(
                                                    new FileInputStream(newChild2));
                                    BufferedOutputStream bos =
                                            new BufferedOutputStream(
                                                    new FileOutputStream(newChild))) {
                                processClassForLambda(lambdaMethodsMap, bis, bos);
                            }
                            FileUtils.delete(newChild2);
                        }
                    } else {
                        FileUtils.copyFile(child, newChild);
                    }
                } else if (child.isDirectory()) {
                    processDirectory(child, newChild);
                } else {
                    throw new RuntimeException("oops");
                }
            }
        }
    }

    private boolean isSuspectClassName(final String name) {
        return name.endsWith(".class")
                && !name.startsWith("R$")
                && !"R.class".equals(name)
                && !"BuildConfig.class".equals(name);
    }

    private void processJar(File jar, File newJar) throws IOException {
        try (JarOutputStream jos =
                        new JarOutputStream(
                                new BufferedOutputStream(new FileOutputStream(newJar)));
                JarFile jf = new JarFile(jar)) {
            Enumeration<JarEntry> enumeration = jf.entries();
            while (enumeration.hasMoreElements()) {
                JarEntry entry = enumeration.nextElement();
                String entryName = entry.getName();
                if (ignoreClasses(entryName)) {
                    continue;
                }
                ZipEntry zipEntry = new ZipEntry(entryName);
                try (InputStream is = jf.getInputStream(zipEntry)) {
                    jos.putNextEntry(zipEntry);
                    if (isSuspectClassName(entryName)) {
                        // 这里也要处理lambda表达式
                        ByteArrayOutputStream tempBos = new ByteArrayOutputStream();
                        HashMap<String, MethodChanger> lambdaMethodsMap = processClass(is, tempBos);
                        if (null != lambdaMethodsMap && lambdaMethodsMap.size() > 0) {
                            ByteArrayInputStream lambdaIns =
                                    new ByteArrayInputStream(tempBos.toByteArray());
                            tempBos = new ByteArrayOutputStream();
                            processClassForLambda(lambdaMethodsMap, lambdaIns, tempBos);
                        }
                        IOUtils.copy(new ByteArrayInputStream(tempBos.toByteArray()), jos);
                    } else {
                        IOUtils.copy(is, jos);
                    }
                }
            }
        }
    }

    private HashMap<String, MethodChanger> processClass(
            final InputStream bis, final OutputStream bos) throws IOException {
        ClassReader reader = new ClassReader(bis);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        TeaClassVisitor cv = new TeaClassVisitor(writer);
        reader.accept(cv, ClassReader.EXPAND_FRAMES);
        bos.write(writer.toByteArray());
        return cv.getNeedToHookForLambda();
    }

    public void setAndroidJars(ArrayList fileList) {
        // TODO: 2019/1/13
    }

    /** revisit methods again for lambda, not a good solution, need to find another way */
    private void processClassForLambda(
            HashMap<String, MethodChanger> lambdaMethodsMap,
            final InputStream bis,
            final OutputStream bos)
            throws IOException {
        ClassReader reader = new ClassReader(bis);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = new LambdaClassVisitor(writer, lambdaMethodsMap);
        reader.accept(cv, ClassReader.EXPAND_FRAMES);
        bos.write(writer.toByteArray());
    }

    /**
     * 是否在埋点黑名单中
     *
     * @param name 埋点名
     * @return true 在
     */
    public static boolean isInTrackBlackList(String name) {
        if (null != TRACK_BLACK_LIST && null != name) {
            for (Iterator<String> it = TRACK_BLACK_LIST.iterator(); it.hasNext(); ) {
                String item = it.next();
                if (name.equals(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断指定类是否被忽略
     *
     * @param className 类名
     * @return true: 忽略
     */
    private boolean ignoreClasses(String className) {
        if (null == className) {
            return true;
        }
        for (String ignore : ignoreClassPrefix) {
            if (className.startsWith(ignore)) {
                return true;
            }
        }
        return false;
    }
}
