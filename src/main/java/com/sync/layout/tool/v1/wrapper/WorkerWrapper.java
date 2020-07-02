package com.sync.layout.tool.v1.wrapper;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import javax.annotation.Generated;

import com.sync.layout.tool.v1.callback.ICallback;
import com.sync.layout.tool.v1.callback.IWorker;
import com.sync.layout.tool.v1.exception.DependErrorException;
import com.sync.layout.tool.v1.result.ResultState;
import com.sync.layout.tool.v1.result.WorkerResult;

/**
 * 任务包装器, 包装了前置任务和后续任务; 执行流程的判断和任务的启动执行<br>
 * 泛型: T为任务入参; V为执行结果返回值<br>
 * 
 * @author mengaijun
 * @Description: TODO
 * @date: 2020年3月5日 上午11:25:46
 */
public class WorkerWrapper<T, V> {
    /**
     * 执行任务的入参
     */
    private T param;

    /**
     * 任务
     */
    private IWorker<T, V> worker;

    /**
     * 任务执行的回调
     */
    private ICallback<T, V> callback;

    /**
     * 任务执行结果<br>
     * 任务执行结果提前分配好内存, 任务执行完成后, 直接设置值; 提前获取引用, 可以得到结果的值
     */
    private volatile WorkerResult<V> workerResult = WorkerResult.defaultResult();

    /** 任务执行完后, 可以执行的任务 */
    private Set<WorkerWrapper<T, V>> nextWorkers = new HashSet<>();

    /** 依赖的任务 */
    private Set<WorkerWrapper<T, V>> dependWorkers = new HashSet<>();
    /** 必须完成的依赖的任务 */
    private Set<WorkerWrapper<T, V>> mustDependWorkers = new HashSet<>();

    /**
     * 任务执行状态
     * 
     * @author mengaijun
     * @Description: TODO
     * @date: 2020年3月5日 上午11:24:55
     */
    private enum WorkerState {
        FINISH, ERROR, WORKING, INIT;
    }

    /** 任务状态; 初始未执行为状态为初始化; 作为对任务cas操作的入口 */
    private volatile WorkerState state = WorkerState.INIT;
    /** 任务状态更新器, 保证原子更新 */
    static AtomicReferenceFieldUpdater<WorkerWrapper, WorkerState> stateUpdater = AtomicReferenceFieldUpdater
            .newUpdater(WorkerWrapper.class, WorkerState.class, "state");

    public WorkerWrapper() {
        
    }

    public WorkerWrapper(T param, IWorker<T, V> worker, ICallback<T, V> callback) {
        super();
        if (worker == null) {
            throw new RuntimeException("执行任务不能为空!");
        }
        this.param = param;
        this.worker = worker;
        this.callback = callback;
    }

    /**
     * Creates builder to build {@link WorkerWrapper}.
     * 
     * @return created builder
     */
    @Generated("SparkTools")
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 开始工作, fromWrapper表示这次work是由哪个上游任务发起的
     * 
     * @param poolExecutor
     * @param dependWrapper
     *            依赖的任务
     * @param timeoutTime
     * @date: 2020年3月5日 下午4:38:24
     */
    public void work(ThreadPoolExecutor poolExecutor, WorkerWrapper dependWrapper) {
        // 如果不是初始状态, 说明已经被依赖任务启动了
        if (getState() != WorkerState.INIT) {
            return;
        }

        // 如果dependWrapper为null, 说明自己没有依赖的任务, 是第一个执行的(不存在并发启动)
        if (dependWrapper == null) {
            // 执行当前任务
            fire();
            // 使用线程池, 启动执行后边的任务
            beginNext(poolExecutor);
            return;
        }

        // 如果只有一个前置依赖任务, 没有并发问题，
        if (dependWorkers.size() == 1) {
            // dependWrapper就是依赖的那一个任务; 说明是dependWrapper依赖任务执行完了, 才启动的自己
            // 依赖的任务失败了，自己也设置失败；依赖的任务成功了，启动自己
            doDependsOneJob(dependWrapper);
            beginNext(poolExecutor);
            return;
        }

        // 有多个前置任务(此处可能会并发执行, 由多个依赖任务同时执行)
        if (doDependsJobs(poolExecutor, dependWrapper)) {
            beginNext(poolExecutor);
        }
    }



    /**
     * 有多个前置任务, 执行自己
     * 
     * @param poolExecutor
     * @param fromWrapper
     *            当前完成的依赖任务
     * @param timeoutTime
     * @return boolean
     * @date: 2020年3月5日 下午5:48:13
     */
    private boolean doDependsJobs(ThreadPoolExecutor poolExecutor, WorkerWrapper fromWrapper) {

        // 如果任务已经不是初始状态，说明已经被启动过
        if (state != WorkerState.INIT) {
            return false;
        }
        
        // 创建必须完成的上游wrapper集合
        Set<WorkerWrapper<T, V>> mustWrapper = getMustDependWorkers();

        // 标记当前完成的依赖的项目是否为必须完成的
        boolean nowDependIsMust = mustWrapper.contains(fromWrapper) ? true : false;

        // 如果任务为必须的，且失败了，设置当前任务快速失败
        if (nowDependIsMust && fromWrapper.getState() == WorkerState.ERROR) {
            return fastFail(new DependErrorException("依赖的任务错误"));
        }

        // ************* 判断必须的任务都完成了, 执行自己 ***********
        // 可能多线程并发执行
        // 如果必须依赖的任务都执行完成了, 可以执行自己
        for (WorkerWrapper mustDependWorker : mustWrapper) {
            // 如果有必需的任务没有执行完成, 直接返回, 不能执行自己
            if (mustDependWorker.getState() == WorkerState.INIT || mustDependWorker.getState() == WorkerState.WORKING) {
                return false;
            }
        }

        // 所有任务都执行完, 并且是finish, 开始执行自己(可能多线程并发访问)
        // fire本任务成功的线程, 再去执行本任务的之后的任务
        return fire();
    }

    /**
     * 有一个执行依赖的任务, 执行自己
     * 
     * @param fromWrapper
     * @return boolean
     * @date: 2020年3月5日 下午5:40:09
     */
    private boolean doDependsOneJob(WorkerWrapper dependWrapper) {
        // 依赖的任务执行失败了, 自己也就不用执行了
        if (ResultState.TIMEOUT == dependWrapper.getWorkResult().getResultState()) {
            return fastFail(null);
        } else if (ResultState.EXCEPTION == dependWrapper.getWorkResult().getResultState()) {
            return fastFail(dependWrapper.getWorkResult().getEx());
        } else {
            // 前面任务正常完毕了，该自己了(此处也为单线程执行, 没有并发)
            return fire();
        }
    }

    /**
     * 快速失败<br>
     * 任务状态为失败, 任务执行结果为失败
     * 
     * @param e
     *            异常信息
     * @return boolean
     * @date: 2020年3月5日 下午4:32:56
     */
    private boolean fastFail(Exception e) {
        // 是否更新任务结果成功
        boolean isUpdateSuccess = false;

        // 如果任务还是初始状态
        // 设置任务状态为失败, 设置任务执行结果为失败
        // 并发操作的话, 仅一个线程修改成功
        if (state == WorkerState.INIT) {
            isUpdateSuccess = stateUpdater.compareAndSet(this, WorkerState.INIT, WorkerState.ERROR);
            if (isUpdateSuccess) {
                // 设置结果状态
                workerResult.compareAndSetResult(ResultState.DEFAULT, ResultState.EXCEPTION);
                // 设置错误回调
                workerResult.setEx(e);
                workerResult.setResult(worker.defaultValue());
                callback.result(false, param, workerResult);
                return true;
            }

        }
        
        return false;
    }

    /**
     * 执行成功结果
     * 
     * @param result
     * @return WorkResult<V>
     * @date: 2020年3月6日 下午2:38:20
     */
    private WorkerResult<V> successResult(V result) {
        workerResult.setResult(result);
        workerResult.setResultState(ResultState.SUCCESS);
        return workerResult;
    }

    /**
     * 执行失败结果
     * 
     * @param e
     * @return WorkerResult<V>
     * @date: 2020年3月23日 下午3:28:14
     */
    private WorkerResult<V> failResult(Exception e) {
        workerResult.setEx(e);
        workerResult.setResult(null);
        workerResult.setResultState(ResultState.EXCEPTION);
        return workerResult;
    }

    /**
     * 启动当前任务; 可能存在多线程同时调用(到此处就是已经达到了执行条件)
     * 
     * @return boolean 成功执行返回true; 没有成功执行返回false
     * 
     * @date: 2020年3月5日 下午3:41:52
     */
    private boolean fire() {
        // 任务不是初始化状态, 表示已经被启动
        // 如果任务结果不是默认状态, 表示已经执行完成
        if (state != WorkerState.INIT || 
                workerResult.getResultState() != ResultState.DEFAULT) {
            return false;
        }
        
        // 启动任务; 如果多线程并发执行, 只有一个线程可以修改成功
        if (stateUpdater.compareAndSet(this, WorkerState.INIT, WorkerState.WORKING)) {
            try {
                // 任务启动前的回调
                callback.begin();
                // 执行任务
                V result = worker.action(param);
                // 设置结果值
                // 设置结果值需要发生在设置状态值之前（happen-before）
                workerResult = successResult(result);

                // 修改执行结果
                stateUpdater.compareAndSet(this, WorkerState.WORKING, WorkerState.FINISH);

                callback.result(true, param, workerResult);
            } catch (Exception e) {
                e.printStackTrace();
                // 设置错误结果
                stateUpdater.compareAndSet(this, WorkerState.WORKING, WorkerState.ERROR);
                // 设置为执行失败错误
                workerResult = failResult(e);
                callback.result(false, param, workerResult);
            }

            // 启动任务就算是fire成功
            return true;
        }

        return false;
    }

    /**
     * 执行后继的任务(阻塞操作, 会等待所有next都执行完成, 才返回)
     * 
     * @param poolExecutor
     * @param timeoutTime
     *            void
     * @date: 2020年3月5日 下午5:05:36
     */
    private void beginNext(ThreadPoolExecutor poolExecutor) {
        if (nextWorkers.isEmpty()) {
            return;
        }

        // 如果只有一个任务, 在当前线程执行, 避免线程切换
        if (nextWorkers.size() == 1) {
            // next的任务的上游就是this任务
            nextWorkers.forEach((nextWorker) -> {
                nextWorker.work(poolExecutor, this);
            });
            return;
        }

        // 有多个下个任务, 使用线程池异步执行
        CompletableFuture[] futures = new CompletableFuture[nextWorkers.size()];
        int i = 0;
        for (WorkerWrapper nextWorker : nextWorkers) {
            futures[i] = CompletableFuture
                    .runAsync(() -> nextWorker.work(poolExecutor, this), poolExecutor);
            ++i;
        }

        // 等待next所有执行完成, 不需要
        /*
        try {
            // 等待所有任务执行完成
            CompletableFuture.allOf(futures).get();
            // CompletableFuture.allOf(futures).get(timeout, unit);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        */
    }

    /**
     * Builder to build {@link WorkerWrapper}.
     */
    @Generated("SparkTools")
    public static final class Builder<T, V> {
        private T param;
        private IWorker<T, V> worker;
        private ICallback<T, V> callback;
        private Set<NextWrapper<T, V>> nextWorkers = new HashSet();
        private Set<DependWrapper<T, V>> dependWorkers = new HashSet();

        private Builder() {
        }

        public Builder withParam(T param) {
            this.param = param;
            return this;
        }

        public Builder withWorker(IWorker<T, V> worker) {
            this.worker = worker;
            return this;
        }

        public Builder withCallback(ICallback<T, V> callback) {
            this.callback = callback;
            return this;
        }

        /**
         * 添加下一个任务, 默认当前任务必须完成, 才能执行下一个
         * 
         * @param nextWorkersParam
         *            下个任务
         * @return Builder
         * @date: 2020年3月6日 下午5:23:17
         */
        public Builder withNextWorkers(WorkerWrapper<T, V>... nextWorkersParam) {
            if (nextWorkersParam == null) {
                return this;
            }

            for (WorkerWrapper<T, V> nextWorker : nextWorkersParam) {
                withNextWorker(nextWorker, true);
            }
            return this;
        }

        /**
         * 添加下一个任务
         * 
         * @param nextWorkersParam
         * @return Builder
         * @date: 2020年3月6日 下午5:23:39
         */
        public Builder withNextWorkers(NextWrapper<T, V>... nextWorkersParam) {
            if (nextWorkersParam == null) {
                return this;
            }

            for (NextWrapper<T, V> nextWorker : nextWorkersParam) {
                nextWorkers.add(nextWorker);
            }
            return this;
        }

        /**
         * 根据当前任务是否必须完成, 添加下一个任务
         * 
         * @param nextWorkerParam
         *            下一个任务
         * @param selfIfMust
         *            是否当前任务必须执行完, 下一个任务才能执行
         * @return Builder
         * @date: 2020年3月6日 下午5:23:52
         */
        public Builder withNextWorker(WorkerWrapper<T, V> nextWorkerParam, boolean selfIfMust) {
            if (nextWorkerParam == null) {
                return this;
            }

            withNextWorkers(new NextWrapper(nextWorkerParam, selfIfMust));
            return this;
        }

        /**
         * 添加依赖的任务
         * 
         * @param dependWorkersParam
         * @return Builder
         * @date: 2020年3月10日 上午10:20:07
         */
        public Builder withDependWorkers(DependWrapper<T, V>... dependWorkersParam) {
            if (dependWorkersParam == null) {
                return this;
            }

            for (DependWrapper<T, V> dependWorker : dependWorkersParam) {
                dependWorkers.add(dependWorker);
            }
            return this;
        }

        /**
         * 添加依赖的任务
         * 
         * @param dependWorker
         * @param isMust
         *            是否依赖的任务必须执行完
         * @return Builder
         * @date: 2020年3月10日 上午10:20:19
         */
        public Builder withDependWorkers(WorkerWrapper<T, V> dependWorker, boolean isMust) {
            withDependWorkers(new DependWrapper<>(dependWorker, isMust));
            return this;
        }

        /**
         * 添加依赖的任务; 默认必须执行完
         * 
         * @param dependWorkersParam
         * @return Builder
         * @date: 2020年3月10日 上午10:20:25
         */
        public Builder withDependWorkers(WorkerWrapper<T, V>... dependWorkersParam) {
            if (dependWorkersParam == null) {
                return this;
            }

            for (WorkerWrapper<T, V> dependWorker : dependWorkersParam) {
                withDependWorkers(new DependWrapper<>(dependWorker, true));
            }
            return this;
        }

        /**
         * 创建任务对象
         * 
         * @return WorkerWrapper
         * @date: 2020年3月6日 下午4:11:55
         */
        public WorkerWrapper build() {
            WorkerWrapper<T, V> workerWrapper = new WorkerWrapper(param, worker, callback);

            // 根据nextWrappers设置信息
            for (NextWrapper<T, V> nextWrapper : nextWorkers) {
                // 设置当前任务的next信息
                workerWrapper.getNextWorkers().add(nextWrapper.getNextWrapper());
                // 设置当前任务的为next任务的depend
                nextWrapper.getNextWrapper().getDependWorkers().add(workerWrapper);
                // 如果自己是必须的, 则当前任务为next的mustDepend
                if(nextWrapper.isSelfIfMust()) {
                    nextWrapper.getNextWrapper().getMustDependWorkers().add(workerWrapper);
                }
            }

            // 根据dependWrappers设置信息
            for (DependWrapper<T, V> dependWrapper : dependWorkers) {
                // 当前任务为依赖任务的next
                dependWrapper.getDependWrapper().getNextWorkers().add(workerWrapper);
                // 设置当前任务的depend信息
                workerWrapper.getDependWorkers().add(dependWrapper.getDependWrapper());
                // 如果依赖任务是必须的, 依赖任务是当前任务的mustDepend
                if (dependWrapper.isMust()) {
                    workerWrapper.getMustDependWorkers().add(dependWrapper.getDependWrapper());
                }
            }

            return workerWrapper;
        }
    }

    public WorkerResult<V> getWorkResult() {
        return workerResult;
    }

    public WorkerState getState() {
        return state;
    }

    public void setState(WorkerState state) {
        this.state = state;
    }

    public T getParam() {
        return param;
    }

    public void setParam(T param) {
        this.param = param;
    }

    public Set<WorkerWrapper<T, V>> getNextWorkers() {
        return nextWorkers;
    }

    public void setNextWorkers(Set<WorkerWrapper<T, V>> nextWorkers) {
        this.nextWorkers = nextWorkers;
    }

    public Set<WorkerWrapper<T, V>> getDependWorkers() {
        return dependWorkers;
    }

    public void setDependWorkers(Set<WorkerWrapper<T, V>> dependWorkers) {
        this.dependWorkers = dependWorkers;
    }

    public Set<WorkerWrapper<T, V>> getMustDependWorkers() {
        return mustDependWorkers;
    }

    public void setMustDependWorkers(Set<WorkerWrapper<T, V>> mustDependWorkers) {
        this.mustDependWorkers = mustDependWorkers;
    }

    public IWorker<T, V> getWorker() {
        return worker;
    }

    public void setWorker(IWorker<T, V> worker) {
        this.worker = worker;
    }

    public WorkerResult<V> getWorkerResult() {
        return workerResult;
    }

    public void setWorkerResult(WorkerResult<V> workerResult) {
        this.workerResult = workerResult;
    }

    public ICallback<T, V> getCallback() {
        return callback;
    }

    public void setCallback(ICallback<T, V> callback) {
        this.callback = callback;
    }

}
