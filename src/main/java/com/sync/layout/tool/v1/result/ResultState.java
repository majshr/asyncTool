package com.sync.layout.tool.v1.result;

/**
 * 任务执行结果状态
 * 
 * @author mengaijun
 * @Description: TODO
 * @date: 2020年3月10日 上午11:20:33
 */
public enum ResultState {
    /**
     * 成功
     */
    SUCCESS,
    /**
     * 失败
     */
    TIMEOUT,
    /**
     * 超时
     */
    EXCEPTION,
    /**
     * 默认状态
     */
    DEFAULT
}
