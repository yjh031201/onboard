package com.kanban.backend.config;

import com.kanban.backend.settings.ArchiveGuardInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ArchiveGuardInterceptor archiveGuardInterceptor;

    public WebMvcConfig(ArchiveGuardInterceptor archiveGuardInterceptor) {
        this.archiveGuardInterceptor = archiveGuardInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(archiveGuardInterceptor).addPathPatterns("/api/**");
    }
}
