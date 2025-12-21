package com.catowl.sts.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")  // 允许所有来源（包括 Postman）
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // 允许的请求方法
                .allowedHeaders("*")  // 允许的请求头
                .exposedHeaders("Authorization")  // 允许前端获取的响应头
                .allowCredentials(false);
    }
    @Value("${file.qrcode-dir}")
    private String qrDir;

    @Value("${file.upload-dir}")
    private String upDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry){
        String qrLocation = "file:" + qrDir + "/";
        registry.addResourceHandler("/qrcode/**")
                .addResourceLocations(qrLocation)
                .setCachePeriod(3600);//缓存1h

        String avatarLocation = "file:" + upDir + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(avatarLocation)
                .setCachePeriod(3600);
    }
}
