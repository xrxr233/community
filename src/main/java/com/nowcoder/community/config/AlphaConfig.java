package com.nowcoder.community.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;

@Configuration  //配置类，可以在类中的方法将需要的对象装配到容器中
public class AlphaConfig {
    /*
    * 方法返回的对象会被装配到容器中，bean的名字是方法名
    * */
    @Bean
    public SimpleDateFormat simpleDateFormate() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }
}
