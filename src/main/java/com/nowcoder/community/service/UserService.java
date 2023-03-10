package com.nowcoder.community.service;

import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private RedisTemplate redisTemplate;

//    @Autowired
//    private LoginTicketMapper loginTicketMapper;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    private SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");

    public User findUserById(int id) {
//        return userMapper.selectById(id);
        User user = this.getCache(id);
        if(user == null) {
            user = this.initCache(id);
        }
        return user;
    }

    public User findUserByEmail(String email) {
        return userMapper.selectByEmail(email);
    }

    /* 注册 */
    public Map<String, Object> register(User user) {
        Map<String, Object> map = new HashMap<>();
        //空值处理
        if(user == null) {
            throw new IllegalArgumentException("参数不能为空！");
        }
        if(StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "账号不能为空！");
            return map;
        }
        if(StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码不能为空！");
            return map;
        }
        if(StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱不能为空！");
            return map;
        }

        //验证账号
        User u = userMapper.selectByName(user.getUsername());
        if(u != null) {
            map.put("usernameMsg", "该账号已存在！");
            return map;
        }
        //验证邮箱
        u = userMapper.selectByEmail(user.getEmail());
        if(u != null) {
            map.put("emailMsg", "该邮箱已被注册！");
            return map;
        }

        //注册用户
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        //向Redis中存入用户创建时间
        String redisKey = RedisKeyUtil.getUserCreateTimeKey(user.getId());
        redisTemplate.opsForValue().set(redisKey, df.format(user.getCreateTime()));

        //激活邮件
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        //激活的url：http://localhost:8080/community/activation/用户id/激活码，用户id在添加用户到MySQL时自动回填到user
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(), "激活账号", content);

        return map;
    }

    /* 激活 */
    public int activation(int userId, String code) {
        User user = userMapper.selectById(userId);
        if(user.getStatus() == 1) {
            return ACTIVATION_REPEAT;
        }else if(user.getActivationCode().equals(code)) {
            userMapper.updateStatus(userId, 1);
            this.clearCache(userId);  //发生更新，清除缓存
            return ACTIVATION_SUCCESS;
        }else {
            return ACTIVATION_FAILURE;
        }
    }

    /* 登录 */
    public Map<String, Object> login(String username, String password, int expiredSeconds) {
        Map<String, Object> map = new HashMap<>();

        //空值处理
        if(StringUtils.isBlank(username)) {
            map.put("usernameMsg", "账号不能为空！");
            return map;
        }
        if(StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空！");
            return map;
        }

        //验证账号
        User user = userMapper.selectByName(username);
        if(user == null) {
            map.put("usernameMsg", "该账号不存在！");
            return map;
        }

        //验证账号是否激活
        if(user.getStatus() == 0) {
            map.put("usernameMsg", "该账号未激活！");
            return map;
        }

        //验证密码
        password = CommunityUtil.md5(password + user.getSalt());
        if(!user.getPassword().equals(password)) {
            map.put("passwordMsg", "密码不正确！");
            return map;
        }

        //生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));
//        loginTicketMapper.insertLoginTicket(loginTicket);

        //保存到Redis中
        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(redisKey, loginTicket);  //Redis自动把Ticket对象序列化为JSON字符串

        map.put("ticket", loginTicket.getTicket());

        return map;
    }

    /* 登出 */
    public void logout(String ticket) {
//        loginTicketMapper.updateStatus(ticket, 1);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(redisKey, loginTicket);
    }

    /* 忘记密码，向用户邮箱发送验证码 */
    public void emailVerificationCode(String email, String verificationCode) {
        Context context = new Context();
        context.setVariable("email", email);
        context.setVariable("verificationCode", verificationCode);
        String content = templateEngine.process("/mail/forget", context);
        mailClient.sendMail(email, "忘记密码", content);
    }

    /* 忘记密码，修改用户密码 */
    public Map<String, Object> modifyPassword(String email, String newPassword) {
        Map<String, Object> map = new HashMap<>();

        //查找用户是否存在
        User user = userMapper.selectByEmail(email);
        if(user == null) {
            map.put("emailMsg", "邮箱不存在！");
            return map;
        }

        //修改用户密码
        newPassword = CommunityUtil.md5(newPassword + user.getSalt());
        userMapper.updatePassword(user.getId(), newPassword);

        //清除缓存
        this.clearCache(user.getId());

        return map;
    }

    /* 查询凭证，用于检查登录状态 */
    public LoginTicket findLoginTicket(String ticket) {
//        return loginTicketMapper.selectByTicket(ticket);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(redisKey);
    }

    /* 更新用户头像 */
    public int updateHeader(int userId, String headerUrl) {
//        return userMapper.updateHeader(userId, headerUrl);
        int rows = userMapper.updateHeader(userId, headerUrl);

        //清除缓存
        this.clearCache(userId);

        return rows;
    }

    /* 修改用户密码 */
    public void modifyPassword(User user, String newPassword) {
        newPassword = CommunityUtil.md5(newPassword + user.getSalt());
        userMapper.updatePassword(user.getId(), newPassword);

        //清除缓存
        this.clearCache(user.getId());
    }

    public User findUserByName(String username) {
        return userMapper.selectByName(username);
    }

    /* 根据id从表中删除一条用户记录（仅供测试用） */
    public int deleteUserById(int id) {
        return userMapper.deleteUserById(id);
    }

    /* 查询表中所有用户id（系统用户id=1除外） */
    public List<Integer> findUserIds() {
        return userMapper.selectUserIds();
    }

    /*
    * 缓存原则
    * 1、优先从缓存中取值
    * 2、取不到则初始化缓存数据
    * 3、数据变化时清除缓存数据，如修改密码、更新头像
    * */

    /* 从缓存中取值 */
    private User getCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }

    /* 初始化缓存 */
    private User initCache(int userId) {
        User user = userMapper.selectById(userId);
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);
        return user;
    }

    /* 清除缓存 */
    private void clearCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }

    /* 获得用户的权限 */
    public Collection<? extends GrantedAuthority> getAuthorities(int userId) {
        User user = this.findUserById(userId);

        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                switch (user.getType()) {
                    case 1:
                        return AUTHORITY_ADMIN;
                    case 2:
                        return AUTHORITY_MODERATOR;
                    default:
                        return AUTHORITY_USER;
                }
            }
        });

        return list;
    }

}
