package com.hz.community.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.hz.community.dao.DiscussPostMapper;
import com.hz.community.entity.DiscussPost;
import com.hz.community.util.SensitiveFilter;
import org.apache.ibatis.annotations.Param;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostService {

    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);

    private  DiscussPostMapper discussPostMapper;

    private SensitiveFilter sensitiveFilter;

    @Value("${caffeine.posts.max-size}")
    private int maxSize;

    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;

    //帖子列表缓存
    private LoadingCache<String, List<DiscussPost>> postListCache;

    //帖子总数缓存
    private LoadingCache<Integer, Integer> postRowsCache;

    @PostConstruct
    public void init() {
        //初始化帖子列表缓存
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    @Nullable
                    @Override
                    public List<DiscussPost> load(@NonNull String key) throws Exception {
                        if (key == null || key.length() == 0) {
                            throw new IllegalArgumentException("参数错误!");
                        }
                        String[] params = key.split(":");
                        if (params == null || params.length != 2) {
                            throw new IllegalArgumentException("参数错误!");
                        }
                        int offset = Integer.valueOf(params[0]);
                        int limit = Integer.valueOf(params[1]);
                        logger.debug("load post list from DB.");
                        return discussPostMapper.selectDiscussPosts(0, offset, limit, 1);
                    }
                });
        //初始化帖子总数缓存
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Nullable
                    @Override
                    public Integer load(@NonNull Integer key) throws Exception {
                        logger.debug("load post rows from DB.");
                        return discussPostMapper.selectDiscussPostRows(key);
                    }
                });
    }

    @Autowired
    public DiscussPostService(DiscussPostMapper discussPostMapper, SensitiveFilter sensitiveFilter) {
        this.discussPostMapper = discussPostMapper;
        this.sensitiveFilter = sensitiveFilter;
    }

    public List<DiscussPost> findDiscussPosts(int userId,int offset,int limit,int orderMode){
        if (userId == 0 && orderMode == 1) {
            return postListCache.get(offset + ":" + limit);
        }
        return  discussPostMapper.selectDiscussPosts(userId,offset,limit,orderMode);
    }

    public List<DiscussPost> findDiscussPostsByStatus(int userId, int offset, int limit, int orderMode, int status) {
        return discussPostMapper.selectDiscussPostsByStatus(userId, offset, limit, orderMode, status);
    }

    public List<DiscussPost> findDiscussPostsByType(int userId, int type, int tip) {
        return discussPostMapper.selectDiscussPostsByType(userId, type, tip);
    }

    public List<DiscussPost> findDiscussPostsByTip(int userId, int offset, int limit, int orderMode, int tip, int status) {
        return discussPostMapper.selectDiscussPostsByTip(userId, offset, limit, orderMode, tip, status);
    }

    public List<DiscussPost> findDiscussPostsByTag(int discussId, String tag,int type,int offset,int limit) {
        return discussPostMapper.selectDiscussPostsByTag(discussId, tag, type, offset, limit);
    }

    public int findDiscussPostRows(int userId){
        if (userId == 0) {
            return postRowsCache.get(userId);
        }
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    public int findDiscussPostRowsOfStatus(int userId, int status) {
        return discussPostMapper.selectDiscussPostRowsOfStatus(userId, status);
    }

    public int findDiscussPostRowsOfTag(@Param("userId") int userId, String tag) {
        return discussPostMapper.selectDiscussPostRowsOfTag(userId, tag);
    }

    public int findDiscussPostRowsOfTip(int userId, int tip, int status) {
        return discussPostMapper.selectDiscussPostRowsOfTip(userId, tip, status);
    }

    public int addDiscussPost(DiscussPost discussPost){
        if(discussPost == null){
            throw new IllegalArgumentException("参数不能为空!");
        }
        //转义html标签
        discussPost.setTitle(HtmlUtils.htmlEscape(discussPost.getTitle()));
        discussPost.setContent(HtmlUtils.htmlEscape(discussPost.getContent()));
        //过滤敏感词
        discussPost.setTitle(sensitiveFilter.filter(discussPost.getTitle()));
        discussPost.setContent(sensitiveFilter.filter(discussPost.getContent()));
        return discussPostMapper.insertDiscussPost(discussPost);
    }

    public int updateDiscussPost(DiscussPost discussPost){
        if(discussPost == null){
            throw new IllegalArgumentException("参数不能为空!");
        }
        //转义html标签
        discussPost.setTitle(HtmlUtils.htmlEscape(discussPost.getTitle()));
        discussPost.setContent(HtmlUtils.htmlEscape(discussPost.getContent()));
        //过滤敏感词
        discussPost.setTitle(sensitiveFilter.filter(discussPost.getTitle()));
        discussPost.setContent(sensitiveFilter.filter(discussPost.getContent()));
        return discussPostMapper.updateDiscussPost(discussPost);
    }

    public DiscussPost findDiscussPostById(int id){
        return discussPostMapper.selectDiscussPostById(id);
    }

    public int updateCommentCount(int id, int commentCount){
        return discussPostMapper.updateCommentCount(id,commentCount);
    }

    public int updateType(int id, int type) {
        return discussPostMapper.updateType(id, type);
    }

    public int updateStatus(int id, int status) {
        return discussPostMapper.updateStatus(id, status);
    }

    public int updateScore(int id, double score) {
        return discussPostMapper.updateScore(id, score);
    }

    public String[] getDiscussPostTag(DiscussPost discussPost) {
        return discussPost.getTag().split(",");
    }

    public List<DiscussPost> getRelatedDiscussPost(int discussId, String[] tags) {
        List<DiscussPost> relateList = new ArrayList<>();
        for (String tag:tags){
            List<DiscussPost> relates = discussPostMapper.selectDiscussPostsByTag(discussId,tag,0,0,0);
            if (relates != null) {
                for (DiscussPost discussPost : relates) {
                    if(!relateList.contains(discussPost)){
                        relateList.add(discussPost);
                    }
                }
            }

        }
        return relateList;
    }

}
