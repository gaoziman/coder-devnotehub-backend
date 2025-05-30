package org.leocoder.devnote.hub;

import cn.hutool.core.util.URLUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.github.xiaoymin.knife4j.spring.configuration.Knife4jProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-30 22:08
 * @description : DevNoteHub 启动类
 */
@Slf4j
@RequiredArgsConstructor
@SpringBootApplication
@MapperScan("org.leocoder.devnote.hub.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableScheduling // 开启定时任务支持
@EnableAsync // 开启异步支持
public class DevNoteHubApplication  implements ApplicationRunner {
    private final ServerProperties serverProperties;

    public static void main(String[] args) {
        SpringApplication.run(DevNoteHubApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) {
        String hostAddress = "localhost";
        Integer port = serverProperties.getPort();
        String contextPath = serverProperties.getServlet().getContextPath();

        // 如果 contextPath 为空或没有以 "/" 开头，添加 "/"
        if (contextPath == null || !contextPath.startsWith("/")) {
            contextPath = "/";
        }

        // 生成 baseUrl，并确保 contextPath 的格式正确
        String baseUrl = URLUtil.normalize("http://%s:%s%s".formatted(hostAddress, port, contextPath));

        // 确保 baseUrl 以 "/" 结尾
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        log.info("----------------------------------------------");
        log.info("API 地址：{}", baseUrl);

        Knife4jProperties knife4jProperties = SpringUtil.getBean(Knife4jProperties.class);
        if (!knife4jProperties.isProduction()) {
            log.info("API 文档：{}doc.html", baseUrl);
        }
        log.info("----------------------------------------------");
    }
}
