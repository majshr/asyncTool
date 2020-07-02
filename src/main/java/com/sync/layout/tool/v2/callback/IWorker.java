package com.sync.layout.tool.v2.callback;

/**
 * 每个任务单元需要实现该接口
 * 
 * @author mengaijun
 * @Description: TODO
 * @date: 2020年3月20日 下午5:03:14
 */
public interface IWorker<T, V> {
    /**
     * 任务具体操作
     *
     * @param object
     *            任务执行的参数
     */
    V action(T object);

    /**
     * 超时、异常时，返回的默认值
     * @return 默认值
     */
    V defaultValue();
}
