package com.liangheee.thread.pattern.twophaseterminated;

import lombok.extern.slf4j.Slf4j;

public class Test02 {
    public static void main(String[] args) {
        Monitor monitor = new Monitor();
        monitor.start();
        monitor.start();
        monitor.start();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        monitor.stop();
    }
}

@Slf4j(topic = "c.Monitor")
class Monitor {
    private Thread monitor;
    private volatile boolean stop;
    private boolean starting; // 确保当前线程只会被启动一次，balking模式

    public void start(){

        // 没有任何控制这里会有线程安全问题
        synchronized (this){
            if(starting){
                return;
            }
            starting = true;
        }

        monitor = new Thread(() -> {
            while(true){
                if(stop){
                    log.debug("监控关闭");
                    break;
                }

                try {
                    Thread.sleep(1000);
                    log.debug("记录监控信息");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "monitor");
        monitor.start();
    }

    public void stop(){
        if(monitor != null && monitor.isAlive()){
            stop = true;
            monitor.interrupt();
        }
    }
}
