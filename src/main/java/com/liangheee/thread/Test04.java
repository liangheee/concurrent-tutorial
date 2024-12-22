package com.liangheee.thread;

import lombok.extern.slf4j.Slf4j;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

@Slf4j(topic = "c.Test04")
public class Test04 {
    public static void main(String[] args) {
        MyAtomicInteger atomicInteger = new MyAtomicInteger(10000);
        for(int i = 0;i < 1000;i++){
            Thread t = new Thread(() -> {
                atomicInteger.addAndGet(-10);
            }, "t" + i);
            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        log.debug("当前结果：{}",atomicInteger.get());
    }
}

class MyAtomicInteger {
    private volatile Integer value;
    private long valueOffset;
    private static Unsafe unsafe;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = (Unsafe) theUnsafe.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public MyAtomicInteger(int value) {
        this.value = value;
        try {
            valueOffset = unsafe.objectFieldOffset(MyAtomicInteger.class.getDeclaredField("value"));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public int get(){
        return value;
    }

    public int incrementAndGet(){
        Integer prev = this.value;
        Integer next = prev + 1;
        while(!unsafe.compareAndSwapObject(this,valueOffset,prev,next)){
            prev = this.value;
            next = prev + 1;
        }
        return next;
    }

    public int getAndIncrement(){
        Integer prev = this.value;
        Integer next = prev + 1;
        Integer cur = prev;

        while(!unsafe.compareAndSwapObject(this,valueOffset,prev,next)){
            prev = this.value;
            next = prev + 1;
        }

        return cur;
    }

    public int decrementAndGet(){
        Integer prev = this.value;
        Integer next = prev - 1;
        while(!unsafe.compareAndSwapObject(this,valueOffset,prev,next)){
            prev = this.value;
            next = prev - 1;
        }
        return next;
    }

    public int getAndDecrement(){
        Integer prev = this.value;
        Integer next = prev - 1;
        Integer cur = prev;

        while(!unsafe.compareAndSwapObject(this,valueOffset,prev,next)){
            prev = this.value;
            next = prev - 1;
        }
        return cur;
    }

    public int addAndGet(int i){
        Integer prev = this.value;
        Integer next = prev + i;
        while(!unsafe.compareAndSwapObject(this,valueOffset,prev,next)){
            prev = this.value;
            next = prev + i;
        }
        return next;
    }

    public int getAndAdd(int i){
        Integer prev = this.value;
        Integer next = prev + i;
        Integer cur = prev;
        while(!unsafe.compareAndSwapObject(this,valueOffset,prev,next)){
            prev = this.value;
            next = prev + i;
        }
        return cur;
    }
}
