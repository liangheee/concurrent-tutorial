package com.liangheee.thread.pattern.sequencecontroll;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 同步模式-交替运行
 */
public class Test02 {
    public static void main(String[] args) {
        AwaitSignal awaitSignal = new AwaitSignal(5);
        Condition condition1 = awaitSignal.newCondition();
        Condition condition2 = awaitSignal.newCondition();
        Condition condition3 = awaitSignal.newCondition();

        Thread t1 = new Thread(() -> {
            awaitSignal.print("a", condition1, condition2);
        }, "t1");

        Thread t2 = new Thread(() -> {
            awaitSignal.print("b", condition2, condition3);
        }, "t2");

        Thread t3 = new Thread(() -> {
            awaitSignal.print("c", condition3, condition1);
        }, "t3");

        t1.start();
        t2.start();
        t3.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        awaitSignal.lock();
        try {
           condition1.signal();
        } finally {
            awaitSignal.unlock();
        }
    }
}

class AwaitSignal extends ReentrantLock {
    private int loopNumber;

    public AwaitSignal(int loopNumber) {
        this.loopNumber = loopNumber;
    }

    public void print(String msg, Condition current,Condition next) {
        for(int i = 0;i < loopNumber;i++){
            this.lock();
            try {
                current.await();
                System.out.print(msg);
                next.signal();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                this.unlock();
            }
        }
    }
}
