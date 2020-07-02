package com.sync.layout.tool.v1.wrapper;

import java.util.Objects;

/**
 * 对依赖任务的包装
 * 
 * @author mengaijun
 * @Description: TODO
 * @date: 2020年3月10日 上午11:22:36
 */
public class DependWrapper<T, V> {

    /**
     * 依赖的任务的具体信息
     */
    private WorkerWrapper<T, V> dependWrapper;

    /**
     * 是否该依赖必须完成后才能执行自己.<p>
     * 因为存在一个任务，依赖于多个任务，是让这多个任务全部完成后才执行自己，还是某几个执行完毕就可以执行自己<br/>
     * 如     <br/>
     * 1         <br/>
     * ---3      <br/>
     * 2         <br/>
     * 或                     <br/>
     * 1---3     <br/>
     * 2---3     <br/>
     * 这两种就不一样，上面的就是必须12都完毕，才能3  <br/>
     * 下面的就是1完毕就可以3
     */
    private boolean must = true;

    public DependWrapper(WorkerWrapper<T, V> dependWrapper, boolean must) {
        this.dependWrapper = dependWrapper;
        this.must = must;
    }

    @Override
    public int hashCode() {
        return dependWrapper == null ? 0 : dependWrapper.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return Objects.equals(this, obj);
    }

    public DependWrapper() {
    }

    public WorkerWrapper<T, V> getDependWrapper() {
        return dependWrapper;
    }

    public void setDependWrapper(WorkerWrapper<T, V> dependWrapper) {
        this.dependWrapper = dependWrapper;
    }

    public boolean isMust() {
        return must;
    }

    public void setMust(boolean must) {
        this.must = must;
    }

    @Override
    public String toString() {
        return "DependWrapper{" +
                "dependWrapper=" + dependWrapper +
                ", must=" + must +
                '}';
    }
}
