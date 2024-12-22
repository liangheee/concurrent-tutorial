package com.liangheee.thread;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Vector;

@Slf4j(topic = "c.ExerciseSell")
public class ExerciseSell {
    public static void main(String[] args) {
        TicketWindow ticketWindow = new TicketWindow(1000);

        List<Thread> threads = new ArrayList<>();
        // vector是线程安全的
        List<Integer> counts = new Vector<>();

        for(int i = 0;i < 2000;i++){
            Thread thread = new Thread(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                // 存在多个线程对共享变量的读写操作，对于sellTicket()方法而言是临界区，满足竞态条件
                int sellCount = ticketWindow.sellTicket(randomAmount());
                counts.add(sellCount);
            });

            threads.add(thread);
            thread.start();
        }

        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        log.debug("剩余票数：{}",ticketWindow.getTicket());
        log.debug("卖出票数：{}",counts.stream().mapToInt(i -> i).sum());
    }

    // random是线程安全的
    private static Random random = new Random();

    private static Integer randomAmount(){
        return new Random().nextInt(5) + 1;
    }
}

class TicketWindow {
    private int count;

    public TicketWindow(int count) {
        this.count = count;
    }

    public int getTicket(){
        return count;
    }

    public synchronized int sellTicket(int ticket){
        if(count >= ticket){
            count -= ticket;
            return ticket;
        }else{
            return 0;
        }
    }

}
