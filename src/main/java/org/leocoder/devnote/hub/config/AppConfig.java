package org.leocoder.devnote.hub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-05-15
 * @description : 应用配置类
 */
@Configuration
public class AppConfig {
    
    /**
     * 创建RestTemplate Bean用于HTTP请求
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}