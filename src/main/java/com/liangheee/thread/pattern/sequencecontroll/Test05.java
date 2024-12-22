package com.liangheee.thread.pattern.sequencecontroll;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 同步模式-固定顺序运行
 */
public class Test05 {
    public static void main(String[] args) {
        ReentrantLock lock = new ReentrantLock();
        Condition t1Condition = lock.newCondition();
        Condition t2Condition = lock.newCondition();
        Thread t1 = new Thread(() -> {
            lock.lock();
            try {
                t1Condition.await();
                System.out.println("1");
                t2Condition.signal();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }, "t1");

        Thread t2 = new Thread(() -> {
            lock.lock();
            try {
                t2Condition.await();
                System.out.println("2");
                t1Condition.signal();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }, "t2");


        t1.start();
        t2.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        lock.lock();
        try {
            System.out.println("开始～");
            t1Condition.signal();
        }finally {
            lock.unlock();
        }
    }
}