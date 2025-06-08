package edu.hingu.project.config;

import java.nio.file.Paths;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // ✅ For static images from /static/images in classpath
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");

        // ✅ For uploaded profile pictures from local file system
        String uploadDir = Paths.get(System.getProperty("user.dir"), "uploads", "profile-pictures")
                                 .toAbsolutePath()
                                 .toString();

        registry.addResourceHandler("/profile-pictures/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
