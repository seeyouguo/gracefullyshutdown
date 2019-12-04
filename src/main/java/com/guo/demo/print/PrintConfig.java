package com.guo.demo.print;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 文件名称： PrintConfig
 * 包路径： com.kylin.demo.print
 * author：guoxiaoxu
 * 创建时间： 2019/10/10 - 19:06
 */
@Configuration
public class PrintConfig {

    /**
     * spring.profiles.active = ivi
     */
    @Configuration
    @Profile("ivi")
    public static class IviPrintConfiguration {

        @Bean
        public PrintService iviPrintService() {
            return new IviPrintService();
        }

    }

    /**
     * spring.profiles.active = other
     */
    @Configuration
    @Profile({"other"})
    public static class DefaultPrintConfiguration {
        @Bean
        public PrintService defaultEmailService() {
            return new DefaultPrintService();
        }
    }

}
