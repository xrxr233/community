package com.nowcoder.community.util;

/**
 * 生成Redis的key
 * */
public class RedisKeyUtil {
    private static final String SPLIT = ":";
    private static final String PREFIX_ENTITY_LIKE = "like:entity";
    private static final String PREFIX_USER_LIKE = "like:user";

    /* 获取某个实体（帖子、评论）的赞的key
    *  key: like:entity:entityType:entityId
    *  value: set(userId)，可以哪个用户给该实体点了赞
    *  */
    public static String getEntityLikeKey(int entityType, int entityId) {
        return PREFIX_ENTITY_LIKE + SPLIT + entityType + SPLIT + entityId;
    }

    /* 获取某个用户收到的赞的key
    *  key: like:user:userId
    *  value: int
    *  */
    public static String getUserLikeKey(int userId) {
        return PREFIX_USER_LIKE + SPLIT + userId;
    }
}
