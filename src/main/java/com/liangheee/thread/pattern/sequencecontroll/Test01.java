package com.liangheee.thread.pattern.sequencecontroll;

/**
 * 同步模式-交替运行
 */
public class Test01 {
    public static void main(String[] args) {
        WaitNotify waitNotify = new WaitNotify(5);
        new Thread(() -> {
            waitNotify.print("a",0,1);
        },"t1").start();

        new Thread(() -> {
            waitNotify.print("b",1,2);
        },"t2").start();

        new Thread(() -> {
            waitNotify.print("c",2,0);
        },"t3").start();
    }
}

class WaitNotify {

    private int loopNumber;
    private int flag;

    public WaitNotify(int loopNumber) {
        this.loopNumber = loopNumber;
    }

    public void print(String msg,int current,int next){
        for(int i = 0;i < loopNumber;i++){
            synchronized (this){
                while(flag != current){
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.print(msg);
                this.flag = next;
                this.notifyAll();
            }
        }
    }
}
