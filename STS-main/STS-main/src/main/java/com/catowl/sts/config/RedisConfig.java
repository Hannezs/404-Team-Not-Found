package com.catowl.sts.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    @SuppressWarnings("all")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {

        // --- 1. 创建 RedisTemplate 对象 ---
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // --- 2. 创建序列化器 ---

        // (a) 创建 JSON 序列化器
        Jackson2JsonRedisSerializer<Object> jacksonSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();

        om.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // (b) 指定要序列化的域(field,get,set)和修饰符范围(ANY)
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // (c) [关键] 序列化时写入类型信息，反序列化时才能正确转换为指定对象
        //      - LaissezFaireSubTypeValidator.instance: 验证器，允许所有类型
        //      - ObjectMapper.DefaultTyping.NON_FINAL: 只对非 final 类型添加类型信息
        //      - JsonTypeInfo.As.PROPERTY: 将类型信息作为一个属性 (如 "@class") 存储
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        // (d) [关键] 支持 Java 8 的时间类型 (LocalDateTime, LocalDate 等)
        om.registerModule(new JavaTimeModule());

        jacksonSerializer.setObjectMapper(om);

        // (e) 创建 String 序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // --- 3. 配置 Template 的序列化方式 ---

        // (1) Key 序列化方式 (使用 String)
        template.setKeySerializer(stringSerializer);

        // (2) Value 序列化方式 (使用 JSON)
        template.setValueSerializer(jacksonSerializer);

        // (3) Hash Key 序列化方式 (使用 String)
        template.setHashKeySerializer(stringSerializer);

        // (4) Hash Value 序列化方式 (使用 JSON)
        template.setHashValueSerializer(jacksonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
