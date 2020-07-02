package com.sync.layout.tool.v1.test;

import java.util.concurrent.ExecutionException;

import com.jd.platform.async.executor.Async;
import com.sync.layout.tool.v1.executor.ExecutorUtil;
import com.sync.layout.tool.v1.result.WorkerResult;
import com.sync.layout.tool.v1.wrapper.WorkerWrapper;

/**
 * 后面请求依赖于前面请求的执行结果
 * 
 * @author wuweifeng wrote on 2019-12-26
 * @version 1.0
 */
public class Test {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        DeWorker w = new DeWorker();
        DeWorker1 w1 = new DeWorker1();
        DeWorker2 w2 = new DeWorker2();

        WorkerWrapper<WorkerResult<User>, String> workerWrapper2 = WorkerWrapper.builder()
                .withWorker(w2).withCallback(w2).build();

        WorkerWrapper<WorkerResult<User>, User> workerWrapper1 = WorkerWrapper.builder()
                .withWorker(w1).withCallback(w1).withNextWorkers(workerWrapper2).build();

        WorkerWrapper<String, User> workerWrapper = WorkerWrapper.builder().withWorker(w).withParam("0")
                .withNextWorkers(workerWrapper1).withCallback(w).build();
        // 虽然尚未执行，但是也可以先取得结果的引用，作为下一个任务的入参
        WorkerResult<User> result = workerWrapper.getWorkResult();
        WorkerResult<User> result1 = workerWrapper1.getWorkResult();

        workerWrapper1.setParam(result);
        workerWrapper2.setParam(result1);

        // Async.beginWork(3500, workerWrapper);
        ExecutorUtil.beginWork(3500, workerWrapper);

        System.out.println(workerWrapper2.getWorkResult());
        Async.shutDown();
    }
}
