package com.nowcoder.community.controller;

import com.google.code.kaptcha.Producer;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;

@Controller
public class LoginController implements CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private Producer kaptchaProducer;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    /* 注册页面 */
    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String getRegisterPage() {
        return "/site/register";
    }

    /* 登录页面 */
    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLoginPage() {
        return "/site/login";
    }

    /* 忘记密码页面 */
    @RequestMapping(path = "/forget", method = RequestMethod.GET)
    public String getForgetPage() {
        return "/site/forget";
    }

    /* 注册
    *  SpringMVC自动根据字段设置user对象属性
    *  */
    @RequestMapping(path = "/register", method = RequestMethod.POST)
    public String register(Model model, User user) {
        Map<String, Object> map = userService.register(user);
        if(map == null || map.isEmpty()) {
            //注册成功，跳转到中间页面
            model.addAttribute("msg", "注册成功，我们已经向您的邮箱发送了一封激活邮件，请尽快激活！");
            model.addAttribute("target", "/index");
            return "/site/operate-result";
        }else {
            //注册失败
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "/site/register";
        }
    }

    /* 激活
       http://localhost:8080/community/activation/用户id/激活码
     */
    @RequestMapping(path = "/activation/{userId}/{code}", method = RequestMethod.GET)
    public String activation(Model model, @PathVariable("userId") int userId, @PathVariable("code") String code) {
        int result = userService.activation(userId, code);
        if(result == ACTIVATION_SUCCESS) {
            model.addAttribute("msg", "激活成功，您的账号已经可以正常使用了！");
            model.addAttribute("target", "/login");
        }else if(result == ACTIVATION_REPEAT) {
            model.addAttribute("msg", "无效操作，您的账号已经激活过了！");
            model.addAttribute("target", "/index");
        }else {
            model.addAttribute("msg", "激活失败，您提供的激活码不正确！");
            model.addAttribute("target", "/index");
        }
        return "/site/operate-result";
    }

    /* 生成验证码（由浏览器自动请求） */
    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response, HttpSession session) {
        //生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        //将验证码存入session
        session.setAttribute("kaptcha", text);

        //将图片输出给浏览器
        response.setContentType("image/png");
        try {
            ServletOutputStream os = response.getOutputStream();
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            logger.error("响应验证码失败：" + e.getMessage());
        }
    }

    /* 登录 */
    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public String login(String username, String password, String code, boolean rememberme,
                        Model model, HttpSession session, HttpServletResponse response) {
        //检查验证码
        String kaptcha = (String) session.getAttribute("kaptcha");
        if(StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)) {
            model.addAttribute("codeMsg", "验证码不正确！");
            return "/site/login";
        }

        //检查账号，密码
        int expiredSeconds = rememberme ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        if(map.containsKey("ticket")) {
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index";
        }else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/login";
        }
    }

    /* 登出 */
    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket) {
        userService.logout(ticket);
        return "redirect:/login";  //重定向默认是GET请求
    }

    /* 忘记密码，向指定邮箱发送验证码 */
    @RequestMapping(path = "/getcode/{email}", method = RequestMethod.GET)
    public String getCode(@PathVariable("email") String email, HttpSession session, Model model) {
        //检查邮箱是否存在
        if(StringUtils.isBlank(email)) {
            model.addAttribute("emailMsg", "邮箱不能为空！");
            return "/site/forget";
        }
        User user = userService.findUserByEmail(email);
        if(user == null) {
            model.addAttribute("emailMsg", "邮箱不存在！");
            return "/site/forget";
        }

        //生成4位验证码，并存入session
        String verificationCode = CommunityUtil.generateUUID().substring(0, 4);
        session.setAttribute("verificationCode", verificationCode);

        //向邮箱发送包含验证码邮件
        userService.emailVerificationCode(email, verificationCode);
        model.addAttribute("emailMsg", "验证码已发送！");

        return "/site/forget";
    }

    /* 忘记密码，验证用户输入验证码，修改为新密码 */
    @RequestMapping(path = "/changepassword", method = RequestMethod.POST)
    public String changePassword(String email, String verificationCode, String newPassword,
                                 HttpSession session, Model model) {
        //检查验证码是否正确
        String code = (String) session.getAttribute("verificationCode");
        if(StringUtils.isBlank(code) || StringUtils.isBlank(verificationCode) || !code.equals(verificationCode)) {
            model.addAttribute("codeMsg", "验证码不正确！");
            return "/site/forget";
        }

        //修改用户密码
        Map<String, Object> map = userService.modifyPassword(email, newPassword);
        if(map == null || map.isEmpty()) {
            //成功
            return "redirect:/login";
        }else {
            //失败
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "/site/forget";
        }
    }

}
