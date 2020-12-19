package com.hz.community.config;

import com.hz.community.interceptor.DataInterceptor;
import com.hz.community.interceptor.LoginTicketInterceptor;
import com.hz.community.interceptor.MessageInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private LoginTicketInterceptor loginTicketInterceptor;

    private MessageInterceptor messageInterceptor;

    private DataInterceptor dataInterceptor;

    @Autowired
    public WebMvcConfig(LoginTicketInterceptor loginTicketInterceptor, MessageInterceptor messageInterceptor, DataInterceptor dataInterceptor) {
        this.loginTicketInterceptor = loginTicketInterceptor;
        this.messageInterceptor = messageInterceptor;
        this.dataInterceptor = dataInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginTicketInterceptor).
                excludePathPatterns("/**/*.css","/**/*.js","/**/*.png","/**/*.jpg","/**/*.jpeg");

        registry.addInterceptor(messageInterceptor).
                excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");

        registry.addInterceptor(dataInterceptor).
                excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");
    }
}
