package com.hz.community.controller;

import com.hz.community.annotation.LoginRequired;
import com.hz.community.entity.Comment;
import com.hz.community.entity.DiscussPost;
import com.hz.community.entity.Page;
import com.hz.community.entity.User;
import com.hz.community.service.*;
import com.hz.community.util.CommunityConstant;
import com.hz.community.util.CommunityUtil;
import com.hz.community.util.HostHolder;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private UserService userService;

    private HostHolder hostHolder;

    private LikeService likeService;

    private FollowService followService;

    private DiscussPostService discussPostService;

    private CommentService commentService;

    private DataService dataService;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.header.name}")
    private String headerBucketName;

    @Value("${qiniu.bucket.header.url}")
    private String headerBucketUrl;

    @Autowired
    public UserController(UserService userService, HostHolder hostHolder, LikeService likeService, FollowService followService, DiscussPostService discussPostService, CommentService commentService, DataService dataService) {
        this.userService = userService;
        this.hostHolder = hostHolder;
        this.likeService = likeService;
        this.followService = followService;
        this.discussPostService = discussPostService;
        this.commentService = commentService;
        this.dataService = dataService;
    }

    @Value("${community.path.domain}")
    private String domian;

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${server.servlet.context-path}")
    private String contextPath;


    @LoginRequired
    @GetMapping("/setting/{userId}")
    public String getSettingPage(@PathVariable("userId") int userId, Model model) {
        //上传文件名称
        String fileName = CommunityUtil.generateUUID();
        //设置响应信息
        String url = headerBucketUrl + "/" + fileName;
        StringMap policy = new StringMap();
        policy.put("returnBody", CommunityUtil.getJSONString(0, "上传成功"));

        //生成上传凭证
        Auth auth = Auth.create(accessKey, secretKey);
        String uploadToken = auth.uploadToken(headerBucketName, fileName, 3600, policy);
        model.addAttribute("uploadToken", uploadToken);
        model.addAttribute("fileName", fileName);
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }
        //用户
        model.addAttribute("user", user);
        return "/html/user/set";
    }

    //更新头像路径
    @PostMapping("/header/url")
    @ResponseBody
    public String updateHeaderUrl(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return CommunityUtil.getJSONString(1, "文件名不能为空!");
        }
        String url = headerBucketUrl + "/" + fileName;
        userService.updateHeader(hostHolder.getUser().getId(), url);
        return CommunityUtil.getJSONString(0);
    }

    //修改个人信息
    @PostMapping("/information")
    @ResponseBody
    public String updateUserInformation(int sex, String signature, String email, String location) {


        userService.updateInformation(hostHolder.getUser().getId(), sex, signature, email, location);

        return CommunityUtil.getJSONString(0, "修改成功!");
    }


    //更改密码
    @PostMapping("/changePwd")
    @ResponseBody
    public String changePassword(String oldPassword,String newPassword){
        User user = hostHolder.getUser();
        //空值处理
        if(StringUtils.isBlank(oldPassword)){
            return CommunityUtil.getJSONString(1, "原密码不能为空!");
        }
        if(StringUtils.isBlank(newPassword)){
            return CommunityUtil.getJSONString(2, "新密码不能为空!");
        }
        //验证密码
        oldPassword = CommunityUtil.md5(oldPassword + user.getSalt());
        if(!user.getPassword().equals(oldPassword)){
            return CommunityUtil.getJSONString(1, "原密码不正确!");
        }
        newPassword = CommunityUtil.md5(newPassword + user.getSalt());
        userService.changePassword(user.getId(),newPassword);
        return CommunityUtil.getJSONString(0,"密码修改成功");
    }

    //个人主页
    @GetMapping("/profile/{userId}")
    public String getProfilePage(@PathVariable("userId") int userId, Model model, Page page, @RequestParam(name = "orderMode", defaultValue = "0") int orderMode) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }
        //用户

        model.addAttribute("user", user);
/*      //点赞数量
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);
        //关注数量
        long followeeCount =  followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);
        //粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);*/
        //是否已关注
        boolean hasFollowed = false;
        if (hostHolder.getUser() != null) {
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);

        List<DiscussPost> list = discussPostService.findDiscussPosts(userId, 0, 12, orderMode);
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        int count = 0;
        if (list != null) {
            for (DiscussPost post : list) {
                Map<String, Object> map = new HashMap<>();
                if(!isToday(post.getCreateTime().getTime(),new Date().getTime())){
                    continue;
                }
                map.put("post", post);
                long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
                map.put("likeCount", likeCount);
                discussPosts.add(map);
                ++count;
            }
        }
        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("postCount", count);

        List<Comment> commentList = commentService.findCommentsByUserId(userId, 0, 6);
        List<Map<String, Object>> comments = new ArrayList<>();
        count = 0;
        if (commentList != null) {
            for (Comment comment : commentList) {
                Map<String, Object> map = new HashMap<>();
                if(!isToday(comment.getCreateTime().getTime() ,new Date().getTime())){
                    continue;
                }
                DiscussPost discussPost = null;
                if (comment.getEntityType() == 1) {
                    discussPost = discussPostService.findDiscussPostById(comment.getEntityId());
                }else {
                    discussPost = discussPostService.findDiscussPostById(commentService.findCommentById(comment.getEntityId()).getEntityId());
                }
                map.put("discussPost", discussPost);
                map.put("comment", comment);
                ++count;
                comments.add(map);
            }
        }
        model.addAttribute("comments", comments);
        model.addAttribute("commentCount", count);
        return "html/user/home";
    }

    //签到
    @PostMapping("/sign")
    @ResponseBody
    public String sign() {
        User user = hostHolder.getUser();
        if (user == null) {
            return CommunityUtil.getJSONString(1, "签到失败，请先登录!");
        }
        dataService.recordSign(user.getId(), new Date());
        userService.updateExperience(user.getId(), user.getExperience() + 10);
        return CommunityUtil.getJSONString(0);

    }

    //个人帖子
    @GetMapping("/mypost/{userId}")
    public String getMyPostPage(@PathVariable("userId") int userId, Model model, Page page, @RequestParam(name = "orderMode", defaultValue = "0") int orderMode) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }
        model.addAttribute("user", user);
        page.setLimit(12);
        page.setRows(discussPostService.findDiscussPostRows(userId));
        page.setPath("/user/mypost/" + userId);
        model.addAttribute("count", discussPostService.findDiscussPostRows(userId));
        List<DiscussPost> list = discussPostService.findDiscussPosts(userId, page.getOffset(), page.getLimit(), orderMode);

        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if (list != null) {
            for (DiscussPost post : list) {
                Map<String, Object> map = new HashMap<>();
                map.put("post", post);
                long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
                map.put("likeCount", likeCount);
                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts", discussPosts);

        return "/html/user/index";
    }

    //个人评论
    @GetMapping("/myreply/{userId}")
    public String getMyReplyPage(@PathVariable("userId") int userId, Model model, Page page, @RequestParam(name = "orderMode", defaultValue = "0") int orderMode) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }
        model.addAttribute("user", user);
        page.setLimit(5);
        page.setRows(commentService.findCommentCountOfUser(userId));
        page.setPath("/user/myreply/"+userId);
        List<Comment> list = commentService.findCommentsByUserId(userId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> comments = new ArrayList<>();
        if (list != null) {
            for (Comment comment : list) {
                Map<String, Object> map = new HashMap<>();
                DiscussPost discussPost = null;
                if (comment.getEntityType() == 1) {
                    discussPost = discussPostService.findDiscussPostById(comment.getEntityId());
                }else {
                    discussPost = discussPostService.findDiscussPostById(commentService.findCommentById(comment.getEntityId()).getEntityId());
                }
                map.put("post", discussPost);
                map.put("comment", comment);
                comments.add(map);
            }
        }
        model.addAttribute("comments", comments);

        return "/html/user/index";
    }



    //更改头像(废弃)
   /* @LoginRequired
    @PostMapping("/upload")
    @ResponseBody
    public String uploadHeader(MultipartFile headerImage, Model model) {
        if (headerImage == null) {
            *//*model.addAttribute("error", "您还没有选择图片!");*//*
            return CommunityUtil.getJSONString(1, "文件名不能为空!");
        }
        String fileName = headerImage.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        if (StringUtils.isBlank(suffix)) {
            *//*model.addAttribute("error", "文件的格式不正确!");*//*
            return CommunityUtil.getJSONString(1, "文件的格式不正确!");
        }
        //生成随机名字
        fileName = CommunityUtil.generateUUID() + suffix;
        //确定文件路径
        File dest = new File(uploadPath + "/" + fileName);
        try {
            //存储文件
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败:" + e.getMessage());
            throw new RuntimeException("上传文件失败,服务器发生异常!", e);
        }

        User user = hostHolder.getUser();
        String headerUrl = domian + contextPath + "/user/header/" + fileName;
        userService.updateHeader(user.getId(), headerUrl);

        return CommunityUtil.getJSONString(0);
    }*/
    //获取头像(废弃)
/*
    @GetMapping("/header/{fileName}")
    public void getHeader(@PathVariable("fileName")String fileName, HttpServletResponse response){
        //服务器存放路径
        fileName = uploadPath + "/" + fileName;
        //文件后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        //响应数据
        response.setContentType("images/" + suffix);
        try (
            FileInputStream fis = new FileInputStream(fileName);
            OutputStream os = response.getOutputStream();
        ){

            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b = fis.read(buffer)) != -1){
                os.write(buffer,0,b);
            }
        } catch (IOException e) {
            logger.error("读取头像失败:"+ e.getMessage());
        }
    }

*/





    private boolean isToday(long date,long today) {
        Calendar instance = Calendar.getInstance();
        instance.setTimeInMillis(date);
        int d1 = instance.get(Calendar.DAY_OF_YEAR);
        instance.setTimeInMillis(today);
        int d2 = instance.get(Calendar.DAY_OF_YEAR);
        return d1 == d2;
    }
}
