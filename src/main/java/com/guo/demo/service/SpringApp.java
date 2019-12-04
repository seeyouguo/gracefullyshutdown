package com.guo.demo.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PostConstruct;

//@Service
public class SpringApp {

    private static final Logger logger = LoggerFactory.getLogger(SpringApp.class);


    @Autowired
    @Qualifier("myThreadTask")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @PostConstruct
    public void startJob() {
        for (int i = 0; i < 10; i++) {
            if (i == 3) {
                threadPoolTaskExecutor.submit(new SpringHandleTask(i));
            } else {
                threadPoolTaskExecutor.submit(new SpringTask(i));
            }
        }

    }

    static class SpringTask implements Runnable {
        private Integer index;

        SpringTask(Integer index) {
            this.index = index;
        }

        @Override
        public void run() {
            logger.info("SpringTask " + index + " " + " started");
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                logger.info("SpringTask " + index + " " + " interrupted");
                e.printStackTrace();
            }
            logger.info("SpringTask " + index + " " + " finished");
        }
    }

    static class SpringHandleTask implements Runnable {
        private Integer index;
        private Boolean flag = true;

        SpringHandleTask(Integer index) {
            this.index = index;
        }

        @Override
        public void run() {
            logger.info("SpringHandleTask " + index + " " + " started");
            while (flag) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.info("SpringHandleTask " + index + " " + " interrupted");
                    flag = false;
                    e.printStackTrace();
                }
                logger.info("SpringHandleTask " + index + " " + " rocking");
            }
            logger.info("SpringHandleTask " + index + " " + " finished");
        }
    }


}