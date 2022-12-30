package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class LikeService {
    @Autowired
    private RedisTemplate redisTemplate;

    /* 点赞/取消点赞 */
    public void like(int userId, int entityType, int entityId) {
        //获得key
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);

        //查看是否点过赞
        Boolean isMember = redisTemplate.opsForSet().isMember(entityLikeKey, userId);
        if(isMember) {
            //取消点赞
            redisTemplate.opsForSet().remove(entityLikeKey, userId);
        }else {
            //点赞
            redisTemplate.opsForSet().add(entityLikeKey, userId);
        }
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

}
