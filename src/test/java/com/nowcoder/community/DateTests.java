package com.nowcoder.community;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.Calendar;
import java.util.Date;

@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class DateTests {
    @Test
    public void test1() {
        Date end = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(end);
        calendar.add(Calendar.DATE, -7);
        Date start = calendar.getTime();

        System.out.println(start + ": " + start.getTime());
        System.out.println(end + ": " + end.getTime());
    }
}
