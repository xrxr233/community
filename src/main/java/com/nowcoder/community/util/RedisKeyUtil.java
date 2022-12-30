package com.nowcoder.community.util;

/**
 * 生成Redis的key
 * */
public class RedisKeyUtil {
    private static final String SPLIT = ":";
    private static final String PREFIX_ENTITY_LIKE = "like:entity";
    private static final String PREFIX_USER_LIKE = "like:user";
    private static final String PREFIX_FOLLOWEE = "followee";
    private static final String PREFIX_FOLLOWER = "follower";

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

    /* 某个用户关注的实体（用户、帖子等）
    *  key: followee:userId:entityType
    *  value: zset(entityId, now)
    *
    *  userId：关注者
    *  entityType：关注的实体类型
    *
    *  entityId：关注的实体id
    *  now：当前时间作为分数（按照关注的时间顺序列举）
    * */
    public static String getFolloweeKey(int userId, int entityType) {
        return PREFIX_FOLLOWEE + SPLIT + userId + SPLIT + entityType;
    }

    /* 某个实体拥有的粉丝
    *  key: follower:entityType:entityId
    *  value: zset(userId, now)
    *
    *  entityType：实体类型
    *  entityId：实体id
    *
    *  userId：关注该实体的用户id
    *  now：关注时间
    *  */
    public static String getFollowerKey(int entityType, int entityId) {
        return PREFIX_FOLLOWER + SPLIT + entityType + SPLIT + entityId;
    }
}
