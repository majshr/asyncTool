package com.sync.layout.tool.v1.test;

import com.sync.layout.tool.v1.callback.ICallback;
import com.sync.layout.tool.v1.callback.IWorker;
import com.sync.layout.tool.v1.result.WorkerResult;

/**
 * @author wuweifeng wrote on 2019-11-20.
 */
public class DeWorker1 implements IWorker<WorkerResult<User>, User>, ICallback<WorkerResult<User>, User> {

    @Override
    public User action(WorkerResult<User> result) {
        System.out.println("par1的入参来自于par0： " + result.getResult());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new User("user1");
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
    public void result(boolean success, WorkerResult<User> param, WorkerResult<User> workResult) {
        System.out.println("worker1 的结果是：" + workResult.getResult());
    }

}
