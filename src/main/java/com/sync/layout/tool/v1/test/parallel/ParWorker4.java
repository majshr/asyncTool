package com.sync.layout.tool.v1.test.parallel;

import com.jd.platform.async.executor.timer.SystemClock;
import com.sync.layout.tool.v1.callback.ICallback;
import com.sync.layout.tool.v1.callback.IWorker;
import com.sync.layout.tool.v1.result.WorkerResult;

/**
 * @author wuweifeng wrote on 2019-11-20.
 */
public class ParWorker4 implements IWorker<String, String>, ICallback<String, String> {

    @Override
    public String action(String object) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "result = " + SystemClock.now() + "---param = " + object + " from 4";
    }


    @Override
    public String defaultValue() {
        return "worker4--default";
    }

    @Override
    public void begin() {
        //System.out.println(Thread.currentThread().getName() + "- start --" + System.currentTimeMillis());
    }

    @Override
    public void result(boolean success, String param, WorkerResult<String> workResult) {
        if (success) {
            System.out.println("callback worker4 success--" + SystemClock.now() + "----" + workResult.getResult()
                    + "-threadName:" +Thread.currentThread().getName());
        } else {
            System.err.println("callback worker4 failure--" + SystemClock.now() + "----"  + workResult.getResult()
                    + "-threadName:" +Thread.currentThread().getName());
        }
    }

}
