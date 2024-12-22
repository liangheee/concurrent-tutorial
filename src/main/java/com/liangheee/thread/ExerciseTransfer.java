package com.liangheee.thread;

import lombok.extern.slf4j.Slf4j;

import java.util.Random;

@Slf4j(topic = "c.ExerciseTransfer")
public class ExerciseTransfer {
    public static void main(String[] args) throws InterruptedException {
        Amount amount1 = new Amount(1000);
        Amount amount2 = new Amount(1000);
        Thread t1 = new Thread(() -> {
            for(int i = 0;i < 1000;i++){
                amount1.transfer(amount2, randomMoney());
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                amount2.transfer(amount1, randomMoney());
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        log.debug("总金额：{}",amount1.getMoney() + amount2.getMoney());
    }

    private static Random random = new Random();
    private static int randomMoney(){
        return random.nextInt(5) + 1;
    }
}

class Amount {
    private int money;

    public Amount(int money) {
        this.money = money;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public void transfer(Amount target,int money){
        synchronized (Amount.class){
            if(this.money >= money){
                this.setMoney(this.money - money);
                target.setMoney(target.getMoney() + money);
            }
        }
    }
}
