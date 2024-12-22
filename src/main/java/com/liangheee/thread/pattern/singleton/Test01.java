package com.liangheee.thread.pattern.singleton;

import java.io.Serializable;

public class Test01 {
    public static void main(String[] args) {

    }

    private static class LazyHolder {
        static final LazyHolder INSTANCE = new LazyHolder();
    }

    public static LazyHolder getLazyHolder() {
        return LazyHolder.INSTANCE;
    }
}

//final class Singleton implements Serializable {
//    private Singleton(){}
//
//    private static final Singleton instance = new Singleton();
//
//    public static Singleton getInstance(){
//        return instance;
//    }
//
//    public Object readResolve(){
//        return instance;
//    }
//}

final class Singleton {
    private Singleton() {}

    private static volatile Singleton instance = new Singleton();

    public static Singleton getInstance() {
        if(instance != null){
            return instance;
        }

        synchronized (Singleton.class) {
            if(instance != null){
                return instance;
            }

            instance = new Singleton();
            return instance;
        }
    }
}
