package com.nowcoder.community.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.util.RedisKeyUtil;
import com.nowcoder.community.util.SensitiveFilter;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostService {
    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${caffeine.posts.max-size}")
    private int maxSize;

    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;

    /* caffeine核心接口：Cache，LoadingCache，AsyncLoadingCache */

    /* 帖子列表的缓存 */
    private LoadingCache<String, List<DiscussPost>> postListCache;

    /* 帖子总数的缓存 */
    private LoadingCache<Integer, Integer> postRowsCache;

    /* 初始化缓存：从数据库中查询热门帖子并存入Redis和本地缓存 */
    @PostConstruct
    public void init() {
        //初始化帖子列表本地缓存（key为offset和limit，因为在查询热门帖子时调用findDiscussPosts方法，
        // 该方法的userId为0，orderMode为1，而offset和limit是可变的，使用":"分隔）
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    //缓存中没有数据时的查询策略
                    @Nullable
                    @Override
                    public List<DiscussPost> load(@NonNull String key) throws Exception {
                        if(key == null || key.length() == 0) {
                            throw new IllegalArgumentException("参数错误！");
                        }
                        String[] params = key.split(":");
                        if(params == null || params.length != 2) {
                            throw new IllegalArgumentException("参数错误！");
                        }

                        int offset = Integer.valueOf(params[0]);
                        int limit = Integer.valueOf(params[1]);

                        //从二级缓存中获取数据（Redis）
                        List<DiscussPost> hotDiscussPosts = getHotDiscussPostsFromRedis(offset, limit);
                        if(hotDiscussPosts == null) {
                            hotDiscussPosts = initRedisWithHotDiscussPosts(offset, limit);
                        }

                        return hotDiscussPosts;
                    }
                });

        //初始化帖子总数本地缓存
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Nullable
                    @Override
                    public Integer load(@NonNull Integer key) throws Exception {
                        int rows = getDiscussPostRowsFromRedis();
                        if(rows < 0) {
                            rows = initRedisWithDiscussPostRows();
                        }
                        return rows;
                    }
                });

    }

    /* 初始化Redis（热门帖子） */
    private List<DiscussPost> initRedisWithHotDiscussPosts(int offset, int limit) {
        //热门帖子的Redis key
        String redisKey = RedisKeyUtil.getHotPostKey(offset, limit);

        //从MySQL中获取数据
        logger.debug("从数据库中查询热门帖子！");
        List<DiscussPost> hotDiscussPosts = discussPostMapper.selectDiscussPosts(0, offset, limit, 1);

        //将帖子集合存储到Redis中，并设置过期时间
        redisTemplate.opsForValue().set(redisKey, JSON.toJSON(hotDiscussPosts).toString());
        redisTemplate.expire(redisKey, expireSeconds * 2, TimeUnit.SECONDS);

        return hotDiscussPosts;
    }

    /* 从Redis中获得数据（热门帖子） */
    private List<DiscussPost> getHotDiscussPostsFromRedis(int offset, int limit) {
        //热门帖子的Redis key
        String redisKey = RedisKeyUtil.getHotPostKey(offset, limit);

        //根据key获得帖子集合并返回（可能返回null）
        String listArray = (String) redisTemplate.opsForValue().get(redisKey);
        if(StringUtils.isBlank(listArray)) {
            return null;
        }
        logger.debug("从Redis中获得了热门帖子！");
        return JSON.parseArray(listArray, DiscussPost.class);
    }

    /* 初始化Redis（帖子行数） */
    private int initRedisWithDiscussPostRows() {
        //帖子行数的key
        String redisKey = RedisKeyUtil.getPostRowsKey();

        //从MySQL中获取数据
        logger.debug("从数据库中查询帖子行数！");
        int rows = discussPostMapper.selectDiscussPostRows(0);

        //将行数保存到Redis中，并设置过期时间
        redisTemplate.opsForValue().set(redisKey, rows);
        redisTemplate.expire(redisKey, expireSeconds * 2, TimeUnit.SECONDS);

        return rows;
    }

    /* 从Redis中获得数据（帖子行数） */
    private int getDiscussPostRowsFromRedis() {
        //帖子行数的key
        String redisKey = RedisKeyUtil.getPostRowsKey();

        //获得帖子行数（可能为null）
        Integer rows =  (Integer) redisTemplate.opsForValue().get(redisKey);
        if(rows == null) {
            return -1;
        }
        logger.debug("从Redis中获得了帖子行数！");
        return rows;
    }

    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode) {
        if(userId == 0 && orderMode == 1) {
            //查询热门帖子
            return postListCache.get(offset + ":" + limit);
        }

        logger.debug("从数据库中查询帖子！");
        return discussPostMapper.selectDiscussPosts(userId, offset, limit, orderMode);
    }

    public int findDiscussPostRows(int userId) {
        if(userId == 0) {
            return postRowsCache.get(userId);
        }

        logger.debug("从数据库中查询帖子总行数！");
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    /* 添加帖子 */
    public int addDiscussPost(DiscussPost discussPost) {
        if(discussPost == null) {
            throw new IllegalArgumentException("参数不能为空！");
        }

        //过滤HTML标签
        discussPost.setTitle(HtmlUtils.htmlEscape(discussPost.getTitle()));
        discussPost.setContent(HtmlUtils.htmlEscape(discussPost.getContent()));

        //过滤敏感词
        discussPost.setTitle(sensitiveFilter.filter(discussPost.getTitle()));
        discussPost.setContent(sensitiveFilter.filter(discussPost.getContent()));

        //添加帖子
        return discussPostMapper.insertDiscussPost(discussPost);
    }

    /* 根据id查询帖子 */
    public DiscussPost findDiscussPostById(int id) {
        return discussPostMapper.selectDiscussPostById(id);
    }

    /* 更新帖子回复数量 */
    public int updateCommentCount(int id, int commentCount) {
        return discussPostMapper.updateCommentCount(id, commentCount);
    }

    public int updateType(int id, int type) {
        return discussPostMapper.updateType(id, type);
    }

    public int updateStatus(int id, int type) {
        return discussPostMapper.updateStatus(id, type);
    }

    public int updateScore(int id, double score) {
        return discussPostMapper.updateScore(id, score);
    }
}
