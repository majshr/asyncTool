package com.sync.layout.tool.v2.worker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Generated;

import com.sync.layout.tool.v1.wrapper.WorkerWrapper;
import com.sync.layout.tool.v2.cache.TaskStatusCache;
import com.sync.layout.tool.v2.callback.ICallback;
import com.sync.layout.tool.v2.callback.IWorker;
import com.sync.layout.tool.v2.exception.DependErrorException;
import com.sync.layout.tool.v2.result.ResultState;
import com.sync.layout.tool.v2.result.WorkerResult;
import com.sync.layout.tool.v2.util.StringUtil;

public class Worker<T, V> implements IWorker<T, V> {

    private String jobName;

    /**
     * 任务名字
     */
    private String name;

    private T param;

    private IWorker<T, V> realWorker;

    private ICallback<T, V> callback;

    /**
     * 任务执行结果<br>
     * 任务执行结果提前分配好内存, 任务执行完成后, 直接设置值; 提前获取引用, 可以得到结果的值
     */
    private volatile WorkerResult<V> workerResult = WorkerResult.defaultResult();

    /** 任务执行完后, 可以执行的任务 */
    private Set<Worker<T, V>> nextWorkers = new HashSet<>();

    /**
     * 依赖的任务
     */
    private HashMap<String, Worker<T, V>> dependWorkers = new HashMap<>();

    /**
     * 直接依赖的任务
     */
    private HashMap<String, Worker<T, V>> directDependWorkers = new HashMap<>();

    private AtomicReference<WorkerState> workerState = new AtomicReference<Worker.WorkerState>(WorkerState.INIT);

    /**
     * 任务执行状态
     * 
     * @author mengaijun
     * @Description: TODO
     * @date: 2020年3月5日 上午11:24:55
     */
    private enum WorkerState {
        SUCCESS, ERROR, WORKING, INIT;
    }

    /**
     * 启动任务
     * 
     * @param poolExecutor
     * @param dependWorker
     * @date: 2020年5月20日 下午4:24:41
     */
    public void work(ThreadPoolExecutor poolExecutor, Worker dependWorker) {
        // 任务已经被处理
        if (workerState.get() != WorkerState.INIT) {
            return;
        }

        // 没有直接依赖任务，可以直接启动
        if (directDependWorkers.isEmpty()) {
            // 第一批次的任务，肯定能启动成功
            fireCurrentWorker();
            fireNextWorkers(poolExecutor);
        }

        // 如果依赖任务执行失败，直接启动当前任务
        // 可能多个依赖的任务同时触发激活，只有一个能激活成功，比如两个依赖任务同时执行失败，到此步，激活当前任务
        if (dependWorker.getWorkerState().get() == WorkerState.ERROR) {
            if (fireCurrentWorker()) {
                fireNextWorkers(poolExecutor);
            }
        }
        

        // 依赖的任务执行成功
        // 有依赖任务，判断直接依赖任务是否执行完
        if (isDirectDependWorkersFinish()) {
            // 激活当前任务，只可能是所有依赖任务都成功了，才能到这
            if (fireCurrentWorker()) {
                fireNextWorkers(poolExecutor);
            }
        }

    }

    /**
     * 依赖任务是否都完成
     * 
     * @return boolean
     * @date: 2020年5月20日 下午5:25:02
     */
    private boolean isDirectDependWorkersFinish() {
        for (Entry<String, Worker<T, V>> entry : directDependWorkers.entrySet()) {
            Worker<T, V> worker = entry.getValue();
            // 有任务失败，整体任务失败; 可以激活当前任务了
            if (worker.getWorkerState().get() == WorkerState.ERROR) {
                TaskStatusCache.updateTaskStatus(jobName, false);
                return true;
            }
            // 依赖的任务没有都完成
            if (worker.getWorkerState().get() == WorkerState.INIT
                    || worker.getWorkerState().get() == WorkerState.WORKING) {
                return false;
            }
        }

        // 都执行成功了
        return true;
    }

    /**
     * 激活当前任务
     * 
     * @return boolean
     * @date: 2020年5月20日 下午4:38:29
     */
    private boolean fireCurrentWorker() {
        // 任务不是初始化状态, 表示已经被启动
        // 如果任务结果不是默认状态, 表示已经执行完成
        if (workerState.get() != WorkerState.INIT) {
            return false;
        }

        if (workerState.compareAndSet(WorkerState.INIT, WorkerState.WORKING)) {
            // 整体任务有失败的步骤，设置本任务执行失败
            if (TaskStatusCache.getStatus(jobName)) {
                workerState.set(WorkerState.ERROR);
                failResult(new DependErrorException("依赖的任务执行失败！"));
                return true;
            }

            // 之前任务都执行成功，启动本任务
            try {
                // 执行任务
                V result = realWorker.action(param);
                // 设置任务执行结果
                successResult(result);
                // 设置任务执行状态为成功
                workerState.set(WorkerState.SUCCESS);
            } catch (Exception e) {
                workerState.set(WorkerState.ERROR);
                TaskStatusCache.updateTaskStatus(jobName, false);
            }

            return true;
        }
        return false;
    }

    /**
     * 激活next任务
     * 
     * @param poolExecutor
     * @date: 2020年5月20日 下午5:01:05
     */
    private void fireNextWorkers(ThreadPoolExecutor poolExecutor) {
        // 如果下个任务只有一个，在当前线程启动
        if(nextWorkers.size() == 1) {
            nextWorkers.forEach(worker -> {
                worker.work(poolExecutor, this);
            });
        } else {
            // 每个任务提交到线程池中，并发执行
            nextWorkers.forEach(worker -> {
                poolExecutor.execute(() -> {
                    worker.work(poolExecutor, this);
                });
            });
        }
    }

    /**
     * 执行成功结果
     * 
     * @param result
     * @return WorkResult<V>
     * @date: 2020年3月6日 下午2:38:20
     */
    private WorkerResult<V> successResult(V result) {
        workerResult.setResultState(ResultState.SUCCESS);
        workerResult.setResult(result);
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
        workerResult.setResultState(ResultState.EXCEPTION);
        workerResult.setEx(e);
        workerResult.setResult(null);
        return workerResult;
    }

    /**
     * Builder to build {@link WorkerWrapper}.
     */
    @Generated("SparkTools")
    public static final class Builder<T, V> {
        private T param;
        private IWorker<T, V> realWorker;
        private String name;
        private ICallback<T, V> callback;
        private HashMap<String, Worker<T, V>> dependWorkerSet = new HashMap();
        private HashMap<String, Worker<T, V>> directDependWorkerSet = new HashMap();

        private Worker<T, V> worker = new Worker();

        private Builder() {
        }

        public Builder withParam(T param) {
            this.param = param;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withWorker(IWorker<T, V> worker) {
            this.realWorker = worker;
            return this;
        }

        public Builder withCallback(ICallback<T, V> callback) {
            this.callback = callback;
            return this;
        }

        /**
         * 设置任务的依赖任务，必须依赖任务执行完成，当前任务才能执行
         * 
         * @param dependWorkersParam
         *            下个任务
         * @return Builder
         * @date: 2020年3月6日 下午5:23:17
         */
        public Builder withDependWorkers(Worker<T, V>... dependWorkers) {
            if (dependWorkers == null) {
                return this;
            }

            for (Worker<T, V> dependWorker : dependWorkers) {
                withDependWorker(dependWorker);
            }
            return this;
        }

        /**
         * 设置直接依赖任务，依赖任务执行完，才能执行当前任务
         * 
         * @param dependWorker
         *            下一个任务
         * @return Builder
         * @date: 2020年3月6日 下午5:23:52
         */
        public Builder withDependWorker(Worker<T, V> dependWorker) {
            if (dependWorker == null) {
                return this;
            }

            // 依赖任务为当前任务的直接依赖, 也是当前任务的依赖
            directDependWorkerSet.put(dependWorker.getName(), dependWorker);
            dependWorkerSet.put(dependWorker.getName(), dependWorker);

            // 依赖任务的依赖也是当前任务的依赖
            dependWorkerSet.putAll(dependWorker.getDependWorkers());
            
            // 当前任务为依赖任务的下一个任务
            dependWorker.getNextWorkers().add(worker);

            return this;
        }

        /**
         * 创建任务对象
         * 
         * @return Worker
         * @date: 2020年3月6日 下午4:11:55
         */
        public Worker build() {
            worker.setParam(param);
            worker.setRealWorker(realWorker);
            worker.setCallback(callback);
            worker.setDependWorkers(dependWorkerSet);
            worker.setDirectDependWorkers(directDependWorkerSet);
            // 如果没有设置任务名，随机生成一个任务名
            worker.setName(name);
            if (StringUtil.isEmpty(name)) {
                worker.setName(UUID.randomUUID().toString());
            }
            return worker;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public T getParam() {
        return param;
    }

    public void setParam(T param) {
        this.param = param;
    }

    public IWorker<T, V> getRealWorker() {
        return realWorker;
    }

    public void setRealWorker(IWorker<T, V> realWorker) {
        this.realWorker = realWorker;
    }

    public WorkerResult<V> getWorkerResult() {
        return workerResult;
    }

    public void setWorkerResult(WorkerResult<V> workerResult) {
        this.workerResult = workerResult;
    }

    public Set<Worker<T, V>> getNextWorkers() {
        return nextWorkers;
    }

    public void setNextWorkers(Set<Worker<T, V>> nextWorkers) {
        this.nextWorkers = nextWorkers;
    }

    public HashMap<String, Worker<T, V>> getDependWorkers() {
        return dependWorkers;
    }

    public void setDependWorkers(HashMap<String, Worker<T, V>> dependWorkers) {
        this.dependWorkers = dependWorkers;
    }

    public HashMap<String, Worker<T, V>> getDirectDependWorkers() {
        return directDependWorkers;
    }

    public void setDirectDependWorkers(HashMap<String, Worker<T, V>> directDependWorkers) {
        this.directDependWorkers = directDependWorkers;
    }

    public ICallback<T, V> getCallback() {
        return callback;
    }

    public void setCallback(ICallback<T, V> callback) {
        this.callback = callback;
    }

    public AtomicReference<WorkerState> getWorkerState() {
        return workerState;
    }

    public void setWorkerState(AtomicReference<WorkerState> workerState) {
        this.workerState = workerState;
    }

    @Override
    public V action(T object) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public V defaultValue() {
        // TODO Auto-generated method stub
        return null;
    }

}
