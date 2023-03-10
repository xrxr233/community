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

    /* ???????????? */
    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model) {
        if(headerImage == null) {
            model.addAttribute("error", "???????????????????????????");
            return "/site/setting";
        }

        //??????????????????
        String fileName = headerImage.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        if(StringUtils.isBlank(suffix)) {
            model.addAttribute("error", "????????????????????????");
            return "/site/setting";
        }

        //?????????????????????
        fileName = CommunityUtil.generateUUID() + suffix;
        //????????????????????????
        File dest = new File(uploadPath + "/" + fileName);
        try {
            //????????????
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("?????????????????????" + e.getMessage());
            throw new RuntimeException("?????????????????????????????????????????????", e);
        }

        //????????????????????????????????????web???????????????
        //http://localhost:8080/community/user/header/?????????????????????
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + fileName;
        userService.updateHeader(user.getId(), headerUrl);

        return "redirect:/index";
    }

    @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        //?????????????????????
        fileName = uploadPath + "/" + fileName;

        //????????????
        String suffix = fileName.substring(fileName.lastIndexOf("."));

        //????????????
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
            logger.error("?????????????????????" + e.getMessage());
        }finally {
            if(fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    logger.error("??????????????????????????????" + e.getMessage());
                }
            }
        }
    }

    @LoginRequired
    @RequestMapping(path = "/changepassword", method = RequestMethod.POST)
    public String changePassword(String oldPassword, String newPassword, Model model) {
        //??????????????????????????????
        if(StringUtils.isBlank(oldPassword) || StringUtils.isBlank(newPassword)) {
            model.addAttribute("passwordMsg", "?????????????????????");
            return "/site/setting";
        }

        //?????????????????????????????????
        User user = hostHolder.getUser();
        if(user != null) {
            oldPassword = CommunityUtil.md5(oldPassword + user.getSalt());
            if(!oldPassword.equals(user.getPassword())) {
                model.addAttribute("passwordMsg", "??????????????????");
                return "/site/setting";
            }
            //??????????????????
            userService.modifyPassword(user, newPassword);
            return "redirect:/logout";
        }else {
            //?????????????????????????????????????????????????????????????????????
            model.addAttribute("msg", "????????????????????????????????????????????????");
            model.addAttribute("target", "/login");
            return "/site/operate-result";
        }
    }

    /* ???????????????userId????????????????????????????????????id??? */
    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model) {
        User user = userService.findUserById(userId);
        if(user == null) {
            throw new RuntimeException("?????????????????????");
        }

        //????????????
        model.addAttribute("user", user);
        model.addAttribute("likeCount", likeService.findUserLikeCount(userId));  //?????????

        //???????????????????????????????????????
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);

        //????????????
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);

        //?????????????????????????????????
        boolean hasFollowed = false;
        if(hostHolder.getUser() != null) {
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);

        return "/site/profile";
    }

    /* ?????????????????????????????? */
    @RequestMapping(path = "/userpost/{userId}", method = RequestMethod.GET)
    public String getDiscussPostOfUser(@PathVariable("userId") int userId, Page page, Model model) {
        //????????????
        User user = userService.findUserById(userId);
        if(user == null) {
            throw new RuntimeException("?????????????????????");
        }
        model.addAttribute("user", user);

        //????????????????????????????????????
        int postNumber = discussPostService.findDiscussPostRows(userId);
        model.addAttribute("postNumber", postNumber);

        //??????????????????
        page.setLimit(5);
        page.setPath("/user/userpost/" + userId);
        page.setRows(postNumber);

        //??????????????????????????????
        List<Map<String, Object>> discussPostVoList = new ArrayList<>();
        List<DiscussPost> discussPosts = discussPostService.findDiscussPosts(userId, page.getOffset(), page.getLimit(), 0);
        if(discussPosts != null) {
            for(DiscussPost post : discussPosts) {
                Map<String, Object> map = new HashMap<>();
                map.put("post", post);
                //????????????????????????
                map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));

                discussPostVoList.add(map);
            }
        }
        model.addAttribute("posts", discussPostVoList);

        return "/site/my-post";
    }

    /* ?????????????????????????????????????????? */
    @RequestMapping(path = "/userreply/{userId}", method = RequestMethod.GET)
    public String getUserCommentToDiscussPost(@PathVariable("userId") int userId, Page page, Model model) {
        //????????????
        User user = userService.findUserById(userId);
        if(user == null) {
            throw new RuntimeException("?????????????????????");
        }
        model.addAttribute("user", user);

        //??????????????????????????????????????????????????????
        int commentNumber = commentService.findCommentCountByUserId(userId, ENTITY_TYPE_POST);
        model.addAttribute("commentNumber", commentNumber);

        //??????????????????
        page.setLimit(5);
        page.setPath("/user/userreply/" + userId);
        page.setRows(commentNumber);

        //??????????????????????????????????????????
        List<Map<String, Object>> commentVoList = new ArrayList<>();
        List<Comment> commentToDiscussPostList = commentService.findCommentByUserId(userId, ENTITY_TYPE_POST, page.getOffset(), page.getLimit());
        if(commentToDiscussPostList != null) {
            for(Comment comment : commentToDiscussPostList) {
                Map<String, Object> map = new HashMap<>();
                map.put("comment", comment);
                //?????????????????????
                map.put("post", discussPostService.findDiscussPostById(comment.getEntityId()));

                commentVoList.add(map);
            }
        }
        model.addAttribute("comments", commentVoList);

        return "/site/my-reply";
    }
}
