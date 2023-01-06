package com.nowcoder.community.service;

import com.nowcoder.community.entity.UserActivity;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 网站数据统计
 * UV(Unique Visitor)：独立访客，根据IP判断（可以不登录），通过Redis的HyperLogLog统计（近似）
 * DAU(Daily Active User)：日活跃用户（必须登录），通过Redis的bitmap统计（精确）
 *
 * 例子：百度的UV较高（因为很多人使用百度搜索），DAU较低（因为很多人不登录）
 * */
@Service
public class DataService implements CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(DataService.class);

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserActivityService userActivityService;

    private SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");

    private UserActivity latestRecord;

    /* 将指定的IP计入UV */
    public void recordUV(String ip) {
        String redisKey = RedisKeyUtil.getUVKey(df.format(new Date()));

        redisTemplate.opsForHyperLogLog().add(redisKey, ip);
    }

    /* 统计指定日期范围内的UV */
    public long calculateUV(Date start, Date end) {
        if(start == null || end == null) {
            throw new IllegalArgumentException("参数不能为空！");
        }

        //整理日期范围内的key
        List<String> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while(!calendar.getTime().after(end)) {
            String key = RedisKeyUtil.getUVKey(df.format(calendar.getTime()));
            keyList.add(key);
            calendar.add(Calendar.DATE, 1);
        }

        //合并数据
        String redisKey = RedisKeyUtil.getUVKey(df.format(start), df.format(end));
        redisTemplate.opsForHyperLogLog().union(redisKey, keyList.toArray());

        //返回结果
        return redisTemplate.opsForHyperLogLog().size(redisKey);
    }

    /* 将指定用户计入DAU */
    public void recordDAU(int userId) {
        String redisKey = RedisKeyUtil.getDAUKey(df.format(new Date()));
        redisTemplate.opsForValue().setBit(redisKey, userId, true);
    }

    /* 统计指定日期范围内的DAU */
    public long calculateDAU(Date start, Date end) {
        if(start == null || end == null) {
            throw new IllegalArgumentException("参数不能为空！");
        }

        //整理日期范围内的key
        List<byte[]> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while(!calendar.getTime().after(end)) {
            String key = RedisKeyUtil.getDAUKey(df.format(calendar.getTime()));
            keyList.add(key.getBytes());
            calendar.add(Calendar.DATE, 1);
        }

        //进行OR运算
        return (long) redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                String redisKey = RedisKeyUtil.getDAUKey(df.format(start), df.format(end));
                connection.bitOp(RedisStringCommands.BitOperation.OR,
                        redisKey.getBytes(), keyList.toArray(new byte[0][0]));
                return connection.bitCount(redisKey.getBytes());
            }
        });
    }

    /* 判断用户是否为活跃用户 */
    public boolean isActiveUser(int userId) {
        String redisKey = RedisKeyUtil.getCurrentUserActivityKey();

        //判断key是否过期
        if(!redisTemplate.hasKey(redisKey)) {
            //从MySQL中获得key
            try {
                latestRecord = getUserActivityRecordFromDB();  //lastRecord是该类的成员变量，保存最新的用户活跃度记录
            } catch (UnsupportedEncodingException e) {
                logger.error("获取活跃用户数据失败：" + e.getMessage());
                throw new RuntimeException(e);
            }
        }

        //判断用户是否是本次统计之后，下次统计之前注册的用户，认为是活跃用户（bitmap中不含有此类用户信息）
        //情况1：Redis中还未保存该用户的创建时间
        String createTimeKey = RedisKeyUtil.getUserCreateTimeKey(userId);
        String createTime = (String) redisTemplate.opsForValue().get(createTimeKey);
        try {
            if(StringUtils.isBlank(createTime) || df.parse(createTime).getTime() >= latestRecord.getTo().getTime()) {
                return true;
            }
        } catch (ParseException e) {
            logger.error("获取活跃用户数据失败：" + e.getMessage());
            throw new RuntimeException(e);
        }

        return redisTemplate.opsForValue().getBit(redisKey, userId);
    }

    /* 从数据库中获得最新的活跃用户统计，并存入Redis中 */
    private UserActivity getUserActivityRecordFromDB() throws UnsupportedEncodingException {
        //从MySQL中查询最新的用户活跃度记录
        UserActivity latestRecord = userActivityService.findLatestRecord();
        if(latestRecord == null) {
            throw new IllegalArgumentException("数据库中没有用户活跃度记录！");
        }

        //构造Redis的key
        String redisKey = RedisKeyUtil.getCurrentUserActivityKey();

        //将bitmap还原为byte数组
        byte[] byteArray = latestRecord.getBitmap().getBytes("UTF8");

        //根据byte数组获得每个用户的活跃度
        for(int i = 0; i < byteArray.length; i++) {
            for(int startOffset = i * 8, endOffset = startOffset + 8; startOffset < endOffset; startOffset++) {
                int userId = startOffset;
                int bit = ((byteArray[i] >> (userId % 8)) & 1);
                redisTemplate.opsForValue().setBit(redisKey, userId, bit == 1);
            }
        }

        //设置过期时间
        redisTemplate.expire(redisKey, QUARTZ_USER_ACTIVITY_INTERVAL / 2, TimeUnit.DAYS);

        return latestRecord;
    }

}
