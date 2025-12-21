package com.catowl.sts.service;

import com.catowl.sts.model.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    User selectUser();
    int updateUserInfo(User user);
    User getInfoById(Long id);
    String login(User user);
    void logout();
    int insertUser(User user);
    String handleAvatarUpload(MultipartFile file);
    String generateFileName(MultipartFile file);
    void deleteOldAvatar(String oldAvatarUrl);
    int deleteUser();
}
