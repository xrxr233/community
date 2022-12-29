package com.nowcoder.community.controller.advice;

import com.nowcoder.community.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 配置类
 * 统一处理所有被@Controller修饰的方法抛出的异常
 * 因为方法调用为：表现层 -> 业务层 -> 数据层，因此所有的异常都会汇聚到表现层（Controller），所以只需要统一处理表现层的异常
 * */
@ControllerAdvice(annotations = Controller.class)
public class ExceptionAdvice {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionAdvice.class);

    /* 可以处理所有异常的方法 */
    @ExceptionHandler({Exception.class})
    public void handleException(Exception e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        //记录日志
        logger.error("服务器发生异常：" + e.getMessage());
        //遍历异常详细信息
        for(StackTraceElement element : e.getStackTrace()) {
            logger.error(element.toString());
        }

        String xRequestedWith = request.getHeader("x-requested-with");
        if("XMLHttpRequest".equals(xRequestedWith)) {
            //处理异步请求时发生异常的情况
            response.setContentType("application/plain;charset=utf-8");
            PrintWriter writer = response.getWriter();
            writer.write(CommunityUtil.getJSONString(1, "服务器异常！"));
        }else {
            //处理普通请求时发生异常的情况
            response.sendRedirect(request.getContextPath() + "/error");
        }
    }
}
