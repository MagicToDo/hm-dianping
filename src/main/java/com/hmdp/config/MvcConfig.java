package com.hmdp.config;

import com.hmdp.utils.LoginInterceptor;
import com.hmdp.utils.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
//		页面登录拦截器
		registry.addInterceptor(new LoginInterceptor())
				.excludePathPatterns(
						"/user/code",
						"/user/login",
						"/shop",
						"/shop/**",
						"/shop-type/list",
						"/api/**"
				).excludePathPatterns("/swagger**/**")
				.excludePathPatterns("/webjars/**")
				.excludePathPatterns("/v3/**")
				.excludePathPatterns("/doc.html").order(1);
//		缓存刷新拦截器
		registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate)).addPathPatterns("/**").order(0);
	}

}
