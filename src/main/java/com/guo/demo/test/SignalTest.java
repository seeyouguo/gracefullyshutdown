package com.guo.demo.test;

/**
 * 信号量处理测试
 */
public class SignalTest {

    //shut down hook
    public static void main(String[] args) {
        System.out.println("main");
        Thread shutdownHook = new Thread(() -> System.out.println("关闭"));
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        System.out.println("finished");
    }

//    public void test() throws InterruptedException {
//        // 创建一个信号处理器
//        sun.misc.SignalHandler handler = signal -> {
//            // 什么都不做
//            System.out.println("中断退出");
//        };
//// 设置INT信号(Ctrl+C中断执行)交给指定的信号处理器处理，废掉系统自带的功能
//        sun.misc.Signal.handle(new sun.misc.Signal("INT"), handler);
//        Runnable a = () -> {
//            try {
//                Thread.sleep(1000000L);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        };
//        new Thread(a).start();
//        Thread.currentThread().join();
//    }
}


