package com.liangheee.thread;

import lombok.extern.slf4j.Slf4j;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

@Slf4j(topic = "c.Test02")
public class Test02 {
    public static void main(String[] args) {
        for(int i = 0;i < 3;i++){
            People people = new People();
            people.start();
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        MailBoxes.getIds().forEach(id -> {
            Postman postman = new Postman();
            postman.start(id);
        });
    }
}

@Slf4j(topic = "c.People")
class People extends Thread {
    @Override
    public void run() {
        GuardedObject guardedObject = MailBoxes.createGuardedObject();
        log.debug("等待id = {}信封中～",guardedObject.getId());
        Object response = guardedObject.get(2000);
        log.debug("收到id = {}信封～消息是：{}",guardedObject.getId(),response);
    }
}

@Slf4j(topic = "c.Postman")
class Postman extends Thread {

    private int id;

    public void start(int id){
        this.id = id;
        start();
    }

    @Override
    public void run() {
        GuardedObject guardedObject = MailBoxes.getGuardedObject(id);
        guardedObject.set("消息" + id);
        log.debug("发送消息{}",id);
    }
}

class MailBoxes {
    private static Map<Integer,GuardedObject> boxes = new Hashtable<>();

    public static GuardedObject createGuardedObject(){
        GuardedObject guardedObject = new GuardedObject(IdUtil.generateId());
        boxes.put(guardedObject.getId(),guardedObject);
        return guardedObject;
    }

    public static GuardedObject getGuardedObject(int id){
        return boxes.remove(id);
    }

    public static Set<Integer> getIds(){
        return boxes.keySet();
    }

}

class IdUtil {
    private static int id;
    public synchronized static int generateId(){
        return ++id;
    }
}

@Slf4j(topic = "c.GuardedObject")
class GuardedObject {
    private int id;

    private Object response;

    public GuardedObject(int id){
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public Object get(long timeout){
        synchronized (this){
            long begin = System.currentTimeMillis();
            long passed = 0L;

            while (response == null) {
                long timeWait = timeout - passed;
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
            this.notifyAll();
        }
    }

}
