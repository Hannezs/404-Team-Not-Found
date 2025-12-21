package com.catowl.sts.service.impl;

import com.catowl.sts.exception.UnauthorizedException;
import com.catowl.sts.mapper.AuthMapper;
import com.catowl.sts.model.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MyUserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    private final AuthMapper userMapper;

    public MyUserDetailsServiceImpl(AuthMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new UnauthorizedException("用户不存在:");
        }
        return user;
    }
}
