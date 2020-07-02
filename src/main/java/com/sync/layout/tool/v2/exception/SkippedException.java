package com.sync.layout.tool.v2.exception;

/**
 * 如果任务在执行之前，自己后面的任务已经执行完或正在被执行，则抛该exception
 * 
 * @author mengaijun
 * @Description: TODO
 * @date: 2020年5月20日 下午2:19:37
 */
public class SkippedException extends RuntimeException {
    private static final long serialVersionUID = 6012467353854867630L;

    public SkippedException() {
        super();
    }

    public SkippedException(String message) {
        super(message);
    }
}
