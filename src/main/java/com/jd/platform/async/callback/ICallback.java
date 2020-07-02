package com.jd.platform.async.callback;

import com.jd.platform.async.worker.WorkResult;

/**
 * 每个执行单元执行完毕后，会回调该接口</p>
 * 需要监听执行结果的，实现该接口即可
 * @author wuweifeng wrote on 2019-11-19.
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
     * 耗时操作执行完毕后，就给value注入值
     *
     */
    void result(boolean success, T param, WorkResult<V> workResult);
}
