package com.guo.demo.test;

import java.util.concurrent.CountDownLatch;

public class DirectMemoryTest {

    //信号量测试
    public static void main(String[] args) throws InterruptedException{
        sun.misc.SignalHandler handler = new sun.misc.SignalHandler() {
            @Override
            public void handle(sun.misc.Signal signal) {
                // 什么都不做
                System.out.println("====信号量处理===");
            }
        };
        sun.misc.Signal.handle(new sun.misc.Signal("ABRT"), handler);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }


//    public static void main(String[] args) throws InterruptedException{
//        //分配512MB直接缓存
//        ByteBuffer bb = ByteBuffer.allocateDirect(1024*1024*512);
//
//        TimeUnit.SECONDS.sleep(10);
//
//        //清除直接缓存
//        ((MappedByteBuffer)bb).clear();
//
//        TimeUnit.SECONDS.sleep(10);
//
//        System.out.println("ok");
//    }

}
