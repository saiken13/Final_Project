package edu.hingu.project.config;

import java.nio.file.Paths;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // ✅ Serve images from classpath (static resources)
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");

        // ✅ Serve uploaded profile pictures from file system
        String uploadDir = Paths.get(System.getProperty("user.dir"), "uploads", "profile-pictures")
                                 .toAbsolutePath().toString();
        registry.addResourceHandler("/profile-pictures/**")
                .addResourceLocations("file:" + uploadDir + "/");

        // ✅ Serve uploaded property images (if not packed in classpath)
        String propertyImgDir = Paths.get("src/main/resources/static/images/property-images")
                                     .toAbsolutePath().toString();
        registry.addResourceHandler("/images/property-images/**")
                .addResourceLocations("file:" + propertyImgDir + "/");
    }
}
