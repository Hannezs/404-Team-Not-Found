package com.catowl.sts.controller;

import com.catowl.sts.exception.BadRequestException;
import com.catowl.sts.model.dto.Request.UserLoginRequest;
import com.catowl.sts.model.dto.Request.UserRegisterRequest;
import com.catowl.sts.model.dto.Request.UserUpdateRequest;
import com.catowl.sts.model.dto.Response.MyApiResponse;
import com.catowl.sts.model.dto.Response.UserResponse;
import com.catowl.sts.model.entity.User;
import com.catowl.sts.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

@Api(tags = "1. 认证与用户", description = "处理用户注册、登录和个人信息管理")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024;

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    @ApiOperation(value = "用户注册", notes = "创建新用户账号")
    @ApiResponses({
            @io.swagger.annotations.ApiResponse(code = 201, message = "注册成功"),
            @io.swagger.annotations.ApiResponse(code = 400, message = "请求参数无效 (例如用户名已存在)")
    })
    public ResponseEntity<MyApiResponse<String>> register(
            @Valid @RequestBody UserRegisterRequest registerRequest) {

       User user = new User();
       user.setUsername(registerRequest.getUsername());
       user.setPassword(registerRequest.getPassword());
        if((user.getUsername()==null||user.getUsername().isEmpty())||(user.getPassword()==null||user.getPassword().isEmpty())){
            throw new BadRequestException("用户名或密码不能为空");
        }
        userService.insertUser(user);
        MyApiResponse<String> response = new MyApiResponse<>("注册成功", null);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @ApiOperation(value = "用户登录", notes = "使用用户名和密码获取 JWT")
    @ApiResponses({
            @io.swagger.annotations.ApiResponse(code = 200, message = "登录成功"),
            @io.swagger.annotations.ApiResponse(code = 401, message = "认证失败 (用户名或密码错误)")
    })
    public ResponseEntity<MyApiResponse<String>> login(
            @Valid @RequestBody UserLoginRequest loginRequest) {

        User user = new User();
        user.setUsername(loginRequest.getUsername());
        user.setPassword(loginRequest.getPassword());
        if(user.getUsername()==null||user.getPassword()==null){
            throw new BadRequestException("用户名或密码不能为空");
        }
        String token = userService.login(user);
        MyApiResponse<String> response = new MyApiResponse<>("登录成功", token);
        return ResponseEntity.ok(response);
    }

    @ApiOperation(
            value = "用户登出",
            notes = "清除用户的登录状态和token",
            response = MyApiResponse.class,
            produces = "application/json",
            httpMethod = "DELETE"
    )
    @DeleteMapping("/logout")
    public ResponseEntity<MyApiResponse<String>> logout(){
        userService.logout();
        MyApiResponse<String> response = new MyApiResponse<>("登出成功", null);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @ApiOperation(value = "获取当前用户信息", notes = "获取当前已登录用户的信息 (需要认证)")
    public ResponseEntity<MyApiResponse<UserResponse>> getCurrentUser() {
        User user = userService.selectUser();
        UserResponse userResponse=new UserResponse();
        userResponse.setUser(user);
        MyApiResponse<UserResponse> response = new MyApiResponse<>("获取成功", userResponse);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me")
    @ApiOperation(value = "删除当前用户账号", notes = "删除当前已登录用户的账号 (需要认证)")
    public ResponseEntity<MyApiResponse<String>> deleteCurrentUser() {
        userService.deleteUser();
        userService.logout();
        MyApiResponse<String> response = new MyApiResponse<>("账号删除成功", null);
        return ResponseEntity.ok(response);
    }

    @ApiOperation(
            value = "更新用户信息",
            notes = "更新用户的用户名、密码或简介信息",
            response = MyApiResponse.class,
            produces = "application/json",
            httpMethod = "POST"
    )
    @PostMapping("/update")
    public ResponseEntity<MyApiResponse<String>> update(
            @ApiParam(value = "用户信息", required = true) @RequestBody UserUpdateRequest userUpdateDTO
    ){
        User user = new User();
        userUpdateDTO.setUser(user);
        userService.updateUserInfo(user);
        boolean isUsernameModified = user.getUsername() != null && !user.getUsername().isEmpty();
        boolean isPasswordModified = user.getPassword() != null && !user.getPassword().isEmpty();
        MyApiResponse<String> response = new MyApiResponse<>("用户信息更新成功",null);
        return ResponseEntity.ok(response);
    }

    @ApiOperation(
            value = "更新用户头像",
            notes = "上传并更新用户的头像图片，支持jpg、png格式，大小不超过2MB",
            response = MyApiResponse.class,
            produces = "application/json",
            httpMethod = "POST"
    )
    @PostMapping("/updateavatar")
    public ResponseEntity<MyApiResponse<String>> updateAvatar(
            @ApiParam(value = "头像文件", required = true) @RequestParam(value = "file", required = false) MultipartFile file
    ){
        if(file!=null&&file.isEmpty()){
            if(file.getSize() > MAX_FILE_SIZE){
                throw new BadRequestException("文件过大，最大允许2MB");
            }
        }
        userService.handleAvatarUpload(file);
        MyApiResponse<String> response = new MyApiResponse<>("更新用户头像成功", null);
        return ResponseEntity.ok(response);
    }
}
