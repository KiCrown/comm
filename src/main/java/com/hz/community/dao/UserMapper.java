package com.hz.community.dao;

import com.hz.community.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

    User selectById(int id);

    User selectByName(String username);

    User selectByEmail(String email);

    int insertUser(User user);

    int updateStatus(int id,int status);

    int updateHeader(int id,String headerUrl);

    int updatePassword(int id,String password);

    void updateInformation(int id, int sex, String signature, String email,String location);

    void updateExperience(int id, int experience);
}
