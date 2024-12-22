package com.liangheee.thread.pattern.sequencecontroll;

import java.util.concurrent.locks.LockSupport;

/**
 * 同步模式 - 固定顺序运行
 */
public class Test06 {
    private static boolean flag = true;
    private static Thread t1;
    private static Thread t2;
    public static void main(String[] args) {
        t1 = new Thread(() -> {
            LockSupport.park();
            System.out.println("1");
            LockSupport.unpark(t2);
        },"t1");

        t2 = new Thread(() -> {
            LockSupport.park();
            System.out.println("2");
            LockSupport.unpark(t1);
        },"t2");

        t1.start();
        t2.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("开始～");
        LockSupport.unpark(t1);
    }
}
