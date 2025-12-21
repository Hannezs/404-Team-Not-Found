package com.catowl.sts.filter;

import com.catowl.sts.exception.UnauthorizedException;
import com.catowl.sts.mapper.DeviceMapper;
import com.catowl.sts.model.entity.User;
import com.catowl.sts.utils.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @program: STS
 * @description: 设备绑定拦截器
 * @author: qqCatOwlbb
 * @create: 2025-11-23 14:28
 **/
@Component
public class DeviceAuthFilter extends OncePerRequestFilter {
    @Autowired
    private RedisCache redisCache;

    @Autowired
    private DeviceMapper deviceMapper; // 需要用到上一轮中定义的 Mapper

    // Redis Key 前缀，例如 "device:auth:key-abc-123"
    private static final String DEVICE_AUTH_KEY_PREFIX = "device:auth:";
    // 缓存时间 (例如 24 小时)
    private static final long CACHE_TTL_HOURS = 24;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 只拦截设备上传数据的接口，其他接口跳过
        String path = request.getRequestURI();
        return !path.startsWith("/api/devices/upload");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // 1. 检查 Header 格式
        if (!StringUtils.hasText(header) || !header.startsWith("ApiKey ")) {
            // 如果没有带 Key，直接放行 (交给后续 Security 配置处理，通常会因为没身份而被拒绝)
            // 或者直接在这里抛出异常中断
            chain.doFilter(request, response);
            return;
        }

        String apiKey = header.substring(7); // 去掉 "ApiKey "
        String redisKey = DEVICE_AUTH_KEY_PREFIX + apiKey;

        // 2. 尝试从 Redis 获取用户信息 (模仿 JwtAuthenticationFilter)
        User deviceUser = redisCache.getCacheObject(redisKey);
        if (deviceUser == null) {
            // 3. Redis 未命中 (缓存击穿/过期)，查询数据库 (Lazy Load)
            // 这里的 deviceMapper.findUserByApiKey 需要我们在 Mapper 中补充
            Long userId = deviceMapper.findUserIdByApiKey(apiKey);
            System.out.println(userId);

            if (userId != null) {
                // 暂时需要构造一个简单的 User 对象存入缓存，或者查询完整 User
                // 为保持高性能，建议只存必要信息，或者调用 userService.getById(userId)
                // 这里假设我们有一个方法获取完整 User，为了演示简单，我们手动构造
                deviceUser = new User();
                deviceUser.setId(userId);
                // 存入 Redis，避免下次查库
                redisCache.setCacheObject(redisKey, deviceUser, (int) CACHE_TTL_HOURS, TimeUnit.HOURS);
            } else {
                // Key 无效
                throw new UnauthorizedException("无效的 API Key");
            }
        }

        // 4. 构建 Authentication 对象并存入 Context
        // 这样 Controller 中 @AuthenticationPrincipal 就能拿到了
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(deviceUser, null, null);

        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        chain.doFilter(request, response);
    }
}
