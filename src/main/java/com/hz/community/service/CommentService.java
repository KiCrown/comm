package com.hz.community.service;

import com.hz.community.dao.CommentMapper;
import com.hz.community.entity.Comment;
import com.hz.community.util.CommunityConstant;
import com.hz.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class CommentService implements CommunityConstant {

    private CommentMapper commentMapper;

    private SensitiveFilter sensitiveFilter;

    private DiscussPostService discussPostService;

    @Autowired
    public CommentService(CommentMapper commentMapper,SensitiveFilter sensitiveFilter,DiscussPostService discussPostService){
        this.commentMapper = commentMapper;
        this.sensitiveFilter = sensitiveFilter;
        this.discussPostService =  discussPostService;
    }

    public List<Comment> findCommentsByEntity(int entityType,int entityId,int offset,int limit){
        return commentMapper.selectCommentsByEntity(entityType,entityId,offset,limit);
    }

    public Comment findCommentById(int id) {
        return commentMapper.selectCommentById(id);
    }

    public int findCommentCount(int entityType,int entityId){
        return commentMapper.selectCommentCount(entityType,entityId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED,propagation = Propagation.REQUIRED)
    public int addComment(Comment comment){
        if(comment == null){
            throw new IllegalArgumentException("参数不能为空!");
        }
        //添加评论
        comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
        comment.setContent(sensitiveFilter.filter(comment.getContent()));
        int rows = commentMapper.insertComment(comment);
        //更新帖子评论数量
        if(comment.getEntityType() == ENTITY_TYPE_POST){
            int count = commentMapper.selectCommentCount(comment.getEntityType(), comment.getEntityId());
            discussPostService.updateCommentCount(comment.getEntityId(),count);
        }

        return rows;

    }

    public List<Comment> findCommentsByUserId(int userId, int offset, int limit) {
        return commentMapper.selectCommentsByUserId(userId, offset, limit);
    }

    public int findCommentCountOfUser(int userId) {
        return commentMapper.selectCommentCountOfUser(userId);
    }
}