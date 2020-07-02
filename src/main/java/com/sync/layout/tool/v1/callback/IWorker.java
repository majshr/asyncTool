package com.sync.layout.tool.v1.callback;

/**
 * 每个最小执行单元需要实现该接口
 * 
 * @author mengaijun
 * @Description: TODO
 * @date: 2020年3月20日 下午5:03:14
 */
public interface IWorker<T, V> {
    /**
     * 在这里做耗时操作，如rpc请求、IO等
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
