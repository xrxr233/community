package com.nowcoder.community.quartz;

import com.nowcoder.community.entity.UserActivity;
import com.nowcoder.community.service.UserActivityService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 用户活跃度定时任务（每14天更新一次）
 * 1、从MySQL的user表中查询用户id集合
 * 2、从Redis中查询14天前到今天的所有用户登录情况（14个bitmap）
 * 3、根据id和bitmap统计用户这14天的登录情况
 *      如果用户是这14天才注册的，则默认为活跃用户
 *          用户注册时间从Redis中获取
 * 4、将统计结果以字符串形式写入到MySQL的user_activity表中
 * */
public class UserActivityRefreshJob implements Job, CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(UserActivityRefreshJob.class);

    @Autowired
    private UserService userService;

    @Autowired
    private UserActivityService userActivityService;

    @Autowired
    private RedisTemplate redisTemplate;

    private SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        logger.info("[任务开始] 统计最近的活跃用户！");

        //从MySQL的user表中查询用户id集合
        List<Integer> userIds = userService.findUserIds();
        //key：用户id，value：用户登录天数
        Map<Integer, Integer> userActivity = new HashMap<>(userIds.size());

        //从Redis中查询14天前到今天的所有用户登录情况（14个bitmap）
        Calendar calendar = Calendar.getInstance();
        Date end = new Date();
        calendar.setTime(end);
        int leftDay = QUARTZ_USER_ACTIVITY_INTERVAL;  //剩余需要统计的天数
        while(leftDay > 0) {
            //获得当日的用户登录信息
            String loginKey = RedisKeyUtil.getDAUKey(df.format(calendar.getTime()));
            for(int userId : userIds) {
                userActivity.put(userId, userActivity.getOrDefault(userId, 0) +
                        (redisTemplate.opsForValue().getBit(loginKey, userId) ? 1 : 0));
            }
            //更新日期
            leftDay--;
            calendar.add(Calendar.DATE, -1);
        }

        //将统计结果存入Redis
        calendar.add(Calendar.DATE, 1);
        Date start = calendar.getTime();
        String redisKey = RedisKeyUtil.getUserActivityKey(df.format(start), df.format(end));
        for(int userId : userActivity.keySet()) {
            //获得用户创建时间
            String createTimeKey = RedisKeyUtil.getUserCreateTimeKey(userId);
            String createTime = (String) redisTemplate.opsForValue().get(createTimeKey);
            if(StringUtils.isBlank(createTime)) {
                continue;
            }

            //判断用户是否活跃
            if(isNewUser(start, end, createTime)) {
                //14天内注册的用户，默认为活跃用户
                redisTemplate.opsForValue().setBit(redisKey, userId, true);
            }else {
                //判断用户登录天数是否达到一半
                int activeDays = userActivity.getOrDefault(userId, 0);
                redisTemplate.opsForValue().setBit(redisKey, userId, activeDays >= (QUARTZ_USER_ACTIVITY_INTERVAL / 2));
            }
        }

        //设置过期时间
        redisTemplate.expire(redisKey, QUARTZ_USER_ACTIVITY_INTERVAL, TimeUnit.DAYS);

        logger.info("[任务进行中] 已经统计结果存入到Redis！");

        //将统计结果保存为字符串形式
        Long byteCount = redisTemplate.opsForValue().size(redisKey);  //bitmap的字节数
        byte[] byteArray = new byte[Math.toIntExact(byteCount)];
        for(int i = 0; i < byteArray.length; i++) {
            for(int startOffset = i * 8, endOffset = startOffset + 8; startOffset < endOffset; startOffset++) {
                int userId = startOffset;
                int bit = redisTemplate.opsForValue().getBit(redisKey, userId) ? 1 : 0;
                int mask = (bit) << (userId % 8);
                byteArray[i] = (byte) (byteArray[i] | mask);
            }
        }

        try {
            String bitmap = new String(byteArray, "UTF-8");
            //存入MySQL的user_activity表中
            UserActivity record = new UserActivity();
            record.setFrom(start);
            record.setTo(end);
            record.setBitmap(bitmap);
            record.setCreateTime(new Date());

            userActivityService.addUserActivityRecord(record);

            logger.info("[任务结束] 已将统计结果存入到MySQL！");

        } catch (UnsupportedEncodingException e) {
            logger.error("bitmap获取失败，结果并未存入到MySQL：" + e.getMessage());
        }
    }

    /* 根据用户注册日期判断用户是否是近期注册用户 */
    private boolean isNewUser(Date start, Date end, String createTime) {
        Date createDate = null;
        try {
            createDate = df.parse(createTime);
            return start.getTime() <= createDate.getTime() && end.getTime() >= createDate.getTime();
        } catch (ParseException e) {
            logger.debug("日期转换失败：" + e.getMessage());
        }
        return false;
    }
}
