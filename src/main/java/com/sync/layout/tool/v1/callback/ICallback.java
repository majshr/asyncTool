package com.sync.layout.tool.v1.callback;

import com.sync.layout.tool.v1.result.WorkerResult;

/**
 * 每个执行单元执行完毕后，会回调该接口<br>
 * 需要监听执行结果的，实现该接口即可
 */
public interface ICallback<T, V> {

    /**
     * 任务开始执行前的回调
     * 
     * void 
     * @date: 2020年3月4日 上午10:49:50
     */
    void begin();

    /**
     * 任务执行完成后的回调
     * 
     * @param success
     *            执行是否成功
     * @param param
     *            任务执行的参数
     * @param workResult
     *            任务执行结果
     * @date: 2020年3月20日 下午4:57:32
     */
    void result(boolean success, T param, WorkerResult<V> workResult);
}
