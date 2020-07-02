package com.sync.layout.tool.v1.callback;

import java.util.List;

import com.sync.layout.tool.v1.wrapper.WorkerWrapper;

/**
 * 异步任务完成的回调方法
 * 
 * @author mengaijun
 * @Description: TODO
 * @date: 2020年3月10日 下午2:47:19
 */
public interface AsyncWorkersFinishCallback {
    /**
     * 任务都执行成功后, 可以在任务包装类中获取结果
     * 
     * @param workerWrappers
     * @date: 2020年3月10日 下午2:50:08
     */
    void success(List<WorkerWrapper<?, ?>> workerWrappers);

    /**
     * 任务执行失败回调
     * 
     * @param workerWrappers
     * @param e
     *            void
     * @date: 2020年3月10日 下午2:51:25
     */
    void failure(List<WorkerWrapper<?, ?>> workerWrappers, Throwable e);
}
