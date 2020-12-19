package com.hz.community.controller;

import com.hz.community.annotation.LoginRequired;
import com.hz.community.entity.Comment;
import com.hz.community.entity.DiscussPost;
import com.hz.community.entity.Event;
import com.hz.community.entity.User;
import com.hz.community.event.EventProducer;
import com.hz.community.service.CommentService;
import com.hz.community.service.DiscussPostService;
import com.hz.community.service.UserService;
import com.hz.community.util.CommunityConstant;
import com.hz.community.util.HostHolder;
import com.hz.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.Date;

@Controller
@RequestMapping("/comment")
public class CommentController implements CommunityConstant {

    private CommentService commentService;

    private HostHolder hostHolder;

    private EventProducer eventProducer;

    private DiscussPostService discussPostService;

    private UserService userService;

    private RedisTemplate redisTemplate;

    @Autowired
    public CommentController(CommentService commentService, HostHolder hostHolder, EventProducer eventProducer, DiscussPostService discussPostService, RedisTemplate redisTemplate, UserService userService) {
        this.commentService = commentService;
        this.hostHolder = hostHolder;
        this.eventProducer = eventProducer;
        this.discussPostService = discussPostService;
        this.redisTemplate = redisTemplate;
        this.userService = userService;
    }

    @LoginRequired
    @PostMapping("/add/{discussPostId}")
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment){
        comment.setUserId(hostHolder.getUser().getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        commentService.addComment(comment);

        User user = hostHolder.getUser();

        userService.updateExperience(user.getId(),user.getExperience()+50);

        //触发评论事件
        Event event = new Event()
                .setTopic(TOPIC_COMMENT)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(comment.getEntityType())
                .setEntityId(comment.getEntityId())
                .setData("postId", discussPostId);
        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        } else if (comment.getEntityType() == ENTITY_TYPE_COMMENT) {
            Comment target = commentService.findCommentById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }
        eventProducer.fireEvent(event);

        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            //触发发帖事件
            event = new Event()
                    .setTopic(TOPIC_PUBLISH)
                    .setUserId(comment.getId())
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(discussPostId);

            eventProducer.fireEvent(event);
            //计算帖子分数
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey, discussPostId);
        }

        return "redirect:/discuss/detail/" + discussPostId;
    }

}
