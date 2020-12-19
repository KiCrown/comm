package com.hz.community.controller;

import com.hz.community.entity.DiscussPost;
import com.hz.community.entity.Message;
import com.hz.community.entity.Page;
import com.hz.community.entity.User;
import com.hz.community.service.*;
import com.hz.community.util.CommunityConstant;
import com.hz.community.util.CommunityUtil;
import com.hz.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;

@Controller
public class PageController implements CommunityConstant {


    private DiscussPostService discussPostService;
    private UserService userService;
    private LikeService likeService;
    private MessageService messageService;
    private HostHolder hostHolder;
    private DataService dataService;

    @Autowired
    public PageController(DiscussPostService discussPostService, UserService userService, LikeService likeService, MessageService messageService, HostHolder hostHolder, DataService dataService) {
        this.discussPostService = discussPostService;
        this.userService = userService;
        this.likeService = likeService;
        this.messageService = messageService;
        this.hostHolder = hostHolder;
        this.dataService = dataService;
    }

    @GetMapping("/")
    public String root() {
        return "forward:/index";
    }

    @GetMapping("/index")
    public String getIndexPage(Model model, Page page, @RequestParam(name = "orderMode", defaultValue = "0") int orderMode, @RequestParam(name = "status", defaultValue = "-1") int status) {
        List<DiscussPost> list = null;
        if (status != -1) {
            page.setRows(discussPostService.findDiscussPostRowsOfStatus(0, status));
            page.setPath("/index?status="+status+"&orderMode="+orderMode);
            list = discussPostService.findDiscussPostsByStatus(0, page.getOffset(), page.getLimit(), orderMode, status);
        } else {
            page.setRows(discussPostService.findDiscussPostRows(0));
            page.setPath("/index?status="+status+"&orderMode="+orderMode);
            list = discussPostService.findDiscussPosts(0, page.getOffset(), page.getLimit(), orderMode);
        }



        //最近登录用户
        List<Map<String, Object>> userList = dataService.findLogin(new Date());

        model.addAttribute("users", userList);

        if (hostHolder.getUser() != null) {
            //签到
            boolean hasSigned = dataService.hasSigned(hostHolder.getUser().getId(), new Date());
            model.addAttribute("hasSigned", hasSigned);
            int signedCount = dataService.getSignedCount(hostHolder.getUser().getId());
            model.addAttribute("signedCount", signedCount);
        }


        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if (list != null) {
            for (DiscussPost post : list) {
                Map<String, Object> map = new HashMap<>();
                map.put("post", post);
                User user = userService.findUserById(post.getUserId());
                map.put("user", user);

                long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
                map.put("likeCount", likeCount);

                discussPosts.add(map);
            }
        }
        List<DiscussPost> topList = discussPostService.findDiscussPostsByType(0, 1,-1);
        List<Map<String, Object>> tops = new ArrayList<>();
        if (topList != null) {
            for (DiscussPost post : topList) {
                Map<String, Object> map = new HashMap<>();
                map.put("post", post);
                User user = userService.findUserById(post.getUserId());
                map.put("user", user);

                long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
                map.put("likeCount", likeCount);

                tops.add(map);
            }
        }
        if (hostHolder.getUser() != null) {
            getNoticeList(hostHolder.getUser(), model);
            int letterUnreadCount = messageService.findLetterUnreadCount(hostHolder.getUser().getId(), null);
            model.addAttribute("letterUnreadCount", letterUnreadCount);
        }



        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("tops", tops);
        model.addAttribute("orderMode", orderMode);
        model.addAttribute("status", status);
        return "/html/index";
    }



    @GetMapping("/sort/{tip}&{orderMode}")
    public String getTipPage(Model model, Page page, @PathVariable("orderMode") int orderMode, @PathVariable("tip") int tip,@RequestParam(name = "status",defaultValue = "-1") int status) {
        page.setRows(discussPostService.findDiscussPostRowsOfTip(0, tip, status));
        page.setPath("/sort/" + tip + "&" + orderMode + "?status=" + status);
        List<DiscussPost> list = discussPostService.findDiscussPostsByTip(0, page.getOffset(), page.getLimit(), orderMode, tip, status);
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if (list != null) {
            for (DiscussPost post : list) {
                Map<String, Object> map = new HashMap<>();
                map.put("post", post);
                User user = userService.findUserById(post.getUserId());
                map.put("user", user);

                long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
                map.put("likeCount", likeCount);

                discussPosts.add(map);
            }
        }
        List<DiscussPost> topList = discussPostService.findDiscussPostsByType(0, 1, tip);
        List<Map<String, Object>> tops = new ArrayList<>();
        if (topList != null) {
            for (DiscussPost post : topList) {
                Map<String, Object> map = new HashMap<>();
                map.put("post", post);
                User user = userService.findUserById(post.getUserId());
                map.put("user", user);

                long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
                map.put("likeCount", likeCount);

                tops.add(map);
            }
        }
        //最近登录用户
        List<Map<String, Object>> userList = dataService.findLogin(new Date());

        if (hostHolder.getUser() != null) {
            //签到
            boolean hasSigned = dataService.hasSigned(hostHolder.getUser().getId(), new Date());
            model.addAttribute("hasSigned", hasSigned);
            int signedCount = dataService.getSignedCount(hostHolder.getUser().getId());
            model.addAttribute("signedCount", signedCount);
        }

        model.addAttribute("users", userList);
        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("tops", tops);
        model.addAttribute("orderMode", orderMode);
        model.addAttribute("tip", tip);
        model.addAttribute("status", status);
        return "/html/jie/sort";
    }


    @GetMapping("/error")
    public String getErrorPage() {
        return "/error/500";
    }

    @GetMapping("/denied")
    public String getDeniedPage() {
        return "/error/404";
    }



    private void getNoticeList(User user,Model model) {

        //查询评论类的通知
        Message message = messageService.findLatestNotice(user.getId(), TOPIC_COMMENT);

        if (message != null) {
            Map<String, Object> messageVo = new HashMap<>();

            int count = messageService.findNoticeCount(user.getId(), TOPIC_COMMENT);
            messageVo.put("count", count);
            int unreadCount = messageService.findNoticeUnreadCount(user.getId(), TOPIC_COMMENT);
            messageVo.put("unreadCount", unreadCount);
            model.addAttribute("commentNotice", messageVo);
        }

        //查询点赞类的通知
        message = messageService.findLatestNotice(user.getId(), TOPIC_LIKE);

        if (message != null) {
            Map<String, Object> messageVo = new HashMap<>();

            int count = messageService.findNoticeCount(user.getId(), TOPIC_LIKE);
            messageVo.put("count", count);
            int unreadCount = messageService.findNoticeUnreadCount(user.getId(), TOPIC_LIKE);
            messageVo.put("unreadCount", unreadCount);
            model.addAttribute("likeNotice", messageVo);
        }

        //查询关注类的通知
        message = messageService.findLatestNotice(user.getId(), TOPIC_FOLLOW);
        if (message != null) {
            Map<String, Object> messageVo = new HashMap<>();
            messageVo.put("message", message);

            int count = messageService.findNoticeCount(user.getId(), TOPIC_FOLLOW);
            messageVo.put("count", count);
            int unreadCount = messageService.findNoticeUnreadCount(user.getId(), TOPIC_FOLLOW);
            messageVo.put("unreadCount", unreadCount);
            model.addAttribute("followNotice", messageVo);
        }


        //查询未读消息数量
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount", letterUnreadCount);
        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);

    }
}
