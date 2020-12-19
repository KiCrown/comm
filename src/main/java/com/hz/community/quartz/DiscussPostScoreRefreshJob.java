package com.hz.community.quartz;


import com.hz.community.entity.DiscussPost;
import com.hz.community.service.DiscussPostService;
import com.hz.community.service.ElasticsearchService;
import com.hz.community.service.LikeService;
import com.hz.community.util.CommunityConstant;
import com.hz.community.util.RedisKeyUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class DiscussPostScoreRefreshJob implements Job, CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(DiscussPostScoreRefreshJob.class);

    private RedisTemplate redisTemplate;

    private DiscussPostService discussPostService;

    private LikeService likeService;

    private ElasticsearchService elasticsearchService;

    private static final Date epoch;

    static {
        try {
            epoch = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2020-11-07 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException("初始化失败!", e);
        }
    }

    @Autowired
    public DiscussPostScoreRefreshJob(RedisTemplate redisTemplate, DiscussPostService discussPostService, LikeService likeService, ElasticsearchService elasticsearchService) {
        this.redisTemplate = redisTemplate;
        this.discussPostService = discussPostService;
        this.likeService = likeService;
        this.elasticsearchService = elasticsearchService;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String redisKey = RedisKeyUtil.getPostScoreKey();
        BoundSetOperations operations = redisTemplate.boundSetOps(redisKey);
        if (operations.size() == 0) {
            logger.info("[任务取消]没有需要刷新的帖子!");
            return;
        }

        logger.info("[任务开始]正在刷新帖子分数:" + operations.size());
        while (operations.size() > 0) {
            this.refresh((Integer) operations.pop());
        }
        logger.info("[任务结束]帖子分数刷新完毕!");

    }

    private void refresh(int postId) {
        DiscussPost post = discussPostService.findDiscussPostById(postId);
        if (post == null) {
            logger.error("该帖子不存在:id=" + postId);
            return;
        }
        //是否精华
        boolean wonderful = post.getId() == 1;
        //评论数量
        int commentCount = post.getCommentCount();
        //点赞数量
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, postId);
        //计算权重
        double w = (wonderful ? 75 : 0) + commentCount * 10 + likeCount * 2;
        //分数=帖子权重+距离天数
        double score = Math.log10(Math.max(w, 1)) + (post.getCreateTime().getTime() - epoch.getTime()) / (1000 * 3600 * 24);
        //更新帖子分数
        discussPostService.updateScore(postId, score);
        //同步搜索数据
        post.setScore(score);
        elasticsearchService.saveDiscussPost(post);
    }



}
