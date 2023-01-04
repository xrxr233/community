package com.nowcoder.community.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 访问业务层时记录日志
 * */
//@Component
//@Aspect
public class ServiceLogAspect {
    private static final Logger logger = LoggerFactory.getLogger(ServiceLogAspect.class);

    /* 定义连接点，处理com.nowcoder.community.service下的所有类的所有方法（任意返回值，任意参数） */
    @Pointcut("execution(* com.nowcoder.community.service.*.*(..))")
    public void pointcut() {

    }

    @Before("pointcut()")
    public void before(JoinPoint joinPoint) {
        //日志格式：用户[ip地址]，在[时间]，访问了[com.nowcoder.community.service.xxx()]

        //获得request对象
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if(attributes == null) {
            //比如在EventConsumer中直接调用service层方法，而不是通过Controller层调用，此时没有ServletRequestAttributes对象
            //在这种情况下简单处理，不记录日志直接返回
            return;
        }
        HttpServletRequest request = attributes.getRequest();

        //用户ip地址
        String ip = request.getRemoteHost();

        //访问时间
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        //方法全限定名
        String target = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();

        //记录日志
        logger.info(String.format("用户[%s]，在[%s]，访问了[%s]", ip, now, target));
    }
}
