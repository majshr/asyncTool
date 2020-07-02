package com.sync.layout.tool.v1.executor;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.sync.layout.tool.v1.callback.AsyncWorkersFinishCallback;
import com.sync.layout.tool.v1.wrapper.WorkerWrapper;

/**
 * 任务执行工具类
 * 
 * @author mengaijun
 * @Description: TODO
 * @date: 2020年3月10日 上午11:28:20
 */
public class ExecutorUtil {
    public static volatile ThreadPoolExecutor COMMON_POOL;

    public static ThreadPoolExecutor getCommonPool() {
        if (COMMON_POOL == null) {
            synchronized (ExecutorUtil.class) {
                if (COMMON_POOL == null) {
                    COMMON_POOL = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 2, 1024, 15L,
                            TimeUnit.SECONDS, new LinkedBlockingQueue<>(), (ThreadFactory) Thread::new);
                }
            }
        }
        return COMMON_POOL;
    }


    /**
     * 开始任务, 使用默认线程池, 同步阻塞, 直到所有任务执行完成, 返回
     * 
     * @param timeout
     * @param workerWrappers
     * @return boolean
     * @date: 2020年3月6日 上午11:35:48
     */
    public static boolean beginWork(long timeout, WorkerWrapper<?, ?>... workerWrappers) {
        return beginWork(getCommonPool(), workerWrappers);
    }

    /**
     * 开始任务, 使用自定义线程池, 同步, 所有任务执行完成, 才返回
     * 
     * @param pool
     * @param workerWrapper
     * @return boolean
     * @date: 2020年3月6日 上午11:35:58
     */
    public static boolean beginWork(ThreadPoolExecutor pool, WorkerWrapper<?, ?>... workerWrapper) {
        if (workerWrapper == null || workerWrapper.length == 0) {
            return false;
        }

        List<WorkerWrapper> workerWrappers = Arrays.stream(workerWrapper).collect(Collectors.toList());
        CompletableFuture[] futures = new CompletableFuture[workerWrappers.size()];
        for (int i = 0; i < workerWrappers.size(); i++) {
            WorkerWrapper wrapper = workerWrappers.get(i);
            // 第一层任务没有依赖的任务
            futures[i] = CompletableFuture.runAsync(() -> wrapper.work(pool, null), pool);
        }

        // 阻塞到所有任务都执行完成
        /*
        try {
            CompletableFuture.allOf(futures).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
*/
        return true;
    }

    /**
     * 异步执行任务
     * 
     * @param timeout
     * @param workerWrapper
     * @return boolean
     * @date: 2020年3月10日 上午11:56:07
     */
    public static boolean beginWorkAsync(long timeout, AsyncWorkersFinishCallback callback,
            WorkerWrapper<?, ?>... workerWrappers) {
        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
            return beginWork(getCommonPool(), workerWrappers);
        } , getCommonPool());

        // 异步完成后回调
        future.whenComplete((result, e) -> {
            if (result) {
                callback.success(Arrays.asList(workerWrappers));
            } else {
                callback.failure(Arrays.asList(workerWrappers), e);
            }

        });
        
        return true;
    }

    /**
     * 关闭默认线程池
     * 
     * @date: 2020年3月10日 上午11:34:46
     */
    public static void shutDown() {
        if (COMMON_POOL != null) {
            synchronized (ExecutorUtil.class) {
                if (COMMON_POOL != null) {
                    COMMON_POOL.shutdown();
                    COMMON_POOL = null;
                }
            }
        }
    }

    public static String getThreadCount() {
        return "activeCount=" + COMMON_POOL.getActiveCount() + "  completedCount " + COMMON_POOL.getCompletedTaskCount()
                + "  largestCount " + COMMON_POOL.getLargestPoolSize();
    }
}
