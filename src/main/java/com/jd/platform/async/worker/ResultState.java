package com.jd.platform.async.worker;

/**
 * 结果状态
 * @author wuweifeng wrote on 2019-11-19.
 */
public enum ResultState {
    SUCCESS, // 成功
    TIMEOUT, // 失败
    EXCEPTION, // 超时
    DEFAULT  //默认状态
}
