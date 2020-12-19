package com.hz.community.controller;

import com.hz.community.entity.DiscussPost;
import com.hz.community.entity.Page;
import com.hz.community.service.*;
import com.hz.community.util.CommunityConstant;
import com.hz.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.*;

@Controller
public class SearchController implements CommunityConstant {

    private ElasticsearchService elasticsearchService;

    private UserService userService;

    private LikeService likeService;

    private DiscussPostService discussPostService;

    private DataService dataService;

    private HostHolder hostHolder;

    @Autowired
    public SearchController(ElasticsearchService elasticsearchService, UserService userService, LikeService likeService, DiscussPostService discussPostService, DataService dataService, HostHolder hostHolder) {
        this.elasticsearchService = elasticsearchService;
        this.userService = userService;
        this.likeService = likeService;
        this.discussPostService = discussPostService;
        this.dataService = dataService;
        this.hostHolder = hostHolder;
    }

    @GetMapping("/search")
    public String search(String keyword, Page page, Model model) {
        //搜索帖子
        org.springframework.data.domain.Page<DiscussPost> searchResult = elasticsearchService.searchDiscussPost(keyword, page.getCurrent() - 1, page.getLimit());
        //聚合数据
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if (searchResult != null) {
            for (DiscussPost post : searchResult) {
                Map<String, Object> map = new HashMap<>();
                //帖子
                map.put("post", post);
                //作者
                map.put("user", userService.findUserById(post.getUserId()));
                //点赞数量
                map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));
                discussPosts.add(map);
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
        model.addAttribute("keyword", keyword);
        //分页信息
        page.setPath("/search?keyword=" + keyword);
        page.setRows(searchResult == null ? 0 : (int) searchResult.getTotalElements());

        return "/html/search";
    }


    @GetMapping("/tagpage")
    public String getTagPage(String tag, Model model, Page page) {
        page.setRows(discussPostService.findDiscussPostRowsOfTag(0, tag));
        page.setPath("/tagpage?tag="+tag);
        List<DiscussPost> list = discussPostService.findDiscussPostsByTag(0, tag, 1, page.getOffset(), page.getLimit());
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if (list != null) {
            for (DiscussPost post : list) {
                Map<String, Object> map = new HashMap<>();
                //帖子
                map.put("post", post);
                //作者
                map.put("user", userService.findUserById(post.getUserId()));
                //点赞数量
                map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));
                discussPosts.add(map);
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
        return "/html/search";
    }
}
