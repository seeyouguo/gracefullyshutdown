package com.guo.demo;

/**
 * 文件名称： DemoModel
 * 包路径： com.kylin.demo
 * author：guoxiaoxu
 * 创建时间： 2019/10/22 - 17:11
 */
public class DemoModel {

    ThreadLocal<String> stringThreadLocal = new ThreadLocal<>();

    String getName() {
        String name = Thread.currentThread().getName();
        return stringThreadLocal.get();
    }

    void setName(String name) {
        stringThreadLocal.set(name);
    }

}
