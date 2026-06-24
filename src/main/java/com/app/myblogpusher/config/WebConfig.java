package com.app.myblogpusher.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.app.myblogpusher.interceptor.LoginCheckInterceptor;


@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new LoginCheckInterceptor())
				.addPathPatterns("/**")
				.excludePathPatterns(
					    "/login",
					    "/login/forgot",
					    "/login/reset",
					    "/register",
					    "/health",
					    "/css/**",
					    "/js/**",
					    "/images/**");
	}
}