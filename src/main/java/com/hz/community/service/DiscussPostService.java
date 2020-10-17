package com.hz.community.service;

import com.hz.community.dao.DiscussPostMapper;
import com.hz.community.entity.DiscussPost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiscussPostService {


    private  DiscussPostMapper discussPostMapper;

    @Autowired
    public DiscussPostService(DiscussPostMapper discussPostMapper){
        this.discussPostMapper = discussPostMapper;
    }

    public List<DiscussPost> findDiscussPosts(int useId,int offset,int limit){
        return  discussPostMapper.selectDiscussPosts(useId,offset,limit);
    }

    public int findDiscussPostRows(int userId){
        return discussPostMapper.selectDiscussPostRows(userId);
    }
}
