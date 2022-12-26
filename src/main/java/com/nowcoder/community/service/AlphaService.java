package com.nowcoder.community.service;

import com.nowcoder.community.dao.AlphaDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service
public class AlphaService {
    @Autowired
    private AlphaDao alphaDao;

    public AlphaService() {
        System.out.println("构造AlphaService对象");
    }

    @PostConstruct
    public void init() {
        System.out.println("在构造器调用后，调用初始化方法");
    }

    @PreDestroy
    public void destory() {
        System.out.println("在对象销毁前调用，回收资源");
    }

    public String find() {
        return alphaDao.select();
    }
}
