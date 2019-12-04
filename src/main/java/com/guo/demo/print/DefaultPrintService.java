package com.guo.demo.print;

/**
 * 文件名称： DefaultPrintService
 * 包路径： com.kylin.demo.service
 * author：guoxiaoxu
 * 创建时间： 2019/10/10 - 19:02
 */
public class DefaultPrintService implements PrintService {
    @Override
    public void print() {
        System.out.println("default output");
    }
}
