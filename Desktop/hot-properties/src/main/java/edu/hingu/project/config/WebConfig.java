package edu.hingu.project.config;

import java.nio.file.Paths;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");

        String uploadDir = Paths.get(System.getProperty("user.dir"), "uploads", "profile-pictures")
                                 .toAbsolutePath().toString();
        registry.addResourceHandler("/profile-pictures/**")
                .addResourceLocations("file:" + uploadDir + "/");

        String propertyImgDir = Paths.get("src/main/resources/static/images/property-images")
                                     .toAbsolutePath().toString();
        registry.addResourceHandler("/images/property-images/**")
                .addResourceLocations("file:" + propertyImgDir + "/");
    }
}
