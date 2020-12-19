package com.hz.community.controller;

import com.hz.community.annotation.LoginRequired;
import com.hz.community.entity.DiscussPost;
import com.hz.community.entity.Event;
import com.hz.community.entity.Page;
import com.hz.community.entity.User;
import com.hz.community.event.EventProducer;
import com.hz.community.service.FollowService;
import com.hz.community.service.LikeService;
import com.hz.community.service.UserService;
import com.hz.community.util.CommunityConstant;
import com.hz.community.util.CommunityUtil;
import com.hz.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class FollowController implements CommunityConstant{

    private FollowService followService;
    private HostHolder hostHolder;
    private UserService userService;
    private EventProducer eventProducer;
    private LikeService likeService;

    @Autowired
    public FollowController(FollowService followService, HostHolder hostHolder, UserService userService, EventProducer eventProducer, LikeService likeService) {
        this.followService = followService;
        this.hostHolder = hostHolder;
        this.userService = userService;
        this.eventProducer = eventProducer;
        this.likeService = likeService;
    }

    @PostMapping("/follow")
    @ResponseBody
    @LoginRequired
    public String follow(int entityType, int entityId) {
        User user = hostHolder.getUser();
        followService.follow(user.getId(),entityType,entityId);

        //触发关注事件
        Event event = new Event()
                .setTopic(TOPIC_FOLLOW)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(entityType)
                .setEntityId(entityId)
                .setEntityUserId(entityId);
        eventProducer.fireEvent(event);



        return CommunityUtil.getJSONString(0, "已关注!");
    }

    @PostMapping("/unfollow")
    @ResponseBody
    @LoginRequired
    public String unfollow(int entityType, int entityId) {
        User user = hostHolder.getUser();
        followService.unfollow(user.getId(),entityType,entityId);
        return CommunityUtil.getJSONString(0, "已取消关注!");
    }


    @GetMapping("/collection/{userId}")
    public String getCollection(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }
        model.addAttribute("user",user);
        page.setLimit(12);
        page.setPath("/collection/" + userId);
        page.setRows((int) followService.findFolloweeCount(userId, ENTITY_TYPE_DISCUSS));

        List<Map<String, Object>> discussList = followService.findCollection(userId, page.getOffset(), page.getLimit());
        if (discussList != null) {
            for (Map<String, Object> map : discussList) {
                DiscussPost discussPost = (DiscussPost) map.get("post");
                map.put("hadFollowed", followService.hasFollowed(userId,ENTITY_TYPE_DISCUSS,discussPost.getId()));
                long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPost.getId());
                map.put("likeCount", likeCount);
            }
        }
        model.addAttribute("posts",discussList);
        //关注数量
        long followeeCount =  followService.findFolloweeCount(userId, ENTITY_TYPE_DISCUSS);
        model.addAttribute("followeeCount", followeeCount);
        return "/html/user/collect";

    }




    @GetMapping("/followee/{userId}")
    public String getFollowee(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }
        model.addAttribute("user",user);
        page.setLimit(5);
        page.setPath("/followee/" + userId);
        page.setRows((int) followService.findFolloweeCount(userId, ENTITY_TYPE_USER));

        List<Map<String, Object>> userList = followService.findFollowee(userId, page.getOffset(), page.getLimit());
        if (userList != null) {
            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");
                map.put("hadFollowed", hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users",userList);
        //关注数量
        long followeeCount =  followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);
        return "/html/user/followee";

    }


    @GetMapping("/followers/{userId}")
    public String getFollowers(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }

        model.addAttribute("user",user);
        page.setLimit(5);
        page.setPath("/followers/" + userId);
        page.setRows((int) followService.findFollowerCount(CommunityConstant.ENTITY_TYPE_USER, userId));

        List<Map<String, Object>> userList = followService.findFollowers(userId, page.getOffset(), page.getLimit());
        if (userList != null) {
            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");
                map.put("hadFollowed", hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users",userList);
        //粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);

        return "/html/user/follower";

    }


    private boolean hasFollowed(int userId) {
        if (hostHolder.getUser() == null) {
            return false;
        }
        return followService.hasFollowed(hostHolder.getUser().getId(), CommunityConstant.ENTITY_TYPE_USER, userId);
    }



}
