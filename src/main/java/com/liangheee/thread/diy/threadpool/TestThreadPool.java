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
        threadPool.shutdown();
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
                Worker worker = new Worker(true,keepAliveTime, timeUnit);
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
                Worker worker = new Worker(true,task,keepAliveTime,timeUnit);
                workers.add(worker);
                worker.start();
            } else {
                // 若当前任务还没跑起来
                if(queue.size() + 1 <= queue.getCapacity()){
                    queue.put(task);
                }else{
                    if (workers.size() < maxSize) {
                        // 线程池扩容
                        log.debug("生成救急线程...");
                        Worker worker = new Worker(false,task,keepAliveTime,timeUnit);
                        workers.add(worker);
                        worker.start();
                    }else{
                        // 救急线程满，走拒绝策略
                        queue.tryPut(rejectPolicy,task);
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void closeEmergencyWorkers(Worker worker){
        lock.lock();
        try {
            if(!worker.isCore){
                log.debug("救急线程超时空闲关闭：{}",worker.getName());
                workers.remove(worker);
            }
        } finally {
            lock.unlock();
        }
    }

    public void shutdown(){
        Thread shutdownThread = new Thread(() -> {
            for(;;){
                if(queue.size() == 0){
                    break;
                }
                // 避免空转
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            lock.lock();
            try {
                log.debug("shutdown...");
                for (Worker worker : workers) {
                    for(;;){
                        if(!worker.runningTask){
                            break;
                        }
                    }
                    worker.shutdown = true;
                    worker.interrupt();
                    if(worker.isCore){
                        log.debug("关闭核心线程：{}",worker.getName());
                    }else{
                        log.debug("关闭救急线程：{}",worker.getName());
                    }
                }
            } finally {
                lock.unlock();
            }
        }, "shutdown-thread");
        shutdownThread.start();
    }

    private final class Worker extends Thread {
        /**
         * true-核心线程
         * false-救急线程
         */
        private final boolean isCore;

        private Runnable task;

        private final long keepAliveTime;

        private final TimeUnit timeUnit;

        /**
         * 是否正在运行任务
         * 一定要加volatile，提升可见性，否则会出现线程安全问题
         * 因为在线程池shutdown过程中，shutdownThread进行for循环会对worker.runningTask做优化缓存，获取的是工作内存中的数据
         */
        private volatile boolean runningTask;

        /**
         * 是否被线程池关闭
         */
        private boolean shutdown;

        private Worker(boolean isCore,long keepAliveTime,TimeUnit timeUnit){
            this.isCore = isCore;
            this.keepAliveTime = keepAliveTime;
            this.timeUnit = timeUnit;
        }

        private Worker(boolean isCore,Runnable task,long keepAliveTime,TimeUnit timeUnit){
            this.isCore = isCore;
            this.task = task;
            this.keepAliveTime = keepAliveTime;
            this.timeUnit = timeUnit;
        }

        @Override
        public void run() {
            while(task != null ||
                    (isCore && (task = queue.take()) != null) ||
                    (!isCore && (task = queue.poll(keepAliveTime,timeUnit)) != null)){
                try {
                    if(isCore){
                        log.debug("核心线程正在执行...{}",task);
                    }else{
                        log.debug("救急线程正在执行...{}",task);
                    }
                    runningTask = true;
                    task.run();
                    runningTask = false;
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    task = null;
                }
            }

            // 救急线程等待时间超出最大空闲时间，关闭急救线程
            if(!isCore && !shutdown){
                closeEmergencyWorkers(this);
            }
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
                    log.debug("线程：{},不再等待任务",Thread.currentThread().getName());
                    return null;
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
                    log.debug("线程：{},不再等待任务",Thread.currentThread().getName());
                    return null;
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
