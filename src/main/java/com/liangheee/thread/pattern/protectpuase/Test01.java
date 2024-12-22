package com.liangheee.thread.pattern.protectpuase;

import lombok.extern.slf4j.Slf4j;

/**
 * wait-notify的最佳实践：等待方配合使用while，唤醒方使用notifyAll
 *
 * 保护性暂停设计模式（同步的设计模式，消息一旦产生会被立刻消费）
 * 用于两个线程点对点通信，也就是说一定是一个消费者线程对应一个生产者线程，区别于生产者-消费者设计模式
 *
 * 该模式相对于join方法的好处：
 * join必须等待另一个线程结束，等待线程才能运行；保护性暂停设计模式中对于生产者线程而言，可以在发送完消息后，继续运行其他代码，消费者线程也不用等待生产者线程运行结束
 *
 * ps：其实join底层的实现方式就是保护性暂停设计模式
 */
@Slf4j(topic = "c.Test02")
public class Test01 {
    public static void main(String[] args) {
        GuardedObject guardedObject = new GuardedObject();
        new Thread(() -> {
            log.debug("准备接受消息～");
            Object response = guardedObject.get(2000);
            log.debug("响应结果: {}",response);
        },"t1").start();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        new Thread(() -> {
            log.debug("响应结果中～");
            guardedObject.set("消息");
        },"t2").start();
    }
}

@Slf4j(topic = "c.GuardedObject")
class GuardedObject {
    private Object response;

    public Object get(long timeout){
        synchronized (this){
            long begin = System.currentTimeMillis();
            long passed = 0L;
            // 用while true来解决虚假唤醒（简单说就是某个线程通过notify唤醒时，是随机唤醒wait线程，就可能导致虚假唤醒的可能）
            while(response == null){
                long timeWait = timeout - passed;
                // 增加超时时间，增强保护性暂停设计模式
                if(timeWait <= 0){
                    log.debug("接受响应超时～");
                    break;
                }
                try {
                    this.wait(timeWait);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                passed = System.currentTimeMillis() - begin;
            }
            return response;
        }
    }

    public void set(Object response){
        synchronized (this){
            this.response = response;
            // 尽量使用notifyAll，主要是为了解决notify带来的唤醒随机性问题，当然还需要在wait方同时搭配while()循环来处理业务逻辑
            this.notifyAll();
        }
    }
}
