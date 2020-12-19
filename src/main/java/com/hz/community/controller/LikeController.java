package com.hz.community.controller;

import com.hz.community.entity.Event;
import com.hz.community.entity.User;
import com.hz.community.event.EventProducer;
import com.hz.community.service.LikeService;
import com.hz.community.util.CommunityConstant;
import com.hz.community.util.CommunityUtil;
import com.hz.community.util.HostHolder;
import com.hz.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class LikeController  implements CommunityConstant {

    private LikeService likeService;

    private HostHolder hostHolder;

    private EventProducer eventProducer;

    private RedisTemplate redisTemplate;

    @Autowired
    public LikeController(LikeService likeService, HostHolder hostHolder, EventProducer eventProducer, RedisTemplate redisTemplate) {
        this.likeService = likeService;
        this.hostHolder = hostHolder;
        this.eventProducer = eventProducer;
        this.redisTemplate = redisTemplate;
    }

    //点赞
    @PostMapping("/like")
    @ResponseBody
    public String like(int entityType, int entityId, int entityUserId, int postId) {
        User user = hostHolder.getUser();
        //点赞
        likeService.like(user.getId(), entityType, entityId, entityUserId);
        //数量
        long likeCount = likeService.findEntityLikeCount(entityType, entityId);
        //状态
        int likeStatus = likeService.findEntityLikeStatus(user.getId(), entityType, entityId);
        //返回结果
        Map<String, Object> map = new HashMap<>();
        map.put("likeCount", likeCount);
        map.put("likeStatus", likeStatus);

        //触发点赞事件
        if (likeStatus == 1) {
            Event event = new Event()
                    .setTopic(TOPIC_LIKE)
                    .setUserId(hostHolder.getUser().getId())
                    .setEntityType(entityType)
                    .setEntityId(entityId)
                    .setEntityUserId(entityUserId)
                    .setData("postId", postId);
            eventProducer.fireEvent(event);
        }
        if (entityType == ENTITY_TYPE_POST) {
            //计算帖子分数
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey, postId);
        }
        return CommunityUtil.getJSONString(0, null, map);

    }
}
