package com.nowcoder.community;

import com.alibaba.fastjson.JSON;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.util.CommunityUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class JSONTests {
    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Test
    public void testJSON() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "zhangsan");
        map.put("age", 25);
        System.out.println(CommunityUtil.getJSONString(0, "ok", map));
    }

    @Test
    public void testJSONArray() {
        List<DiscussPost> list = discussPostMapper.selectDiscussPosts(0, 1, 20, 1);
        String listArray = JSON.toJSON(list).toString();
        List<DiscussPost> listAfterParse = JSON.parseArray(listArray, DiscussPost.class);
        for(DiscussPost post : listAfterParse) {
            System.out.println(post);
        }
    }
}
