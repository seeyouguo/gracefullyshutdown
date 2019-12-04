package com.guo.demo.service;


import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

//@Service
public class App {


    private ExecutorService executorService = Executors.newFixedThreadPool(10);

    @PostConstruct
    public void startJob() {
        for (int i = 0; i < 10; i++) {
            if (i == 3) {
                executorService.submit(new Task2(i));
            } else {
                executorService.submit(new Task(i));
            }
        }

    }

    // 使用 kill -9 不会执行
    @PreDestroy
    public void shutdown() {
        // 通知线程关闭, 不 interrupt, 不等待
        try {
            executorService.shutdown();
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.out.println("======= App interrupted...");
            e.printStackTrace();
        }
        // 立即关闭正在执行的线程
        List<Runnable> list = executorService.shutdownNow();
        // 废弃任务列表
        list.forEach(Object::toString);
        System.out.println("+++++++++ App starts to shutdown.");
    }


    static class Task implements Runnable {
        private Integer index;

        Task(Integer index) {
            this.index = index;
        }

        @Override
        public void run() {
            System.out.println("Task " + index + " " + " started");
            try {
                Thread.sleep(5000000);
            } catch (InterruptedException e) {
                System.out.println("Task的线程 " + index + " " + "interrupted");
                e.printStackTrace();
            }


            System.out.println("Task " + index + " " + " finished");
        }
    }

    static class Task2 implements Runnable {
        private Integer index;
        private Boolean flag = true;

        Task2(Integer index) {
            this.index = index;
        }

        @Override
        public void run() {
            System.out.println("Task2 " + index + " " + " started");
            while (flag) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    flag = false;
                    System.out.println("Task2的线程" + index + " " + "interrupted");
                    e.printStackTrace();
                }
                System.out.println("Task2 " + index + " " + " rocking");
            }
            System.out.println("Task2 " + index + " " + " finished");
        }
    }

    static class Task3 implements Runnable {
        private Integer index;

        Task3(Integer index) {
            this.index = index;
        }

        @Override
        public void run() {
            try {
                System.out.println("Java 100");
                while(!Thread.currentThread().isInterrupted())
                    Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


}