package com.hz.community.controller;

import com.google.code.kaptcha.Producer;
import com.hz.community.entity.User;
import com.hz.community.service.UserService;
import com.hz.community.util.CommunityConstant;
import com.hz.community.util.CommunityUtil;
import com.hz.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Controller
public class LoginController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    private UserService userService;

    private Producer producer;

    private RedisTemplate redisTemplate;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    public LoginController(UserService userService, Producer producer, RedisTemplate redisTemplate) {
        this.userService = userService;
        this.producer = producer;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/register")
    public String getRegisterPage(){
        return "/html/user/reg";
    }

    @GetMapping("/login")
    public String getLoginPage(){
        return "/html/user/login";
    }
    //发送激活邮件
    @PostMapping("/register")
    public String register(Model model, User user) {
        Map<String, Object> map = userService.register(user);
        if (map == null || map.isEmpty()) {
            model.addAttribute("msg", "注册成功，我们已经向您的邮箱发送了一封激活邮件，请尽快激活!");
            model.addAttribute("target", "/index");
            return "/html/user/operate-result";
        }else {
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            model.addAttribute("emailMsg",map.get("emailMsg"));
            return "/html/user/reg";
        }

    }
    //激活账号
    @GetMapping("/activation/{userId}/{code}")
    public String activation(Model model, @PathVariable("userId") int userId,@PathVariable("code") String code){
        int result = userService.activation(userId,code);
        if(result == ACTIVATION_SUCCESS){
            model.addAttribute("msg", "激活成功，您的账号已经可以正常使用!");
            model.addAttribute("target", "/login");
        }else if(result == ACTIVATION_REPEAT){
            model.addAttribute("msg", "无效操作，该账号已经激活过了!");
            model.addAttribute("target", "/index");
        }else{
            model.addAttribute("msg", "激活失败，您提供的激活码不正确!");
            model.addAttribute("target", "/index");
        }
        return "/html/user/operate-result";
    }

    @GetMapping("/forget")
    public String getForgetPage() {
        return "/html/user/forget";
    }

    @GetMapping("/resetPage/{email}/{recordCode}")
    public String getForgeTwotPage(@PathVariable("email") String email,@PathVariable("recordCode") String recordCode, Model model) {
        model.addAttribute("email", email);
        model.addAttribute("recordCode", recordCode);
        return "/html/user/forget_change";
    }
    //发送验证码邮件
    @PostMapping("/sendcode")
    public String forget(Model model, String email) {
        Map<String, Object> map = userService.forgetPassword(email);
        if (map.get("code") != null) {
            User user = (User) map.get("user");
            String recordCode = (String) map.get("code");
            recordCode = CommunityUtil.md5(recordCode + user.getSalt());
            model.addAttribute("msg", "发送成功，我们已经向您的邮箱发送了一封验证码邮件!");
            model.addAttribute("target", "/resetPage/" + email + "/" + recordCode);
            return "/html/user/operate-result";
        } else {
            model.addAttribute("emailMsg", map.get("emailMsg"));
            model.addAttribute("email", email);
            return "/html/user/forget";
        }
    }

    //重置密码
    @PostMapping("/reset/{email}/{recordCode}")
    public String resetPassword(@PathVariable("email") String email, @PathVariable("recordCode") String recordCode, String code, String resetPassword, Model model) {
        if (StringUtils.isBlank(resetPassword)) {
            model.addAttribute("passwordMsg", "密码不能为空!");
            model.addAttribute("code", code);
            model.addAttribute("recordCode",recordCode);
            model.addAttribute("email", email);
            return "/html/user/forget_change";
        }
        if (StringUtils.isBlank(code)) {
            model.addAttribute("codeMsg", "验证码不能为空!");
            model.addAttribute("resetPassword", resetPassword);
            model.addAttribute("recordCode",recordCode);
            model.addAttribute("email", email);
            return "/html/user/forget_change";
        }
        User user = userService.findUserByEmail(email);
        String code_md5 = CommunityUtil.md5(code + user.getSalt());
        if (!recordCode.equals(code_md5)) {
            model.addAttribute("codeMsg", "验证码不正确!");
            model.addAttribute("resetPassword", resetPassword);
            model.addAttribute("code", code);
            model.addAttribute("recordCode",recordCode);
            model.addAttribute("email", email);
            return "/html/user/forget_change";
        }

        resetPassword = CommunityUtil.md5(resetPassword + user.getSalt());
        userService.changePassword(user.getId(), resetPassword);

        return "/html/user/login";
    }

    //生成验证码
    @GetMapping("/kaptcha")
    public void getKaptcha(HttpServletResponse response){
        //生成验证码
        String text = producer.createText();
        BufferedImage image = producer.createImage(text);

        //验证码归属
        String kaptchOwner = CommunityUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchOwner", kaptchOwner);
        cookie.setMaxAge(60);
        cookie.setPath(contextPath);
        response.addCookie(cookie);
        //将验证码存入Redis
        String redisKey = RedisKeyUtil.getKaptchaKey(kaptchOwner);
        redisTemplate.opsForValue().set(redisKey, text,60, TimeUnit.SECONDS);

        //将图片输出给浏览器
        response.setContentType("image/png");
        try {
            OutputStream os = response.getOutputStream();
            ImageIO.write(image,"png",os);
        } catch (IOException e) {
           logger.error("响应验证码失败"+e.getMessage());
        }
    }
    //登录
    @PostMapping("/login")
    public String login(String username,String password,String code,boolean remember_me,Model model,HttpServletResponse response,@CookieValue("kaptchOwner") String kaptchOwner){
        //String kaptcha = (String)session.getAttribute("kaptcha");
        String kaptcha = null;
        if (StringUtils.isNoneBlank(kaptchOwner)) {
            String redisKey = RedisKeyUtil.getKaptchaKey(kaptchOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
        }
        //检查验证码
        if(StringUtils.isBlank(kaptcha)||StringUtils.isBlank(code)||!kaptcha.equalsIgnoreCase(code)){
            model.addAttribute("codeMsg","验证码不正确!");
            return "/html/user/login";
        }
        int expiredSeconds = remember_me ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        Map<String,Object> map = userService.login(username,password,expiredSeconds);
        if(map.containsKey("ticket")){
            Cookie cookie = new Cookie("ticket",map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index";
        }else {
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            return "/html/user/login";
        }

    }
    //退出
    @GetMapping("/logout")
    public String logout(@CookieValue("ticket") String ticket){
        userService.logout(ticket);
        SecurityContextHolder.clearContext();
        return "redirect:/index";
    }

}
