package com.vibeclip.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${vibeclip.upload.dir:uploads}")
    private String uploadDir;

    @Value("${vibeclip.upload.base-url:/uploads}")
    private String uploadBaseUrl;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        String normalizedBase = uploadBaseUrl.endsWith("/")
                ? uploadBaseUrl
                : uploadBaseUrl + "/";

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

        registry.addResourceHandler(normalizedBase + "**")
                .addResourceLocations("file:" + uploadPath.toString() + "/");
    }
}