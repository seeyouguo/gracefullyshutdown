package com.guo.demo.test;

/**
 * CPU问题：继续使用JDK 7 和 JDK 12 分别测试CPU个数情况。
 */
public class CpuTest {

    public static void main(String[] args) {

        Runtime runtime = Runtime.getRuntime();

        int processors = runtime.availableProcessors();
        long maxMemory = runtime.maxMemory();

        System.out.format("Number of processors: %d\n", processors);
        System.out.format("Max memory: %d bytes\n", maxMemory);
    }
}