package com.guo.demo.test;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试Docker和Java版本
 */
public class MemEat {
    public static void main(String[] args) {
        List l = new ArrayList<>();
        while (true) {
            byte b[] = new byte[1048576];
            l.add(b);
            Runtime rt = Runtime.getRuntime();
            System.out.println("free memory: " + rt.freeMemory() / (1024 * 1024) + " mb");
        }
    }
}