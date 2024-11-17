package com.zougn.blog.config;


import com.zougn.blog.handler.PageableHandlerInterceptor;
import com.zougn.blog.handler.WebSecurityHandler;
import com.zougn.blog.service.RedisService;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * web mvc配置
 *
 * @author yezhiqiu
 * @date 2021/07/29
 */
@Configuration
@AllArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    private RedisService redisService;

    @Bean
    public WebSecurityHandler getWebSecurityHandler() {
        return new WebSecurityHandler(redisService);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowCredentials(true)
                .allowedHeaders("*")
                .allowedOriginPatterns("*")
                .allowedMethods("*");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new PageableHandlerInterceptor());
        registry.addInterceptor(getWebSecurityHandler());
    }


}
