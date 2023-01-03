package com.nowcoder.community.config;

import com.nowcoder.community.quartz.AlphaJob;
import com.nowcoder.community.quartz.PostScoreRefreshJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

/**
 * 定时任务配置：初始化到数据库（quartz_*表）。只会在第一次被调用
 * */
@Configuration
public class QuartzConfig {
    /*
    * FactoryBean可简化bean的实例化过程：
    * 1、通过FactoryBean封装了bean的实例化过程
    * 2、将FactoryBean装配到spring容器中
    * 3、将FactoryBean注入给其他的bean，则该bean得到的是该FactoryBean所管理的对象实例
    *
    * 配置定时任务JobDetail
    * */
    //@Bean
    public JobDetailFactoryBean alphaJobDetail() {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(AlphaJob.class);  //任务类（执行类中的execute方法）
        factoryBean.setName("alphaJob");  //任务名
        factoryBean.setGroup("alphaJobGroup");  //任务所属组
        factoryBean.setDurability(true);  //任务是否持久保存
        factoryBean.setRequestsRecovery(true);  //任务出问题后是否可恢复

        return factoryBean;
    }

    /*
    * 配置触发器Trigger（SimpleTriggerFactoryBean或者CronTriggerFactoryBean）
    * 会自动注入名为alphaJobDetail的bean，需要与对应的获取FactoryBean的方法同名
    * */
    //@Bean
    public SimpleTriggerFactoryBean alphaTrigger(JobDetail alphaJobDetail) {
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(alphaJobDetail);
        factoryBean.setName("alphaTrigger");
        factoryBean.setGroup("alphaTriggerGroup");
        factoryBean.setRepeatInterval(3000);  //3s执行一次
        factoryBean.setJobDataMap(new JobDataMap());  //使用JobDataMap存储任务状态

        return factoryBean;
    }

    /* 刷新帖子分数 */
    @Bean
    public JobDetailFactoryBean postScoreRefreshJobDetail() {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(PostScoreRefreshJob.class);  //任务类（执行类中的execute方法）
        factoryBean.setName("postScoreRefreshJob");  //任务名
        factoryBean.setGroup("communityJobGroup");  //任务所属组
        factoryBean.setDurability(true);  //任务是否持久保存
        factoryBean.setRequestsRecovery(true);  //任务出问题后是否可恢复

        return factoryBean;
    }

    @Bean
    public SimpleTriggerFactoryBean postScoreRefreshTrigger(JobDetail postScoreRefreshJobDetail) {
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(postScoreRefreshJobDetail);
        factoryBean.setName("postScoreRefreshTrigger");
        factoryBean.setGroup("communityTriggerGroup");
        factoryBean.setRepeatInterval(1000 * 60 * 5);  //5min执行一次
        factoryBean.setJobDataMap(new JobDataMap());  //使用JobDataMap存储任务状态

        return factoryBean;
    }
}
