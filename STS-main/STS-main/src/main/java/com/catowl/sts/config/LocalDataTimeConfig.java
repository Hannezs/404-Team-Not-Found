package com.catowl.sts.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Configuration
public class LocalDataTimeConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToLocalDateTimeConverter());
    }

    static class StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {

        @Override
        public LocalDateTime convert(String source) {
            if (source == null || source.trim().isEmpty()) {
                return null;
            }

            source = source.trim();

            // 1️⃣ 尝试解析带 Z 的 UTC 时间
            try {
                if (source.endsWith("Z")) {
                    Instant instant = Instant.parse(source);
                    return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                }
            } catch (DateTimeParseException ignored) {}

            // 2️⃣ 尝试 ISO_LOCAL_DATE_TIME 格式 (带毫秒或不带毫秒)
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS]");
            try {
                return LocalDateTime.parse(source, formatter);
            } catch (DateTimeParseException ignored) {}

            // 3️⃣ 如果都解析失败，抛异常
            throw new IllegalArgumentException("无法解析的日期时间格式: " + source);
        }
    }
}
