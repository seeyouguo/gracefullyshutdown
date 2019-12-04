package com.guo.demo;

import org.junit.Test;

/**
 * 文件名称： com.kylin.demo.ThreadLocalTest
 * 包路径： PACKAGE_NAME
 * author：guoxiaoxu
 * 创建时间： 2019/10/22 - 15:24
 */
public class ThreadLocalTest {

    @Test
    public void test01(){
        DemoModel demo = new DemoModel();
        demo.setName("main thread no name");
        new Thread(()->{
            demo.setName("thread1 no name");
            System.out.println("thread1 ---> " + demo.getName());
        }).start();

        new Thread(()->{
            demo.setName("thread2 no name");
            System.out.println("thread2 ---> " + demo.getName());
        }).start();

        System.out.println("main ---> " + demo.getName());
    }


}

