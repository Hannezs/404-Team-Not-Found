package com.catowl.sts.config;


import springfox.documentation.service.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.oas.annotations.EnableOpenApi;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableOpenApi
public class SwaggerConfig {

    @Bean
    public Docket docket() {
        return new Docket(DocumentationType.OAS_30)
                .select()
                //指定扫描的包，即controller层
                .apis(RequestHandlerSelectors.basePackage("com.catowl.sts.controller"))
                .paths(PathSelectors.any())
                .build()
                .apiInfo(apiInfo())
                .securitySchemes(securitySchemes())
                .securityContexts(securityContexts());
    }

    private ApiInfo apiInfo() {
        //初始化作者信息
        Contact contact = new Contact("catowl", "https://github.com/qqCatOwlbb", "2734674581@qq.com");

        return new ApiInfo(
                "catowl的swaggerAPI文档",
                "该文档可帮助你实现对后端的交互",
                "1.0",
                "https://github.com/qqCatOwlbb/STS.git",
                contact,
                "Apache2.0",
                "http://www.apache.org/licenses/LICENSE-2.0",
                new ArrayList()
        );
    }

    private List<SecurityScheme> securitySchemes() {
        return Collections.singletonList(
                new ApiKey("Authorization", "Authorization", "header")
        );
    }

    private List<SecurityContext> securityContexts() {
        return Collections.singletonList(
                SecurityContext.builder()
                        .securityReferences(defaultAuth())
                        .forPaths(PathSelectors.regex("^(?!.*(login|register|public)).*$"))
                        .build()
        );
    }

    private List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        return Collections.singletonList(
                new SecurityReference("Authorization", authorizationScopes));
    }
}