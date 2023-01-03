package com.nowcoder.community.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring线程池配置类
 * */
@Configuration
@EnableScheduling  //允许定时任务
@EnableAsync
public class ThreadPoolConfig {
}
