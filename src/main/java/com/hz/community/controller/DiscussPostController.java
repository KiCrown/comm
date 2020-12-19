package com.hz.community.controller;

import com.hz.community.annotation.LoginRequired;
import com.hz.community.entity.*;
import com.hz.community.event.EventProducer;
import com.hz.community.service.*;
import com.hz.community.util.CommunityConstant;
import com.hz.community.util.CommunityUtil;
import com.hz.community.util.HostHolder;
import com.hz.community.util.RedisKeyUtil;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements CommunityConstant {

    private DiscussPostService discussPostService;

    private HostHolder hostHolder;

    private UserService userService;

    private CommentService commentService;

    private LikeService likeService;

    private FollowService followService;

    private EventProducer eventProducer;

    private RedisTemplate redisTemplate;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.header.name}")
    private String headerBucketName;

    @Value("${qiniu.bucket.header.url}")
    private String headerBucketUrl;

    @Autowired
    public DiscussPostController(DiscussPostService discussPostService, HostHolder hostHolder, UserService userService, CommentService commentService, LikeService likeService, EventProducer eventProducer, RedisTemplate redisTemplate, FollowService followService) {
        this.discussPostService = discussPostService;
        this.hostHolder = hostHolder;
        this.userService = userService;
        this.commentService = commentService;
        this.likeService = likeService;
        this.eventProducer = eventProducer;
        this.redisTemplate = redisTemplate;
        this.followService = followService;
    }

    //获取帖子发布页面
    @GetMapping("/addPage")
    public String getDiscussPostAddPage(Model model){
        //上传消息
        uploadInformation(model);
        return "/html/jie/add";
    }

    @GetMapping("/updatePage/{discussPostId}")
    public String getDiscussPostPage(@PathVariable("discussPostId") int discussPostId, Model model) {
        DiscussPost discussPost = discussPostService.findDiscussPostById(discussPostId);
        model.addAttribute("post",discussPost);
        //上传消息
        uploadInformation(model);

        return "/html/jie/update";
    }

    //发布帖子
    @PostMapping("/add")
    @ResponseBody
    public String addDiscussPost(String title,String content,String tip,String tag){
        User user = hostHolder.getUser();
        if(user == null){
            return CommunityUtil.getJSONString(403,"你还没有登录");
        }
        DiscussPost discussPost = new DiscussPost();
        discussPost.setUserId(user.getId());
        discussPost.setTitle(title);
        discussPost.setContent(content);
        discussPost.setTip(Integer.valueOf(tip));
        discussPost.setTag(tag);
        discussPost.setCreateTime(new Date());

        discussPostService.addDiscussPost(discussPost);

        userService.updateExperience(user.getId(),user.getExperience()+100);

        //触发发帖事件
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(user.getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(discussPost.getId());
        eventProducer.fireEvent(event);
        //计算帖子分数
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, discussPost.getId());

        return CommunityUtil.getJSONString(0,"发布成功!");
    }

    //修改帖子
    @PostMapping("/update")
    @ResponseBody
    public String updateDiscussPost(int discussPostId, String title, String content, String tip, String tag) {
        User user = hostHolder.getUser();
        if (user == null) {
            return CommunityUtil.getJSONString(403, "你还没有登录");
        }
        DiscussPost discussPost = discussPostService.findDiscussPostById(discussPostId);
        if (discussPost == null) {
            return CommunityUtil.getJSONString(1, "帖子不存在");
        }
        discussPost.setTitle(title);
        discussPost.setContent(content);
        discussPost.setTip(Integer.valueOf(tip));
        discussPost.setTag(tag);
        discussPost.setCreateTime(new Date());

        discussPostService.updateDiscussPost(discussPost);

        //触发修改事件
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(user.getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(discussPost.getId());
        eventProducer.fireEvent(event);
        //计算帖子分数
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, discussPost.getId());

        return CommunityUtil.getJSONString(0, "修改成功!");
    }

    //获取帖子详情
    @GetMapping("/detail/{discussPostId}")
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model, Page page){
        //帖子详情
        DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
        model.addAttribute("post",post);
        //帖子标签
        String[] tags = discussPostService.getDiscussPostTag(post);
        model.addAttribute("tags",tags);
        //相关帖子
        List<DiscussPost> relateList = discussPostService.getRelatedDiscussPost(post.getId(), tags);
        //上传消息
        uploadInformation(model);

        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if(relateList != null){
            for (DiscussPost discussPost : relateList) {
                Map<String, Object> map = new HashMap<>();
                map.put("post", discussPost);
                discussPosts.add(map);
            }
        }
        model.addAttribute("relate",discussPosts);
        //作者
        User user = userService.findUserById(post.getUserId());
        model.addAttribute("user",user);
        //点赞数量
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST,discussPostId);
        model.addAttribute("likeCount", likeCount);
        //点赞状态
        int likeStatus = hostHolder.getUser() == null ? 0 : likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeStatus", likeStatus);
        //收藏状态
        boolean collectStatus = hostHolder.getUser() == null ? false : followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_DISCUSS, discussPostId);
        model.addAttribute("collectStatus", collectStatus);
        //分页信息
        page.setLimit(5);
        page.setPath("/discuss/detail/" + discussPostId);
        page.setRows(post.getCommentCount());
        //评论列表
        List<Comment> commentList = commentService.findCommentsByEntity(ENTITY_TYPE_POST, post.getId(), page.getOffset(), page.getLimit());
        List<Map<String,Object>> commentVoList = new ArrayList<>();
        if(commentList != null){
            for (Comment comment : commentList){
                //评论VO
                Map<String,Object> commentVo = new HashMap<>();
                //评论
                commentVo.put("comment",comment);
                //作者
                commentVo.put("user",userService.findUserById(comment.getUserId()));
                //点赞数量
                likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT,comment.getId());
                commentVo.put("likeCount", likeCount);
                //点赞状态
                likeStatus = hostHolder.getUser() == null ? 0 : likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeStatus", likeStatus);
                //评论回复列表
                List<Comment> replyList = commentService.findCommentsByEntity(ENTITY_TYPE_COMMENT,comment.getId(),0,Integer.MAX_VALUE);
                List<Map<String,Object>> replyVoList = new ArrayList<>();
                if(replyList != null){
                    for (Comment reply : replyList){
                        Map<String,Object> replyVo = new HashMap<>();
                        //回复
                        replyVo.put("reply", reply);
                        //作者
                        replyVo.put("user",userService.findUserById(reply.getUserId()));
                        //回复目标
                        User target = reply.getTargetId() == 0 ? null : userService.findUserById(reply.getTargetId());
                        replyVo.put("target",target);
                        //点赞数量
                        likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT,reply.getId());
                        replyVo.put("likeCount", likeCount);
                        //点赞状态
                        likeStatus = hostHolder.getUser() == null ? 0 : likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeStatus", likeStatus);
                        replyVoList.add(replyVo);
                    }
                }
                commentVo.put("replies",replyVoList);
                //回复数量
                int replyCount = commentService.findCommentCount(ENTITY_TYPE_COMMENT,comment.getId());
                commentVo.put("replyCount",replyCount);
                commentVoList.add(commentVo);
            }
        }

        model.addAttribute("comments",commentVoList);

        return "/html/jie/detail";
    }

    //置顶
    @PostMapping("/top")
    @ResponseBody
    public String setTop(int id, int rank) {
        if (rank == 1) {
            discussPostService.updateType(id, 1);
        } else {
            discussPostService.updateType(id, 0);
        }

        //触发发帖事件
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);
        return CommunityUtil.getJSONString(0);
    }

    //加精
    @PostMapping("/wonderful")
    @ResponseBody
    public String setWonderful(int id, int rank) {
        if (rank == 1) {
            discussPostService.updateStatus(id, 1);
        } else {
            discussPostService.updateStatus(id, 0);
        }

        //触发发帖事件
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        //计算帖子分数
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, id);

        return CommunityUtil.getJSONString(0);
    }
    //拉黑
    @PostMapping("/delete")
    @ResponseBody
    public String setDelete(int id) {
        discussPostService.updateStatus(id, 2);
        //触发删帖事件
        Event event = new Event()
                .setTopic(TOPIC_DELETE)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);
        return CommunityUtil.getJSONString(0);
    }

    //上传云服务器处理
    private void uploadInformation(Model model){
        //上传文件名称
        String fileName = CommunityUtil.generateUUID();
        //设置响应信息
        StringMap policy = new StringMap();
        String url = headerBucketUrl + "/" + fileName;
        Map<String, Object> map = new HashMap<>();
        map.put("url",url);
        policy.put("returnBody", CommunityUtil.getJSONString(0,"上传成功",map));
        //生成上传凭证
        Auth auth = Auth.create(accessKey, secretKey);
        String uploadToken = auth.uploadToken(headerBucketName, fileName, 3600, policy);
        model.addAttribute("uploadToken", uploadToken);
        model.addAttribute("fileName", fileName);
    }

}
