package com.nowcoder.community.event;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.*;
import com.nowcoder.community.util.CommunityConstant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/**
 * 事件消费者
 * */
@Component
public class EventConsumer implements CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private FollowService followService;

    @Autowired
    private DataService dataService;

    @Value("${wk.image.command}")
    private String wkImageCommand;

    @Value("${wk.image.store}")
    private String wkImageStorage;

    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_LIKE, TOPIC_FOLLOW})
    public void handleCommentMessage(ConsumerRecord record) {
        if(record == null || record.value() == null) {
            logger.error("消息的内容为空！");
            return;
        }

        //获取消息中的Event对象
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if(event == null) {
            logger.error("消息格式错误！");
            return;
        }

        //构造Message对象并存入MySQL，发送站内通知（与消息相关的用户在访问消息时可以获得最新的消息）
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setStatus(0);
        message.setCreateTime(new Date());

        //message的content（JSON字符串）
        Map<String, Object> content = new HashMap<>();
        content.put("userId", event.getUserId());
        content.put("entityType", event.getEntityType());
        content.put("entityId", event.getEntityId());
        if(!event.getData().isEmpty()) {
            for(Map.Entry<String, Object> entry : event.getData().entrySet()) {
                content.put(entry.getKey(), entry.getValue());
            }
        }
        message.setContent(JSONObject.toJSONString(content));

        messageService.addMessage(message);
    }

    /* 消费发帖事件 */
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord record) {
        if(record == null || record.value() == null) {
            logger.error("消息的内容为空！");
            return;
        }

        //获取消息中的Event对象
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if(event == null) {
            logger.error("消息格式错误！");
            return;
        }

        //查询帖子
        DiscussPost discussPost = discussPostService.findDiscussPostById(event.getEntityId());

        //存入elasticsearch
        elasticsearchService.saveDiscussPost(discussPost);

        //推送给该用户的粉丝
        this.pushMessage(event, discussPost);
    }

    /* 服务器推送消息模式 */
    private void pushMessage(Event event, DiscussPost discussPost) {
        //查询用户的所有粉丝数
        long followerCount = followService.findFollowerCount(CommunityConstant.ENTITY_TYPE_USER, event.getUserId());

        //分批次查询所有粉丝，并推送消息
        int offset = 0;
        int limit = 2000;
        long numOfPeopleReceivedMessage = 0;  //已经收到消息的用户数
        while(numOfPeopleReceivedMessage <= followerCount) {
            List<Map<String, Object>> followers = followService.findFollowers(event.getUserId(), offset, limit);
            for(Map<String, Object> map : followers) {
                User follower = (User) map.get("user");

                //判断用户是否为活跃用户（暂不启用）
//                if(!dataService.isActiveUser(follower.getId())) {
//                    continue;
//                }

                //构造通知消息对象
                Message message = new Message();
                message.setFromId(SYSTEM_USER_ID);
                message.setToId(follower.getId());
                message.setConversationId(event.getTopic());
                message.setStatus(0);
                message.setCreateTime(new Date());

                //message的Content（JSON字符串）
                Map<String, Object> content = new HashMap<>();
                content.put("userId", event.getUserId());
                content.put("entityType", event.getEntityType());
                content.put("entityId", event.getEntityId());
                content.put("postId", discussPost.getId());
                content.put("postTitle", discussPost.getTitle());
                if(!event.getData().isEmpty()) {
                    for(Map.Entry<String, Object> entry : event.getData().entrySet()) {
                        content.put(entry.getKey(), entry.getValue());
                    }
                }
                message.setContent(JSONObject.toJSONString(content));

                //存入消息
                messageService.addMessage(message);
            }

            offset += limit;
            numOfPeopleReceivedMessage += limit;
        }
    }

    /* 消费删帖事件 */
    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord record) {
        if(record == null || record.value() == null) {
            logger.error("消息的内容为空！");
            return;
        }

        //获取消息中的Event对象
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if(event == null) {
            logger.error("消息格式错误！");
            return;
        }

        //从elasticsearch中删除
        elasticsearchService.deleteDiscussPost(event.getEntityId());
    }

    /* 消费分享事件（截长图） */
    @KafkaListener(topics = {TOPIC_SHARE})
    public void handleShareMessage(ConsumerRecord record) {
        if(record == null || record.value() == null) {
            logger.error("消息的内容为空！");
            return;
        }

        //获取消息中的Event对象
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if(event == null) {
            logger.error("消息格式错误！");
            return;
        }

        //生成长图
        String htmlUrl = (String) event.getData().get("htmlUrl");
        String fileName = (String) event.getData().get("fileName");
        String suffix = (String) event.getData().get("suffix");

        //cmd: E:/develop/wkhtmltopdf/bin/wkhtmltoimage --quality 75 www.baidu.com  E:/java_project/community/data/html_image/文件名
        String cmd = wkImageCommand + " --quality 75 "
                + htmlUrl + " " + wkImageStorage + "/" + fileName + suffix;
        try {
            Runtime.getRuntime().exec(cmd);
            logger.info("生成长图成功：" + cmd);
        } catch (IOException e) {
            logger.error("生成长图失败：" + e.getMessage());
        }
    }
}
