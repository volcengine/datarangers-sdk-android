// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * fork from com.bytedance.component.silk.road:mohist-standard-tools:0.0.19
 *
 * <p>线程池
 *
 * @author luodong.seu
 */
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class TTExecutors {

    private static ExecutorService sNormalThreadPool;

    public static final int AVAILABLEPROCESSORS = Runtime.getRuntime().availableProcessors();
    public static final int CPU_COUNT = AVAILABLEPROCESSORS > 0 ? AVAILABLEPROCESSORS : 1;
    public static final int CORE_POOL_SIZE_NORMAL = 2 * Math.max(2, Math.min(CPU_COUNT - 1, 6));
    public static final int MAXIMUM_POOL_SIZE_NORMAL = CORE_POOL_SIZE_NORMAL * 2 + 1;
    public static final int KEEP_ALIVE_SECONDS = 30;

    private static final BlockingQueue<Runnable> S_POOLWORK_QUEUE = new LinkedBlockingQueue<>();
    private static final DefaultThreadFactory S_DEFAULT_THREAD_FACTORY =
            new DefaultThreadFactory("TTDefaultExecutors");

    // 正常不会到这里
    private static final RejectedExecutionHandler S_HANDLER =
            new RejectedExecutionHandler() {
                @Override
                public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                    Executors.newCachedThreadPool().execute(r);
                }
            };

    /**
     * The default thread factory.
     */
    private static class DefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        DefaultThreadFactory(String factoryTag) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = factoryTag + "-" + POOL_NUMBER.getAndIncrement() + "-Thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

    static {
        sNormalThreadPool =
                new TTThreadPoolExecutor(
                        CORE_POOL_SIZE_NORMAL,
                        MAXIMUM_POOL_SIZE_NORMAL,
                        KEEP_ALIVE_SECONDS,
                        TimeUnit.SECONDS,
                        S_POOLWORK_QUEUE,
                        S_DEFAULT_THREAD_FACTORY,
                        S_HANDLER);
        ((TTThreadPoolExecutor) sNormalThreadPool).allowCoreThreadTimeOut(true);
    }

    /**
     * 正常通用线程池
     *
     * @return ExecutorService
     */
    public static ExecutorService getNormalExecutor() {
        return sNormalThreadPool;
    }

    /**
     * 替换内部的NormalThreadPool TTExecutors提供注入能力，给线程池收敛提供功配置注入的接口，一般情况下不要覆写
     *
     * @param normalThreadPool ExecutorService
     */
    public static void setNormalThreadPool(ExecutorService normalThreadPool) {
        sNormalThreadPool = normalThreadPool;
    }
}
