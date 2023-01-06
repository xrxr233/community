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
    private static final String PREFIX_KAPTCHA = "kaptcha";
    private static final String PREFIX_TICKET = "ticket";
    private static final String PREFIX_USER = "user";
    private static final String PREFIX_UV = "uv";
    private static final String PREFIX_DAU = "dau";
    private static final String PREFIX_POST = "post";
    private static final String PREFIX_USER_ACTIVITY = "user:activity";
    private static final String PREFIX_USER_CREATE_TIME = "user:createtime";

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

    /* 登录验证码
    *  owner：一个随机字符串，存放在cookie中用于识别需要登录的用户
    *  */
    public static String getKaptchaKey(String owner) {
        return PREFIX_KAPTCHA + SPLIT + owner;
    }

    /* 登录凭证 */
    public static String getTicketKey(String ticket) {
        return PREFIX_TICKET + SPLIT + ticket;
    }

    /* 用户 */
    public static String getUserKey(int userId) {
        return PREFIX_USER + SPLIT + userId;
    }

    /* 单日UV */
    public static String getUVKey(String date) {
        return PREFIX_UV + SPLIT + date;
    }

    /* 区间UV */
    public static String getUVKey(String startDate, String endDate) {
        return PREFIX_UV + SPLIT + startDate + SPLIT + endDate;
    }

    /* 单日活跃用户
    *  key: dau:202315
    *  value: 0010 1100...，其中第3位为1表示2023年1月5日这天id=3的用户登录过
    *  */
    public static String getDAUKey(String date) {
        return PREFIX_DAU + SPLIT + date;
    }

    /* 区间活跃用户
    *  key: dau:20230105:20230112
    *  value: 0110 1101...，此值通过2023年1月5日到2023年1月12日的value进行OR操作得到，1的个数表示这几天内登录过的用户个数
    *  */
    public static String getDAUKey(String startDate, String endDate) {
        return PREFIX_DAU + SPLIT + startDate + SPLIT + endDate;
    }

    /* 帖子分数计算 */
    public static String getPostScoreKey() {
        return PREFIX_POST + SPLIT + "score";
    }

    /* 热门帖子 */
    public static String getHotPostKey(int offset, int limit) {
        return PREFIX_POST + SPLIT + "hot" + SPLIT + offset + SPLIT + limit;
    }

    /* 帖子行数 */
    public static String getPostRowsKey() {
        return PREFIX_POST + SPLIT + "rows";
    }

    /* 用户创建时间
    *  key: user:createtime:5
    *  value: 20230101，表示id=5的用户是在2023年1月1日创建的
    *  */
    public static String getUserCreateTimeKey(int userId) {
        return PREFIX_USER_CREATE_TIME + SPLIT + userId;
    }

    /* 指定日期范围的用户活跃度（用于在Redis中计算，是最新计算的）
    *  key: user:activity:20230101:20230114
    *  value: 0110 1101...，第1位的1表示id=1的用户在这段时间内是活跃的
    *  */
    public static String getUserActivityKey(String start, String end) {
        return PREFIX_USER_ACTIVITY + SPLIT + start + SPLIT + end;
    }

    /*
    *  当前的用户活跃度（用于从Redis中获取，不一定是最新的）
    *  key: user:activity
    *  value: 0110 1101...，第1位的1表示id=1的用户在这段时间内是活跃的
    *  */
    public static String getCurrentUserActivityKey() {
        return PREFIX_USER_ACTIVITY;
    }

}
