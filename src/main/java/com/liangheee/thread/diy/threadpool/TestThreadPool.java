package com.liangheee.thread.diy.threadpool;


import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j(topic = "c.TestThreadPool")
public class TestThreadPool {
    public static void main(String[] args) {
        ThreadPool threadPool = new ThreadPool(
                new BlockingQueue<>(20),
                8,
                15,
                5000,
                TimeUnit.MILLISECONDS,
                (queue,task) -> {
                    // 死等
                    // queue.put(task);
                    // 超时等待
                    // queue.offer(task,timeout,timeUnit);
                    // 调用者执行
                    log.debug("拒绝策略执行...");
                    task.run();
                    // 放弃 -》什么也不用做
                    // 调用者抛出异常
                    // throw new RuntimeException("任务队列已满");
                });

        for(int i = 0;i < 50;i++){
            int j = i;
            threadPool.execute(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log.debug("{}",j + 1);
            });
        }
    }
}

@Slf4j(topic = "c.ThreadPool")
final class ThreadPool {
    private final BlockingQueue<Runnable> queue;

    private final Set<Worker> workers;

    private int coreSize;

    private int maxSize;

    private long keepAliveTime;

    private TimeUnit timeUnit;

    private RejectPolicy<Runnable> rejectPolicy;

    private ReentrantLock lock = new ReentrantLock();

    public ThreadPool(BlockingQueue<Runnable> queue,int coreSize,int maxSize,long keepAliveTime,TimeUnit timeUnit,RejectPolicy<Runnable> rejectPolicy) {
        this.queue = queue;
        this.coreSize = coreSize;
        this.maxSize = maxSize;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.rejectPolicy = rejectPolicy;
        this.workers = new HashSet<>();
    }

    // 预热线程，但是线程并没有开始运行
    public void preStartThreadPool(){
        // balking
        if(workers.size() >= coreSize){
            return;
        }

        lock.lock();
        try {
            for(int i = 0;i < coreSize;i++){
                log.debug("预热线程池");
                Worker worker = new Worker(keepAliveTime, timeUnit);
                workers.add(worker);
                worker.start();
            }
        } finally {
            lock.unlock();
        }
    }

    public void execute(Runnable task){
        lock.lock();
        try {
            if(workers.size() < coreSize){
                Worker worker = new Worker(task,keepAliveTime,timeUnit);
                workers.add(worker);
                worker.start();
            } else {
                // 若当前任务还没跑起来
                if(queue.size() + 1 <= queue.getCapacity()){
                    queue.put(task);
                }else{
                    if (workers.size() < maxSize) {
                        // 线程池扩容
                        log.debug("扩容线程池...");
                        int size = workers.size();
                        for(int i = size;i < maxSize;i++){
                            Worker worker = new Worker(keepAliveTime,timeUnit);
                            workers.add(worker);
                            worker.start();
                        }
                    }
                    // 走拒绝策略
                    queue.tryPut(rejectPolicy,task);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void shrinkingWorkers(Worker worker){
        lock.lock();
        try {
            log.debug("线程空闲超时：{}",worker.getName());
            workers.remove(worker);
        } finally {
            lock.unlock();
        }
    }

    private final class Worker extends Thread {

        private Runnable task;

        private final long keepAliveTime;

        private final TimeUnit timeUnit;

        public Worker(long keepAliveTime,TimeUnit timeUnit){
            this.keepAliveTime = keepAliveTime;
            this.timeUnit = timeUnit;
        }

        public Worker(Runnable task,long keepAliveTime,TimeUnit timeUnit){
            this.task = task;
            this.keepAliveTime = keepAliveTime;
            this.timeUnit = timeUnit;
        }

        @Override
        public void run() {
            while(task != null || (task = queue.poll(keepAliveTime,timeUnit)) != null) {
                try {
                    log.debug("正在执行...{}",task);
                    task.run();
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    task = null;
                }
            }

            // 当前线程等待时间超出最大空闲时间，进行缩容
            shrinkingWorkers(this);
        }
    }

}

@FunctionalInterface
interface RejectPolicy<T> {
    void reject(BlockingQueue<T> queue,T task);
}

@Slf4j(topic = "c.BlockingQueue")
class BlockingQueue<T> {
    private int capacity;

    private Deque<T> queue = new ArrayDeque<>();

    private ReentrantLock lock = new ReentrantLock();

    private Condition emptyWaitSet = lock.newCondition();

    private Condition fullWaitSet = lock.newCondition();

    public BlockingQueue(int capacity) {
        this.capacity = capacity;
    }

    public T take() {
        lock.lock();
        try {
            while(queue.isEmpty()){
                try {
                    log.debug("等待获取任务...");
                    emptyWaitSet.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            T task = queue.removeFirst();
            fullWaitSet.signal();
            return task;
        }finally {
            lock.unlock();
        }
    }

    public T poll(long timeout, TimeUnit timeUnit) {
        lock.lock();
        try {
            long nanos = timeUnit.toNanos(timeout);
            while(queue.isEmpty()){
                try {
                    if(nanos <= 0){
                        return null;
                    }
                    log.debug("超时等待获取任务...");
                    nanos = emptyWaitSet.awaitNanos(nanos);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            T task = queue.removeFirst();
            fullWaitSet.signal();
            return task;
        }finally {
            lock.unlock();
        }
    }

    public void put(T task) {
        lock.lock();
        try {
            while(queue.size() == capacity){
                try {
                    log.debug("等待加入任务队列,{}",task);
                    fullWaitSet.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            queue.addLast(task);
            emptyWaitSet.signal();
        }finally {
            lock.unlock();
        }
    }

    public boolean offer(T task,long timeout, TimeUnit timeUnit) {
        lock.lock();
        try {
            long nanos = timeUnit.toNanos(timeout);
            while(queue.size() == capacity){
                try {
                    if(nanos <= 0){
                        return false;
                    }
                    log.debug("超时等待加入任务队列,{}",task);
                    nanos = fullWaitSet.awaitNanos(nanos);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            queue.addLast(task);
            emptyWaitSet.signal();
            return true;
        }finally {
            lock.unlock();
        }
    }

    public boolean tryPut(RejectPolicy<T> rejectPolicy,T task) {
        lock.lock();
        try {
            if(queue.size() == capacity){
                // 队列满，则走拒绝策略
                rejectPolicy.reject(this,task);
                return false;
            }else{
                // 如果有空闲，加入队列
                log.debug("加入任务队列,{}",task);
                queue.addLast(task);
                emptyWaitSet.signal();
                return true;
            }
        } finally {
            lock.unlock();
        }
    }

    public int getCapacity(){
        return capacity;
    }

    public int size(){
        lock.lock();
        try {
            return queue.size();
        }finally {
            lock.unlock();
        }
    }
}
