package com.nowcoder.community.service;

import com.nowcoder.community.dao.UserActivityMapper;
import com.nowcoder.community.entity.UserActivity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserActivityService {
    @Autowired
    private UserActivityMapper userActivityMapper;

    public int addUserActivityRecord(UserActivity userActivity) {
        return userActivityMapper.insertUserActivityRecord(userActivity);
    }

    public UserActivity findLatestRecord() {
        return userActivityMapper.selectLatestRecord();
    }
}
