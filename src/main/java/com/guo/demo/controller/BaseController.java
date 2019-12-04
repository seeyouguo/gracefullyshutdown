package com.guo.demo.controller;

import org.springframework.beans.BeansException;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BaseController implements ApplicationContextAware {

    private ApplicationContext context;

    @GetMapping("/shutdown")
    public void shutdownContext() {
//        ((ConfigurableApplicationContext) context).close();
        exitApplication((ConfigurableApplicationContext) context);
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        this.context = ctx;

    }

    static void exitApplication(ConfigurableApplicationContext ctx) {
        int exitCode = SpringApplication.exit(ctx, new ExitCodeGenerator() {
            @Override
            public int getExitCode() {
                // 如果异常退出，返回值0会被具体code替代
                return 0;
            }
        });
        System.exit(exitCode);
    }
}