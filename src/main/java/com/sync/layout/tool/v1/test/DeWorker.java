package com.sync.layout.tool.v1.test;

import com.sync.layout.tool.v1.callback.ICallback;
import com.sync.layout.tool.v1.callback.IWorker;
import com.sync.layout.tool.v1.result.WorkerResult;

public class DeWorker implements IWorker<String, User>, ICallback<String, User> {

    @Override
    public User action(String object) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new User("user0");
    }


    @Override
    public User defaultValue() {
        return new User("default User");
    }

    @Override
    public void begin() {
        //System.out.println(Thread.currentThread().getName() + "- start --" + System.currentTimeMillis());
    }

    @Override
    public void result(boolean success, String param, WorkerResult<User> workResult) {
        System.out.println("worker0 的结果是：" + workResult.getResult());
    }

}
