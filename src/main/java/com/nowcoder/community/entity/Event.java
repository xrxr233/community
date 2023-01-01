package com.nowcoder.community.entity;

import java.util.HashMap;
import java.util.Map;

/**
 * kafka消息队列中的事件对象
 * 如点赞、评论、关注等事件
 * */
public class Event {
    /* 事件主题 */
    private String topic;
    /* 事件触发者（点赞的人） */
    private int userId;
    /* 与事件相关的实体（被点赞的帖子） */
    private int entityType;
    private int entityId;
    /* 与事件相关的用户（被点赞的帖子的作者） */
    private int entityUserId;
    /* 其他可能的数据（业务扩展） */
    private Map<String, Object> data = new HashMap<>();

    public String getTopic() {
        return topic;
    }

    public Event setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public int getUserId() {
        return userId;
    }

    public Event setUserId(int userId) {
        this.userId = userId;
        return this;
    }

    public int getEntityType() {
        return entityType;
    }

    public Event setEntityType(int entityType) {
        this.entityType = entityType;
        return this;
    }

    public int getEntityId() {
        return entityId;
    }

    public Event setEntityId(int entityId) {
        this.entityId = entityId;
        return this;
    }

    public int getEntityUserId() {
        return entityUserId;
    }

    public Event setEntityUserId(int entityUserId) {
        this.entityUserId = entityUserId;
        return this;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Event setData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }
}
