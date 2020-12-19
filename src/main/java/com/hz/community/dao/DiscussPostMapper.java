package com.hz.community.dao;

import com.hz.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {

    List<DiscussPost> selectDiscussPosts(int userId,int offset,int limit,int orderMode);

    List<DiscussPost> selectDiscussPostsByStatus(int userId, int offset, int limit, int orderMode, int status);

    List<DiscussPost> selectDiscussPostsByType(int userId, int type ,int tip);

    List<DiscussPost> selectDiscussPostsByTip(int userId, int offset, int limit, int orderMode, int tip, int status);

    List<DiscussPost> selectDiscussPostsByTag(int discussId, String tag,int type,int offset,int limit);

    int selectDiscussPostRows(@Param("userId") int userId);

    int selectDiscussPostRowsOfTag(@Param("userId") int userId,String tag);

    int selectDiscussPostRowsOfStatus(@Param("userId") int userId, int status);

    int selectDiscussPostRowsOfTip(@Param("userId") int userId, int tip, int status);

    int insertDiscussPost(DiscussPost discussPost);

    int updateDiscussPost(DiscussPost discussPost);

    DiscussPost selectDiscussPostById(int id);

    int updateCommentCount(int id,int commentCount);

    int updateType(int id, int type);

    int updateStatus(int id, int status);

    int updateScore(int id, double score);


}
