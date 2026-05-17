package sfedu.ictis.woi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path avatarsPath = Paths.get(uploadDir, "avatars").toAbsolutePath();
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations(avatarsPath.toUri() + "/");

        Path poisPath = Paths.get(uploadDir, "pois").toAbsolutePath();
        registry.addResourceHandler("/pois/**")
                .addResourceLocations(poisPath.toUri() + "/");

        Path iconsPath = Paths.get(uploadDir, "icons").toAbsolutePath();
        registry.addResourceHandler("/icons/**")
                .addResourceLocations(iconsPath.toUri() + "/");
    }
}
