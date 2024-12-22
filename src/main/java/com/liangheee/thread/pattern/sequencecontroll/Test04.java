package com.liangheee.thread.pattern.sequencecontroll;

import lombok.extern.slf4j.Slf4j;

/**
 * 同步模式 - 固定顺序运行
 */
@Slf4j(topic = "c.Test04")
public class Test04 {
    private static Object lock = new Object();
    private static boolean flag = true;
    public static void main(String[] args) {
        new Thread(() -> {
            synchronized (lock){
                while(!flag){
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("1");
                flag = false;
                lock.notifyAll();
            }
        },"t1").start();

        new Thread(() -> {
            synchronized (lock){
                while(flag){
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("2");
                flag = true;
                lock.notifyAll();
            }
        },"t2").start();

    }
}
