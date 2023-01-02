package com.nowcoder.community.controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController implements CommunityConstant {
    @Autowired
    private ElasticsearchService elasticsearchService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private LikeService likeService;
    
    //路径：/search?keyword=xxx
    @RequestMapping(path = "/search", method = RequestMethod.GET)
    public String search(String keyword, Page page, Model model) throws IOException {
        //搜索帖子
        Map<String, Object> searchResult =
                elasticsearchService.searchDiscussPost(keyword, page.getCurrent() - 1, page.getLimit());

        //需要查询每个帖子的作者和点赞数量信息，将这些信息封装为Map
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if(searchResult != null){
            List<DiscussPost> list = (List<DiscussPost>) searchResult.get("discussPosts");
            for(DiscussPost discussPost : list){
                Map<String, Object> map = new HashMap<>();
                map.put("post", discussPost);
                map.put("user", userService.findUserById(discussPost.getUserId()));
                map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPost.getId()));
                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts", discussPosts);

        //在页面显示用户输入的关键字
        model.addAttribute("keyword", keyword);

        //设置分页信息
        page.setPath("/search?keyword=" + keyword);
        int rows = ((Number) (searchResult.get("numOfHits"))).intValue();
        page.setRows(searchResult == null ? 0 : rows);

        return "site/search";
    }
}
