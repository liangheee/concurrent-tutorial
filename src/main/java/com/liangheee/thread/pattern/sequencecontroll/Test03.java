package com.liangheee.thread.pattern.sequencecontroll;

import java.util.concurrent.locks.LockSupport;

/**
 * 同步模式-交替运行
 */
public class Test03 {

    private static Thread t1;
    private static Thread t2;
    private static Thread t3;

    public static void main(String[] args) {
        LockPark lockPark = new LockPark(5);

        t1 = new Thread(() -> {
            lockPark.print("a",t2);
        },"t1");

        t2 = new Thread(() -> {
            lockPark.print("b",t3);
        },"t2");

        t3 = new Thread(() -> {
            lockPark.print("c",t1);
        },"t3");

        t1.start();
        t2.start();
        t3.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        LockSupport.unpark(t1);
    }
}

class LockPark {
    private int loopNumber;

    public LockPark(int loopNumber) {
        this.loopNumber = loopNumber;
    }

    public void print(String msg,Thread next){
        for(int i = 0;i < loopNumber;i++){
            LockSupport.park();
            System.out.print(msg);
            LockSupport.unpark(next);
        }
    }
}
