package com.nowcoder.community;

import com.nowcoder.community.service.AlphaService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ContextConfiguration;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class ThreadPoolTests {
    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolTests.class);

    @Autowired
    private AlphaService alphaService;

    /* JDK普通线程池 */
    private ExecutorService executorService = Executors.newFixedThreadPool(5);

    /* JDK可执行定时任务的线程池 */
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);

    /* Spring普通线程池 */
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    /* Spring可执行定时任务的线程池 */
    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    private void sleep(long m) {
        try {
            Thread.sleep(m);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /* JDK普通线程池 */
    @Test
    public void testExecutorService() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                logger.debug("Hello ExecutorService");
            }
        };

        for(int i = 0; i < 10; i++) {
            executorService.submit(task);
        }

        sleep(10000);
    }

    /* JDK可执行定时任务的线程池 */
    @Test
    public void testScheduledExecutorService() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                logger.debug("Hello ScheduledExecutorService");
            }
        };

        scheduledExecutorService.scheduleAtFixedRate(task, 10000, 1000, TimeUnit.MILLISECONDS);

        sleep(30000);
    }

    /* Spring普通线程池 */
    @Test
    public void testThreadPoolTaskExecutor() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                logger.debug("Hello ExecutorService");
            }
        };

        for(int i = 0; i < 10; i++) {
            taskExecutor.submit(task);
        }

        sleep(10000);
    }

    /* Spring可执行定时任务的线程池 */
    @Test
    public void testThreadPoolTaskScheduler() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                logger.debug("Hello ScheduledExecutorService");
            }
        };

        Date startTime = new Date(System.currentTimeMillis() + 10000);
        taskScheduler.scheduleAtFixedRate(task, startTime, 1000);

        sleep(30000);
    }

    /* Spring普通线程池的简化使用方式 */
    @Test
    public void testThreadPoolTaskExecutorSimple() {
        for(int i = 0; i < 10; i++) {
            alphaService.execute1();  //以多线程方式调用该方法
        }

        sleep(10000);
    }

    /* Spring定时任务线程池的简化使用方式（任务自动调用） */
    @Test
    public void testThreadPoolTaskSchedulerSimple() {
        sleep(30000);
    }
}