package com.guo.demo2;

/**
 * 文件名称： CountOperate
 * 包路径： com.kylin.demo2
 * author：guoxiaoxu
 * 创建时间： 2019/10/22 - 17:29
 */
public class CountOperate extends Thread {
    public CountOperate() {
        System.out.println("CountOperate---begin");
        System.out.println("Thread.currentThread.getName()=" + Thread.currentThread().getName());
        System.out.println("this.name()=" + this.getName());
        System.out.println("Thread.currentThread()==this :"+ (Thread.currentThread() == this));
        System.out.println("CountOperate---end");
    }

    @Override
    public void run() {
        System.out.println("run begin");
        System.out.println("Thread.currentThread().getName()=" + Thread.currentThread().getName());
        System.out.println("this.getName=" + this.getName());
        System.out.println("Thread.currentThread()==this :"+ (Thread.currentThread() == this));
        System.out.println("run---end");
    }
}

class Run {
    public static void main(String[] args) {
        CountOperate countOperate = new CountOperate();
        Thread thread = new Thread(countOperate);
        thread.setName("A");
        thread.start();
//        countOperate.start();
    }
}