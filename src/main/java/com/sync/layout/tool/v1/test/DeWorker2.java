package com.sync.layout.tool.v1.test;

import com.sync.layout.tool.v1.callback.ICallback;
import com.sync.layout.tool.v1.callback.IWorker;
import com.sync.layout.tool.v1.result.WorkerResult;

public class DeWorker2 implements IWorker<WorkerResult<User>, String>, ICallback<WorkerResult<User>, String> {

    @Override
    public String action(WorkerResult<User> result) {
        System.out.println("par2的入参来自于par1： " + result.getResult());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result.getResult().getName();
    }


    @Override
    public String defaultValue() {
        return "default";
    }

    @Override
    public void begin() {
        //System.out.println(Thread.currentThread().getName() + "- start --" + System.currentTimeMillis());
    }

    @Override
    public void result(boolean success, WorkerResult<User> param, WorkerResult<String> workResult) {
        System.out.println("worker2 的结果是：" + workResult.getResult());
    }

}
