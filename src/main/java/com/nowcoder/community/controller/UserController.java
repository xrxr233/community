package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.*;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private HostHolder hostHolder;

    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage() {
        return "/site/setting";
    }

    /* 更改头像 */
    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model) {
        if(headerImage == null) {
            model.addAttribute("error", "您还没有选择图片！");
            return "/site/setting";
        }

        //获取图片后缀
        String fileName = headerImage.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        if(StringUtils.isBlank(suffix)) {
            model.addAttribute("error", "文件格式不正确！");
            return "/site/setting";
        }

        //生成随机文件名
        fileName = CommunityUtil.generateUUID() + suffix;
        //确定文件存放路径
        File dest = new File(uploadPath + "/" + fileName);
        try {
            //存储头像
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败：" + e.getMessage());
            throw new RuntimeException("上传文件失败，服务器发生异常！", e);
        }

        //更新当前用户头像的路径（web访问路径）
        //http://localhost:8080/community/user/header/服务器硬盘路径
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + fileName;
        userService.updateHeader(user.getId(), headerUrl);

        return "redirect:/index";
    }

    @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        //服务器硬盘路径
        fileName = uploadPath + "/" + fileName;

        //文件后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));

        //响应图片
        response.setContentType("image/" + suffix);
        FileInputStream fis = null;
        ServletOutputStream os = null;
        try {
            fis = new FileInputStream(fileName);
            os = response.getOutputStream();
            byte[] buffer = new byte[1024];
            int b = 0;
            while((b = fis.read(buffer)) != -1) {
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            logger.error("读取头像失败：" + e.getMessage());
        }finally {
            if(fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    logger.error("关闭文件输入流失败：" + e.getMessage());
                }
            }
        }
    }

    @LoginRequired
    @RequestMapping(path = "/changepassword", method = RequestMethod.POST)
    public String changePassword(String oldPassword, String newPassword, Model model) {
        //检查用户输入是否为空
        if(StringUtils.isBlank(oldPassword) || StringUtils.isBlank(newPassword)) {
            model.addAttribute("passwordMsg", "密码不能为空！");
            return "/site/setting";
        }

        //检查用户原密码是否正确
        User user = hostHolder.getUser();
        if(user != null) {
            oldPassword = CommunityUtil.md5(oldPassword + user.getSalt());
            if(!oldPassword.equals(user.getPassword())) {
                model.addAttribute("passwordMsg", "原密码错误！");
                return "/site/setting";
            }
            //修改用户密码
            userService.modifyPassword(user, newPassword);
            return "redirect:/logout";
        }else {
            //用户凭证过期，跳转到中间页面，提醒用户重新登录
            model.addAttribute("msg", "您的密码没有被修改，请重新登录！");
            model.addAttribute("target", "/login");
            return "/site/operate-result";
        }
    }

    /* 个人主页（userId为要查看的用户主页的用户id） */
    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model) {
        User user = userService.findUserById(userId);
        if(user == null) {
            throw new RuntimeException("该用户不存在！");
        }

        //用户信息
        model.addAttribute("user", user);
        model.addAttribute("likeCount", likeService.findUserLikeCount(userId));  //获赞数

        //关注数量（关注的用户数量）
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);

        //粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);

        //当前用户是否关注该用户
        boolean hasFollowed = false;
        if(hostHolder.getUser() != null) {
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);

        return "/site/profile";
    }

    /* 用户曾经发布过的帖子 */
    @RequestMapping(path = "/userpost/{userId}", method = RequestMethod.GET)
    public String getDiscussPostOfUser(@PathVariable("userId") int userId, Page page, Model model) {
        //查询用户
        User user = userService.findUserById(userId);
        if(user == null) {
            throw new RuntimeException("该用户不存在！");
        }
        model.addAttribute("user", user);

        //查询用户发布过的帖子数量
        int postNumber = discussPostService.findDiscussPostRows(userId);
        model.addAttribute("postNumber", postNumber);

        //设置分页信息
        page.setLimit(5);
        page.setPath("/user/userpost/" + userId);
        page.setRows(postNumber);

        //查询用户发布过的帖子
        List<Map<String, Object>> discussPostVoList = new ArrayList<>();
        List<DiscussPost> discussPosts = discussPostService.findDiscussPosts(userId, page.getOffset(), page.getLimit(), 0);
        if(discussPosts != null) {
            for(DiscussPost post : discussPosts) {
                Map<String, Object> map = new HashMap<>();
                map.put("post", post);
                //查询帖子的点赞数
                map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));

                discussPostVoList.add(map);
            }
        }
        model.addAttribute("posts", discussPostVoList);

        return "/site/my-post";
    }

    /* 用户曾经发布过的对帖子的评论 */
    @RequestMapping(path = "/userreply/{userId}", method = RequestMethod.GET)
    public String getUserCommentToDiscussPost(@PathVariable("userId") int userId, Page page, Model model) {
        //查询用户
        User user = userService.findUserById(userId);
        if(user == null) {
            throw new RuntimeException("该用户不存在！");
        }
        model.addAttribute("user", user);

        //查询用户发布过的评论数量（针对帖子）
        int commentNumber = commentService.findCommentCountByUserId(userId, ENTITY_TYPE_POST);
        model.addAttribute("commentNumber", commentNumber);

        //设置分页信息
        page.setLimit(5);
        page.setPath("/user/userreply/" + userId);
        page.setRows(commentNumber);

        //查询用户发布过的对帖子的评论
        List<Map<String, Object>> commentVoList = new ArrayList<>();
        List<Comment> commentToDiscussPostList = commentService.findCommentByUserId(userId, ENTITY_TYPE_POST, page.getOffset(), page.getLimit());
        if(commentToDiscussPostList != null) {
            for(Comment comment : commentToDiscussPostList) {
                Map<String, Object> map = new HashMap<>();
                map.put("comment", comment);
                //查询评论的帖子
                map.put("post", discussPostService.findDiscussPostById(comment.getEntityId()));

                commentVoList.add(map);
            }
        }
        model.addAttribute("comments", commentVoList);

        return "/site/my-reply";
    }
}
