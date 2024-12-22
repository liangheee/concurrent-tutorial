package com.liangheee.thread;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

@Slf4j(topic = "c.Test01")
public class Test01 {
    public static void main(String[] args) {
        FutureTask<Integer> futureTask = new FutureTask<Integer>(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                log.debug("t1线程执行～");
                return 10;
            }
        });
        Thread thread = new Thread(futureTask,"t1");
        thread.start();
    }
}
