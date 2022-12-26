package com.nowcoder.community;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class LoggerTests {
    //每个类一个Logger对象，名字和所在类名对应
    private static final Logger logger = LoggerFactory.getLogger(LoggerTests.class);

    @Test
    public void testLogger() {
        System.out.println(logger.getName());

        //debug级别
        logger.debug("debug log");
        //info级别
        logger.info("info log");
        //warn级别
        logger.warn("warn log");
        //error级别
        logger.error("error log");

    }
}
