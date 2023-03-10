package com.nowcoder.community.controller;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@Controller
public class MessageController implements CommunityConstant {
    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    /* 私信列表 */
    @LoginRequired
    @RequestMapping(path = "/letter/list", method = RequestMethod.GET)
    public String getLetterList(Model model, Page page) {
        User user = hostHolder.getUser();

        //设置分页信息
        page.setLimit(5);
        page.setPath("/letter/list");
        page.setRows(messageService.findConversationCount(user.getId()));

        //查询会话列表
        List<Message> conversationList = messageService.findConversations(user.getId(), page.getOffset(), page.getLimit());

        List<Map<String, Object>> conversations = new ArrayList<>();
        if(conversationList != null) {
            for(Message message : conversationList) {
                Map<String, Object> map = new HashMap<>();
                map.put("conversation", message);
                map.put("letterCount", messageService.findLetterCount(message.getConversationId()));
                map.put("unreadCount", messageService.findLetterUnreadCount(user.getId(), message.getConversationId()));

                //与当前用户对话的用户的信息
                int targetId = user.getId() == message.getFromId() ? message.getToId() : message.getFromId();
                map.put("target", userService.findUserById(targetId));

                conversations.add(map);
            }
        }
        model.addAttribute("conversations", conversations);

        //查询总未读消息数量
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount", letterUnreadCount);
        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);

        return "/site/letter";
    }

    /* 私信详情页面 */
    @LoginRequired
    @RequestMapping(path = "/letter/detail/{conversationId}", method = RequestMethod.GET)
    public String getLetterDetail(@PathVariable("conversationId") String conversationId, Page page, Model model) {
        //设置分页信息
        page.setLimit(5);
        page.setPath("/letter/detail/" + conversationId);
        page.setRows(messageService.findLetterCount(conversationId));

        //私信列表
        List<Message> letterList = messageService.findLetters(conversationId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> letters = new ArrayList<>();
        if(letterList != null) {
            for(Message message : letterList) {
                Map<String, Object> map = new HashMap<>();
                map.put("letter", message);
                map.put("fromUser", userService.findUserById(message.getFromId()));  //发送这条消息的用户
                letters.add(map);
            }
        }

        model.addAttribute("letters", letters);

        //获得与当前用户对话的用户信息
        model.addAttribute("target", this.getLetterTarget(conversationId));

        //设置消息已读
        List<Integer> ids = this.getLetterIds(letterList);
        if(!ids.isEmpty()) {
            messageService.readMessage(ids);
        }

        return "/site/letter-detail";
    }

    /* 获取与当前用户对话的用户 */
    private User getLetterTarget(String conversationId) {
        String[] ids = conversationId.split("_");
        int id0 = Integer.parseInt(ids[0]);
        int id1 = Integer.parseInt(ids[1]);

        if(hostHolder.getUser().getId() == id0) {
            return userService.findUserById(id1);
        }else {
            return userService.findUserById(id0);
        }
    }

    /* 设置未读消息为已读 */
    private List<Integer> getLetterIds(List<Message> letterList) {
        List<Integer> ids = new ArrayList<>();

        if(letterList != null) {
            for(Message message : letterList) {
                //当前用户为消息接收者，且消息为未读状态
                if(hostHolder.getUser().getId() == message.getToId() && message.getStatus() == 0) {
                    ids.add(message.getId());
                }
            }
        }

        return ids;
    }

    /* 发送私信：异步 */
    @LoginRequired
    @RequestMapping(path = "/letter/send", method = RequestMethod.POST)
    @ResponseBody
    public String sendLetter(String toName, String content) {
        User target = userService.findUserByName(toName);
        if(target == null) {
            return CommunityUtil.getJSONString(1, "目标用户不存在！");
        }

        //插入消息
        Message message = new Message();
        message.setFromId(hostHolder.getUser().getId());
        message.setToId(target.getId());
        if(message.getFromId() < message.getToId()) {
            message.setConversationId(message.getFromId() + "_" + message.getToId());
        }else {
            message.setConversationId(message.getToId() + "_" + message.getFromId());
        }
        message.setContent(content);
        message.setStatus(0);
        message.setCreateTime(new Date());
        messageService.addMessage(message);

        return CommunityUtil.getJSONString(0);
    }

    /* 删除私信（根据传入的消息id） */
    @LoginRequired
    @RequestMapping(path = "/letter/delete", method = RequestMethod.POST)
    @ResponseBody
    public String deleteLetter(int id) {
        //判断消息是否是当前用户发出的，但当前用户可以删除系统通知
        User user = hostHolder.getUser();
        Message message = messageService.findLetterById(id);
        if(message.getFromId() != user.getId() && message.getFromId() != SYSTEM_USER_ID) {
            return CommunityUtil.getJSONString(1, "只能删除自己发送的消息！");
        }

        //删除消息
        messageService.deleteLetterById(message.getId());

        return CommunityUtil.getJSONString(0);
    }

    /* 系统通知列表
    *  目前的通知类型：点赞、关注、评论
    *  */
    @LoginRequired
    @RequestMapping(path = "/notice/list", method = RequestMethod.GET)
    public String getNoticeList(Model model) {
        User user = hostHolder.getUser();

        //查询评论类通知
        Message message = messageService.findLatestNotice(user.getId(), TOPIC_COMMENT);
        if(message != null) {
            Map<String, Object> messageVo = new HashMap<>();
            messageVo.put("message", message);

            //处理包含已转义字符的JSON字符串
            String content = HtmlUtils.htmlUnescape(message.getContent());  //反转义
            HashMap<String, Object> data = JSONObject.parseObject(content, HashMap.class);

            //保存data中的信息
            messageVo.put("user", userService.findUserById((Integer) data.get("userId")));
            messageVo.put("entityType", data.get("entityType"));
            messageVo.put("entityId", data.get("entityId"));
            messageVo.put("postId", data.get("postId"));

            int count = messageService.findNoticeCount(user.getId(), TOPIC_COMMENT);
            messageVo.put("count", count);

            int unread = messageService.findNoticeUnreadCount(user.getId(), TOPIC_COMMENT);
            messageVo.put("unread", unread);

            model.addAttribute("commentNotice", messageVo);
        }

        //查询点赞类通知
        message = messageService.findLatestNotice(user.getId(), TOPIC_LIKE);
        if(message != null) {
            Map<String, Object> messageVo = new HashMap<>();

            messageVo.put("message", message);

            //处理包含已转义字符的JSON字符串
            String content = HtmlUtils.htmlUnescape(message.getContent());  //反转义
            HashMap<String, Object> data = JSONObject.parseObject(content, HashMap.class);

            //保存data中的信息
            messageVo.put("user", userService.findUserById((Integer) data.get("userId")));
            messageVo.put("entityType", data.get("entityType"));
            messageVo.put("entityId", data.get("entityId"));
            messageVo.put("postId", data.get("postId"));

            int count = messageService.findNoticeCount(user.getId(), TOPIC_LIKE);
            messageVo.put("count", count);

            int unread = messageService.findNoticeUnreadCount(user.getId(), TOPIC_LIKE);
            messageVo.put("unread", unread);

            model.addAttribute("likeNotice", messageVo);
        }

        //查询关注类通知
        message = messageService.findLatestNotice(user.getId(), TOPIC_FOLLOW);
        if(message != null) {
            Map<String, Object> messageVo = new HashMap<>();

            messageVo.put("message", message);

            //处理包含已转义字符的JSON字符串
            String content = HtmlUtils.htmlUnescape(message.getContent());  //反转义
            HashMap<String, Object> data = JSONObject.parseObject(content, HashMap.class);

            //保存data中的信息
            messageVo.put("user", userService.findUserById((Integer) data.get("userId")));
            messageVo.put("entityType", data.get("entityType"));
            messageVo.put("entityId", data.get("entityId"));

            int count = messageService.findNoticeCount(user.getId(), TOPIC_FOLLOW);
            messageVo.put("count", count);

            int unread = messageService.findNoticeUnreadCount(user.getId(), TOPIC_FOLLOW);
            messageVo.put("unread", unread);

            model.addAttribute("followNotice", messageVo);
        }

        //查询关注的用户发布帖子的通知
        message = messageService.findLatestNotice(user.getId(), TOPIC_PUBLISH);
        if(message != null) {
            Map<String, Object> messageVo = new HashMap<>();

            messageVo.put("message", message);

            //处理包含已转义字符的JSON字符串
            String content = HtmlUtils.htmlUnescape(message.getContent());  //反转义
            HashMap<String, Object> data = JSONObject.parseObject(content, HashMap.class);

            //保存data中的信息
            messageVo.put("user", userService.findUserById((Integer) data.get("userId")));
            messageVo.put("entityType", data.get("entityType"));
            messageVo.put("entityId", data.get("entityId"));
            messageVo.put("postId", data.get("postId"));
            messageVo.put("postTitle", data.get("postTitle"));

            int count = messageService.findNoticeCount(user.getId(), TOPIC_PUBLISH);
            messageVo.put("count", count);

            int unread = messageService.findNoticeUnreadCount(user.getId(), TOPIC_PUBLISH);
            messageVo.put("unread", unread);

            model.addAttribute("followeePostNotice", messageVo);
        }

        //查询未读消息数量
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount", letterUnreadCount);
        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);

        return "/site/notice";
    }

    /* 通知详情页面 */
    @RequestMapping(path = "/notice/detail/{topic}", method = RequestMethod.GET)
    public String getNoticeDetail(@PathVariable("topic") String topic, Page page, Model model) {
        User user = hostHolder.getUser();

        //设置分页信息
        page.setLimit(5);
        page.setPath("/notice/detail/" + topic);
        page.setRows(messageService.findNoticeCount(user.getId(), topic));

        //查询消息
        List<Message> noticeList = messageService.findNotices(user.getId(), topic, page.getOffset(), page.getLimit());
        List<Map<String, Object>> noticeVoList = new ArrayList<>();
        if(noticeList != null) {
            for(Message notice : noticeList) {
                Map<String, Object> map = new HashMap<>();
                map.put("notice", notice);

                //通知内容
                String content = HtmlUtils.htmlUnescape(notice.getContent());
                Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
                map.put("user", userService.findUserById((Integer) data.get("userId")));
                map.put("entityType", data.get("entityType"));
                map.put("entityId", data.get("entityId"));
                map.put("postId", data.get("postId"));
                map.put("postTitle", data.get("postTitle"));

                //该通知的作者（系统用户名）
                map.put("fromUser", userService.findUserById(notice.getFromId()));

                noticeVoList.add(map);
            }
        }
        model.addAttribute("notices", noticeVoList);

        //设置已读
        List<Integer> ids = this.getLetterIds(noticeList);
        if(!ids.isEmpty()) {
            messageService.readMessage(ids);
        }

        return "/site/notice-detail";
    }
}
