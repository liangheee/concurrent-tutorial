package com.liangheee.thread.pattern.twophaseterminated;

import lombok.extern.slf4j.Slf4j;

/**
 * 测试并发设计模式-两阶段终止
 */
@Slf4j(topic = "c.Test01")
public class Test01 {
    public static void main(String[] args) {
        try {
            TwoPhaseTermination.start();
            Thread.sleep(3000);
            log.debug("interrupt");
            TwoPhaseTermination.stop();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

@Slf4j(topic = "c.TwoPhaseTermination")
class TwoPhaseTermination {
    private static Thread monitor;
    public static void start(){
        monitor = new Thread(() -> {
            while(true){
                Thread current = Thread.currentThread();
                if(current.isInterrupted()){
                    log.debug("料理后事");
                    break;
                }
                try {
                    Thread.sleep(1000);
                    // 如果是正常运行状态，被打断后不会出现异常，不会清空打断标记，因此可以不用重置打断标记
                    log.debug("执行监控记录");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    // 如果在睡眠的时候被打断，那么会抛出InterruptedException异常，会清空打断标记
                    // 因此我们需要重置打断标记
                    log.debug("重置打断标记");
                    current.interrupt();
                }
            }
        },"monitor");
        monitor.start();
    }

    public static void stop(){
        if(monitor != null && monitor.isAlive()){
            monitor.interrupt();
        }
    }
}
