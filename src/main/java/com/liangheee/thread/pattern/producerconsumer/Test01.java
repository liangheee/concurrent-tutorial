package com.liangheee.thread.pattern.producerconsumer;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;

/**
 * 生产者-消费者设计模式
 */
@Slf4j(topic = "c.Test01")
public class Test01 {
    public static void main(String[] args) {
        MessageQueue messageQueue = new MessageQueue(2);

        for(int i = 0;i < 3;i++){
            int id = i + 1;
            new Thread(() -> {
                messageQueue.put("消息" + id);
            },"product_" + (i + 1)).start();
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        new Thread(() -> {
            Message message;
            while((message = messageQueue.take()) != null){
                log.debug("消费消息id：{},消息内容：{}",message.getId(),message.getContent());
            }
        },"consumer_1").start();
    }
}

@Slf4j(topic = "c.Message")
@Getter
class Message {
    private int id;
    private String content;

    public Message(int id,String content){
        this.id = id;
        this.content = content;
    }

}

class IdUtil {
    private static int id = 0;
    public synchronized static int generateId() {
        return ++id;
    }
}

@Slf4j(topic = "c.MessageQueue")
class MessageQueue {
    private int capacity;

    private LinkedList<Message> queue = new LinkedList<>();

    public MessageQueue(int capacity) {
        this.capacity = capacity;
    }

    public void put(String content){
        synchronized (queue) {
            while(queue.size() >= capacity){
                try {
                    log.debug("消息队列已满，生产者等待~");
                    queue.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            queue.addLast(new Message(IdUtil.generateId(),content));
            queue.notifyAll();
        }
    }

    public Message take(){
        synchronized (queue){
            while(queue.isEmpty()){
                try {
                    log.debug("消息队列为空，消费者等待～");
                    queue.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            Message message = queue.removeFirst();
            queue.notifyAll();
            return message;
        }
    }


}
