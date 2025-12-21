package com.catowl.sts.filter;


import com.catowl.sts.exception.UnauthorizedException;
import com.catowl.sts.model.entity.User;
import com.catowl.sts.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import com.catowl.sts.utils.RedisCache;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private RedisCache redisCache;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 定义不需要过滤的路径
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui/") ||
                path.startsWith("/swagger-resources") ||
                path.startsWith("/v2/api-docs") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-ui.html") ||
                path.startsWith("/webjars/") ||
                path.startsWith("/doc.html");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        //获取token
        String token = request.getHeader("Authorization");

        if(!StringUtils.hasText(token)|| !token.startsWith("Bearer ")){
            //token为空，应该让其跳过下面的token解析代码，交由security内的拦截器来拦截
            chain.doFilter(request,response);
            //拦截器拦截完返回时会再次进入该if，同样要避免执行下面的token解析代码
            return;
        }
        //解析token，获取token存储的用户id
        token = token.substring(7);
        Long userId = Long.parseLong(JwtUtil.getUserIdFromToken(token));
        //从redis中获取用户信息
        String redisKey="login:"+userId;
        User loginUser=redisCache.getCacheObject(redisKey);
        if(Objects.isNull(loginUser)){
            throw new UnauthorizedException("用户未登录");
        }
        //存入上下文中
        UsernamePasswordAuthenticationToken authenticationToken=new UsernamePasswordAuthenticationToken(loginUser,null,null);
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        chain.doFilter(request, response);
    }
}
