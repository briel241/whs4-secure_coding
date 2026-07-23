package com.tinyshop;

import com.tinyshop.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class TinyShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(TinyShopApplication.class, args);
    }
}
