package com.nowcoder.community;

import com.nowcoder.community.util.SensitiveFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class SensitiveTests {
    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Test
    public void testSensitiveFilter() {
        String text = ")))&*※赌博可※以开)(票，※开※※※博以及※吸※※毒还有吃饭，还可以)嫖)※娼(啊啊※啊※※！ll**)吸(毒)";
        text = sensitiveFilter.filter(text);
        System.out.println(text);
    }

}
