package com.catowl.sts.mapper;

import com.catowl.sts.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthMapper {
    User selectUser(@Param("id")Long id);
    User findByUsername(@Param("username") String username);
    int insertUser(User user);
    int updateUser(User user);
    User getinfobyid(@Param("id")Long id);
    int deleteUser(@Param("id")Long id);
}
