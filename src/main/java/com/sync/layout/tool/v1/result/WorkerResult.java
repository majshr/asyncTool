package com.sync.layout.tool.v1.result;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * 执行结果
 * 
 * @author mengaijun
 * @Description: TODO
 * @date: 2020年3月10日 上午11:20:48
 */
public class WorkerResult<V> {
    /**
     * 执行的结果，设置结果信息happen-before于volatile的状态值
     */
    private volatile V result;

    /**
     * 结果状态
     */
    private volatile ResultState resultState;
    /** 结果状态更新器 */
    private static AtomicReferenceFieldUpdater<WorkerResult, ResultState> resultStateUpdater = AtomicReferenceFieldUpdater
            .newUpdater(
            WorkerResult.class,
            ResultState.class, "resultState");

    /**
     * 异常信息
     */
    private volatile Exception ex;

    public WorkerResult(V result, ResultState resultState) {
        this(result, resultState, null);
    }

    public WorkerResult(V result, ResultState resultState, Exception ex) {
        this.result = result;
        this.resultState = resultState;
        this.ex = ex;
    }

    /**
     * 默认结果(ResultState为DEFAULT)
     * 
     * @return WorkerResult<V>
     * @date: 2020年3月20日 下午5:16:09
     */
    public static <V> WorkerResult<V> defaultResult() {
        return new WorkerResult<>(null, ResultState.DEFAULT);
    }

    /**
     * 原子的比较和更新值
     * 
     * @param expect
     *            期望值
     * @param update
     *            更新后的值
     * @return boolean
     * @date: 2020年3月9日 上午10:48:54
     */
    public boolean compareAndSetResult(ResultState expect, ResultState update) {
        return resultStateUpdater.compareAndSet(this, expect, update);
    }

    @Override
    public String toString() {
        return "WorkResult{" + "result=" + result + ", resultState=" + resultState + ", ex=" + ex + '}';
    }

    public Exception getEx() {
        return ex;
    }

    public void setEx(Exception ex) {
        this.ex = ex;
    }

    public V getResult() {
        return result;
    }

    public void setResult(V result) {
        this.result = result;
    }

    public ResultState getResultState() {
        return resultState;
    }

    public void setResultState(ResultState resultState) {
        this.resultState = resultState;
    }
}
