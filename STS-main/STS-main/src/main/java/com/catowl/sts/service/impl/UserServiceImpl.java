package com.catowl.sts.service.impl;

import com.catowl.sts.exception.InternetServerException;
import com.catowl.sts.exception.UnauthorizedException;
import com.catowl.sts.mapper.AuthMapper;
import com.catowl.sts.model.entity.User;
import com.catowl.sts.service.UserService;
import com.catowl.sts.utils.JwtUtil;
import com.catowl.sts.utils.RedisCache;
import de.huxhorn.sulky.ulid.ULID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private AuthMapper userMapper;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${file.upload-dir}")
    private String uploadDir;//上传的目录

    @Autowired
    private RedisCache redisCache;

    private final WebClient webClient = WebClient.builder().build();

    public User selectUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User loginUser = (User) authentication.getPrincipal();
            Long userId = loginUser.getId();
            return userMapper.selectUser(userId);
        } catch (RuntimeException e) {
            throw new InternetServerException("查询用户信息失败，数据库查询时发生异常");
        }
    }

    public int insertUser(User newUser) {
        User user = userMapper.findByUsername(newUser.getUsername());
        if (user != null) {
            throw new InternetServerException("用户名已存在: " + newUser.getUsername());
        }

        ULID ulidGenerator = new ULID();
        ULID.Value ulid = ulidGenerator.nextValue();
        String strId = ulid.toString();
        newUser.setStrId(strId);
        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        newUser.setApiKey(UUID.randomUUID().toString());
        return userMapper.insertUser(newUser);
    }

    public int deleteUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User loginUser = (User) authentication.getPrincipal();
        Long userId = loginUser.getId();
        deleteOldAvatar(loginUser.getAvatar());
        return userMapper.deleteUser(userId);
    }

    public int updateUserInfo(User newUser) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User loginUser = (User) authentication.getPrincipal();
        Long userId = loginUser.getId();
        newUser.setId(userId);
        if (newUser.getUsername() != null && newUser.getUsername().isEmpty()) {
            User user = userMapper.findByUsername(newUser.getUsername());
            if (user != null) {
                throw new InternetServerException("用户名已存在");
            }
        }
        if (newUser.getPassword() != null && !newUser.getPassword().isEmpty()) {
            newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        }
        return userMapper.updateUser(newUser);
    }

    public User getInfoById(Long id) {
        return userMapper.getinfobyid(id);
    }

    public String login(User user) {
        try {
            //把用户信息传入springsecurity的拦截链
            UsernamePasswordAuthenticationToken authenticationToken=new UsernamePasswordAuthenticationToken(user.getUsername(),user.getPassword());
            //获取返回的完整用户信息
            Authentication authentication= authenticationManager.authenticate(authenticationToken);
            //验证用户是否正确
            if(Objects.isNull(authentication)){
                throw new UnauthorizedException("用户名或密码错误");
            }
            //使用userid生成token
            User loginUser= (User) authentication.getPrincipal();
            Long userId=loginUser.getId();
            String jwt = JwtUtil.generateToken(userId);
            //authenticate用户完整信息存入redis，id作为key,过期时间为一天（如果没被登出）
            redisCache.setCacheObject("login:"+userId,loginUser,1, TimeUnit.DAYS);
            //把token响应给前端
            return jwt;
        } catch (BadCredentialsException e) {
            throw new UnauthorizedException("用户名或密码错误");
        }
    }

    public void logout(){
        try{
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User loginUser = (User) authentication.getPrincipal();
            Long userid = loginUser.getId();
            redisCache.deleteObject(("login:" + userid));
        }catch (Exception e){
            throw new InternetServerException("登出失败");
        }
    }

    public String handleAvatarUpload(MultipartFile file) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User loginUser = (User) authentication.getPrincipal();
        Long userId = loginUser.getId();
        User user = userMapper.selectUser(userId);
        String oldAvatarUrl = user.getAvatar();
        deleteOldAvatar(oldAvatarUrl);
        String fileName = generateFileName(file);
        Path filePath = Paths.get(uploadDir, fileName);
        try {
            Files.createDirectories(filePath.getParent());//确保目录存在
            Files.write(filePath, file.getBytes());//写入文件
        } catch (IOException e) {
            throw new InternetServerException("保存头像失败");
        }
        String newAvatarUrl = "/uploads/" + fileName;
        User newUser = new User();
        newUser.setId(userId);
        newUser.setAvatar(newAvatarUrl);
        userMapper.updateUser(newUser);
        return newAvatarUrl;
    }

    public String generateFileName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String fileExt = originalFilename.substring(originalFilename.lastIndexOf("."));
        return "avatar-" + UUID.randomUUID() + fileExt;
    }

    public void deleteOldAvatar(String oldAvatarUrl) {
        if (oldAvatarUrl != null &&oldAvatarUrl.startsWith("/uploads/")){
            String oldFileName=oldAvatarUrl.substring("/uploads/".length());
            Path oldFilePath= Paths.get(uploadDir,oldFileName);
            try{
                Files.deleteIfExists(oldFilePath);
            } catch (IOException e) {
                throw new InternetServerException("删除头像失败");
            }
        }
    }
}
