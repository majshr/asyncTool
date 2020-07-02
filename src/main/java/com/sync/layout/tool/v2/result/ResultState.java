package com.sync.layout.tool.v2.result;

/**
 * 任务执行结果状态
 * 
 * @author mengaijun
 * @Description: TODO
 * @date: 2020年3月10日 上午11:20:33
 */
public enum ResultState {
    SUCCESS, // 成功
    TIMEOUT, // 失败
    EXCEPTION, // 超时
    DEFAULT // 默认状态
}
