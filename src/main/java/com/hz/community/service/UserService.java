package com.hz.community.service;

import com.hz.community.dao.UserMapper;
import com.hz.community.entity.LoginTicket;
import com.hz.community.entity.User;
import com.hz.community.util.CommunityConstant;
import com.hz.community.util.CommunityUtil;
import com.hz.community.util.MailClient;
import com.hz.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {

    private UserMapper userMapper;

    private MailClient mailClient;

    private TemplateEngine templateEngine;

    private RedisTemplate redisTemplate;


    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${community.path.domain}")
    private String domain;

    @Autowired
    public UserService(UserMapper userMapper, MailClient mailClient, TemplateEngine templateEngine, RedisTemplate redisTemplate) {
        this.userMapper = userMapper;
        this.mailClient = mailClient;
        this.templateEngine = templateEngine;
        this.redisTemplate = redisTemplate;
    }

    //根据id查询用户
    public User findUserById(int id){
        User user = getCache(id);
        if (user == null) {
            user = initCache(id);
        }
        return user;
    }
    //注册账号
    public Map<String,Object> register(User user){
        Map<String,Object> map = new HashMap<>();

        if(user == null){
            throw new IllegalArgumentException("参数不能为空");
        }
        if(StringUtils.isBlank(user.getUsername())){
            map.put("usernameMsg","账号不能为空!");
            return map;
        }
        if(StringUtils.isBlank(user.getPassword())){
            map.put("passwordMsg","密码不能为空!");
            return map;
        }
        if(StringUtils.isBlank(user.getEmail())){
            map.put("emailMsg","邮箱不能为空!");
            return map;
        }

        User u = userMapper.selectByName(user.getUsername());
        if(u != null){
            map.put("usernameMsg","账号已存在!");
            return map;
        }
        u = userMapper.selectByEmail(user.getEmail());
        if(u != null){
            map.put("emailMsg","邮箱已被注册!");
            return map;
        }

        user.setSalt(CommunityUtil.generateUUID().substring(0,5));
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setExperience(100);
        user.setSex(0);
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(String.format("/images/avatar/default.png"));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        Context context = new Context();
        context.setVariable("email",user.getEmail());

        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url",url);
        String content = templateEngine.process("/html/mail/activation",context);
        mailClient.sendMail(user.getEmail(),"激活账号",content);

        return map;
    }
    //激活用户账号
    public int activation(int userId,String code){
        User user = userMapper.selectById(userId);
        if(user.getStatus() == 1){
            return ACTIVATION_REPEAT;
        }else if(user.getActivationCode().equals(code)){
            userMapper.updateStatus(userId,1);
            clearCache(userId);
            return ACTIVATION_SUCCESS;
        }else {
            return ACTIVATION_FAILURE;
        }
    }

    //根据邮箱查询用户
    public User findUserByEmail(String email) {
        return userMapper.selectByEmail(email);
    }

    //发送验证码邮寄
    public Map<String, Object> forgetPassword(String email) {
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.isBlank(email)) {
            map.put("emailMsg", "邮箱不能为空!");
            return map;
        }
        User u = userMapper.selectByEmail(email);

        if (u == null) {
            map.put("emailMsg", "邮箱不存在!");
            return map;
        }
        Context context = new Context();
        context.setVariable("email", u.getEmail());
        String code = CommunityUtil.generateUUID().substring(0, 4);
        context.setVariable("code", code);
        map.put("code", code);
        map.put("user", u);
        String content = templateEngine.process("/html/mail/forget", context);

        mailClient.sendMail(u.getEmail(), "验证码", content);


        return map;
    }

    //登录
    public Map<String ,Object> login(String username,String password,int expiredSeconds){
        Map<String,Object> map = new HashMap<>();

        //空值处理
        if(StringUtils.isBlank(username)){
            map.put("usernameMsg","账号不能为空!");
            return map;
        }
        if(StringUtils.isBlank(password)){
            map.put("passwordMsg","密码不能为空!");
            return map;
        }

        //验证账号
        User user = userMapper.selectByName(username);
        if(user == null){
            map.put("usernameMsg","该账号不存在!");
            return map;
        }
        //验证状态
        if(user.getStatus() == 0){
            map.put("usernameMsg","该账号未激活!");
            return map;
        }
        //验证密码
        password = CommunityUtil.md5(password + user.getSalt());
        if(!user.getPassword().equals(password)){
            map.put("passwordMsg","密码不正确!");
            return map;
        }

        //生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds *1000));
        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(redisKey,loginTicket);
        map.put("ticket",loginTicket.getTicket());
        return map;
    }
    //退出
    public void logout(String ticket){
        //loginTicketMapper.updateStatus(ticket,1);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(redisKey, loginTicket);
    }
    //根据凭证查询
    public LoginTicket findLoginTicket(String ticket){
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(redisKey);
    }
    //更新头像
    public int updateHeader(int userId,String headerUrl){
        int rows = userMapper.updateHeader(userId,headerUrl);
        clearCache(userId);
        return rows;
    }
    //更改密码
    public void changePassword(int id,String password){
        userMapper.updatePassword(id,password);
        clearCache(id);
    }

    public User findUserByName(String username){
        return userMapper.selectByName(username);
    }

    public void updateInformation(int userId, int sex, String signature, String email, String location) {
        userMapper.updateInformation(userId, sex, signature, email, location);
        clearCache(userId);
    }

    public void updateExperience(int userId, int experience) {
        userMapper.updateExperience(userId, experience);
        clearCache(userId);
    }

    //优先从缓存取值
    private User getCache(int userId){
        String redisKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }

    //取不到时初始化缓存数据
    private User initCache(int userId) {
        User user = userMapper.selectById(userId);
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);
        return user;
    }

    //数据变更时清除缓存数据
    private void clearCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }

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
