package com.guo.demo.print;

/**
 * 文件名称： IviPrintService
 * 包路径： com.kylin.demo.service
 * author：guoxiaoxu
 * 创建时间： 2019/10/10 - 19:03
 */
public class IviPrintService implements PrintService {
    @Override
    public void print() {
        System.out.println("ivi output");
    }
}
