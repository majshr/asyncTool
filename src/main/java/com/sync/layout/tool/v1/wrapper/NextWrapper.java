package com.sync.layout.tool.v1.wrapper;

import java.util.Objects;

/**
 * 下一个任务的wrapper的封装
 * 
 * @author mengaijun
 * @Description: TODO
 * @date: 2020年3月10日 上午11:26:47
 */
public class NextWrapper<T, V> {

    /**
     * 下一个的任务的具体信息
     */
    private WorkerWrapper<T, V> nextWrapper;

    /**
     * 是否该当前任务必须完成后才能执行下一个.
     * <p>
     */
    private boolean selfIfMust = true;

    public NextWrapper(WorkerWrapper<T, V> nextWrapper, boolean selfIfMust) {
        this.nextWrapper = nextWrapper;
        this.selfIfMust = selfIfMust;
    }

    @Override
    public int hashCode() {
        return nextWrapper == null ? 0 : nextWrapper.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return Objects.equals(this, obj);
    }

    public NextWrapper() {
    }

    public WorkerWrapper<T, V> getNextWrapper() {
        return nextWrapper;
    }

    public void setNextWrapper(WorkerWrapper<T, V> nextWrapper) {
        this.nextWrapper = nextWrapper;
    }

    public boolean isSelfIfMust() {
        return selfIfMust;
    }

    public void setSelfIfMust(boolean selfIfMust) {
        this.selfIfMust = selfIfMust;
    }

    @Override
    public String toString() {
        return "nextWrapper{" + "nextWrapper=" + nextWrapper + ", selfIfMust=" + selfIfMust + '}';
    }
}
