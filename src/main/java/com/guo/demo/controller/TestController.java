package com.guo.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    @GetMapping("/long-process")
    public String pause() throws InterruptedException {
        System.out.println("long-process 开始执行");
        Thread.sleep(20000);
        System.out.println("long-process 执行结束");
        return "Process finished";
    }

}