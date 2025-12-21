package com.catowl.sts.config;

import io.github.imfangs.dify.client.DifyChatflowClient;
import io.github.imfangs.dify.client.DifyClientFactory;
import io.github.imfangs.dify.client.model.DifyConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyDifyConfig {

    @Value("${dify.api-key}")
    private String apikey;

    @Value("${dify.base-url}")
    private String baseUrl;

    @Bean
    public DifyChatflowClient difyChatflowClient(){
        DifyConfig config = DifyConfig.builder()
                .baseUrl(baseUrl) // 例如 https://api.dify.ai/v1
                .apiKey(apikey)
                .connectTimeout(10000)      // 连接超时 10秒
                .readTimeout(300000)        // 读取超时 改为 300秒 (5分钟) [关键修改]
                .writeTimeout(60000)        // 写入超时 60秒
                .build();

        return DifyClientFactory.createChatWorkflowClient(config);
    }
}
