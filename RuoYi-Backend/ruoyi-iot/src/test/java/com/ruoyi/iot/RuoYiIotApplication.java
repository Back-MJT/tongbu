package com.ruoyi.iot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * RuoYi IoT 测试启动类
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class RuoYiIotApplication {
    public static void main(String[] args) {
        SpringApplication.run(RuoYiIotApplication.class, args);
    }
}
