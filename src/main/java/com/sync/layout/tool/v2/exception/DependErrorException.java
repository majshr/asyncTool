package com.sync.layout.tool.v2.exception;

/**
 * 依赖的任务错误异常
 * 
 * @author mengaijun
 * @Description: TODO
 * @date: 2020年3月6日 上午11:06:57
 */
public class DependErrorException extends RuntimeException {

    private static final long serialVersionUID = 580689212248839993L;

    public DependErrorException() {
        super();
    }

    public DependErrorException(String message) {
        super(message);
    }
}
