package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
public class LikeService {
    @Autowired
    private RedisTemplate redisTemplate;

    /* 点赞/取消点赞，同时更新被点赞实体作者收到的赞数量
    *  事务操作
    *  userId：点赞人的id
    *  entityUserId：被点赞人的id
    *  */
    public void like(int userId, int entityType, int entityId, int entityUserId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                //获得key
                String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
                String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId);

                //查看是否点过赞（查询操作，需要放在事务之外）
                boolean isMember = redisTemplate.opsForSet().isMember(entityLikeKey, userId);

                //开启事务
                operations.multi();

                //修改操作
                if(isMember) {
                    //取消点赞
                    operations.opsForSet().remove(entityLikeKey, userId);
                    operations.opsForValue().decrement(userLikeKey);
                }else {
                    //点赞
                    operations.opsForSet().add(entityLikeKey, userId);
                    operations.opsForValue().increment(userLikeKey);
                }

                //提交事务
                return operations.exec();
            }
        });
    }

    /* 查询某个实体的赞的数量 */
    public long findEntityLikeCount(int entityType, int entityId) {
        //获得key
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);

        //返回数量
        return redisTemplate.opsForSet().size(entityLikeKey);
    }

    /* 查询某用户对某实体的点赞状态 */
    public int findEntityLikeStatus(int userId, int entityType, int entityId) {
        //获得key
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);

        //返回整数，方便“点踩”的业务扩展
        return redisTemplate.opsForSet().isMember(entityLikeKey, userId) ? 1 : 0;
    }

    /* 查询某个用户获得的赞的数量 */
    public int findUserLikeCount(int userId) {
        //获得key
        String userLikeKey = RedisKeyUtil.getUserLikeKey(userId);

        //返回数量
        Integer count = (Integer) redisTemplate.opsForValue().get(userLikeKey);
        return count == null ? 0 : count.intValue();
    }

}
