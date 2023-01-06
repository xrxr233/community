package com.nowcoder.community.dao;

import com.nowcoder.community.entity.UserActivity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserActivityMapper {
    /* 插入一条记录 */
    int insertUserActivityRecord(UserActivity userActivity);

    /* 查询最新的用户活跃记录 */
    UserActivity selectLatestRecord();
}
