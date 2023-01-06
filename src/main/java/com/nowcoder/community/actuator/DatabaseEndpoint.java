package com.nowcoder.community.actuator;

import com.nowcoder.community.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 自定义监控端点：数据库连接检查
 * */
@Component
@Endpoint(id = "database")  //端点id
public class DatabaseEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseEndpoint.class);

    /* 数据库连接池接口 */
    @Autowired
    private DataSource dataSource;

    /* 检查连接：只能通过GET访问 */
    @ReadOperation
    public String checkConnection() {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            //返回JSON字符串
            return CommunityUtil.getJSONString(0, "获取连接成功！");
        } catch (SQLException e) {
            logger.info("获取连接失败：" + e.getMessage());
            return CommunityUtil.getJSONString(1, "获取连接失败！");
        } finally {
            if(connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    logger.info("关闭连接失败：" + e.getMessage());
                }
            }
        }
    }
}
