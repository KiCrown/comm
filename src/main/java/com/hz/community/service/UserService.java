package com.hz.community.service;

import com.hz.community.dao.UserMapper;
import com.hz.community.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private UserMapper userMapper;

    @Autowired
    public  UserService(UserMapper userMapper){
        this.userMapper = userMapper;
    }

    public User findUserById(int id){
        return userMapper.selectById(id);
    }
}
