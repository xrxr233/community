package com.nowcoder.community;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class CommunityApplication {
	/* 解决netty启动冲突问题
	*  Redis和elasticsearch底层都基于netty，冲突主要是由于elasticsearch导致的
	*  Netty4Utils.setAvailableProcessors()方法
	*  */
	@PostConstruct
	public void init() {
		System.setProperty("es.set.netty.runtime.available.processors", "false");
	}

	public static void main(String[] args) {
		SpringApplication.run(CommunityApplication.class, args);
	}

}
