package com.nowcoder.community;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class StringAndByteTests {
    @Test
    public  void test1() {
        byte[] byteArray = {-100, 98, 125};
        try {
            String s = new String(byteArray, "UTF8");
            byte[] bytes = s.getBytes("UTF8");
            System.out.println(s);
            System.out.println(Arrays.toString(bytes));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
