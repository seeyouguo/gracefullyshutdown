package com.guo.demo.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.CompletableFuture;

/**
 * 文件名称： AsychController
 * 包路径： com.kylin.demo.controller
 * author：guoxiaoxu
 * 创建时间： 2019/10/23 - 10:48
 */
@RestController
public class AsyncController {

    @Autowired
    @Qualifier("myThreadTask")
    private ThreadPoolTaskExecutor executor;

    Log logger = LogFactory.getLog(AsyncController.class);


    @GetMapping("asyncDeferred")
    public DeferredResult<String> deferredResult() {
        logger.error("async start");
        DeferredResult<String> deferredResult = new DeferredResult<>();
        CompletableFuture.supplyAsync(() -> execute())
                .whenCompleteAsync((result, throwable) -> deferredResult.setResult(result));
        logger.error("async end");
        return deferredResult;
    }

    @GetMapping("asyncDeferredWithOwnThread")
    public DeferredResult<String> deferredResultWithOwnThread() {
        logger.error("async with myThreadPool start");
        DeferredResult<String> deferredResult = new DeferredResult<>();
        CompletableFuture.supplyAsync(() -> execute(), executor)
                .whenCompleteAsync((result, throwable) -> deferredResult.setResult(result));
        logger.error("async with myThreadPool end");
        return deferredResult;
    }


    private String execute() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        System.out.println("block execute 执行结束");
        return "success";
    }

}
