package com.liangheee.thread;

import lombok.Data;
import lombok.ToString;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class Test03 {
    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
        Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        Unsafe unsafe = (Unsafe)theUnsafe.get(null);

        // unsafe对象对底层数据操作都是针对其内存偏移量获取内存空间，在对里面的数据进行修改
        // 因此利用unsafe进行cas操作之前，要先获取Teacher类中属性的偏移地址
        Teacher teacher = new Teacher();
        long idOffset = unsafe.objectFieldOffset(Teacher.class.getDeclaredField("id"));
        long nameOffset = unsafe.objectFieldOffset(Teacher.class.getDeclaredField("name"));

        unsafe.compareAndSwapInt(teacher,idOffset,0,1);
        unsafe.compareAndSwapObject(teacher,nameOffset,null,"张三");

        System.out.println(teacher);
    }
}

@Data
class Teacher {
    private int id;
    private String name;
}
