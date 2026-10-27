package com.app.myblogpusher.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.app.myblogpusher.filter.CsrfProtectionFilter;
import com.app.myblogpusher.interceptor.ApiRateLimitInterceptor;
import com.app.myblogpusher.interceptor.LoginCheckInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public LoginCheckInterceptor loginCheckInterceptor() {
		return new LoginCheckInterceptor();
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {

		// ログイン状態の確認
		registry.addInterceptor(loginCheckInterceptor())
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

		// APIのレート制限
		registry.addInterceptor(apiRateLimitInterceptor())
				.addPathPatterns("/**");
	}

	@Bean
	public ApiRateLimitInterceptor apiRateLimitInterceptor() {
		return new ApiRateLimitInterceptor();
	}
	
	/**
	 * CSRF対策フィルターを登録する。
	 *
	 * 既存フォームへのトークン送信対応が完了するまでは無効にし、
	 * 全画面の対応確認後に有効化する。
	 */
	@Bean
	public FilterRegistrationBean<CsrfProtectionFilter> csrfProtectionFilter() {
	    FilterRegistrationBean<CsrfProtectionFilter> registration =
	            new FilterRegistrationBean<>();

	    registration.setFilter(new CsrfProtectionFilter());
	    registration.addUrlPatterns("/*");
	    registration.setName("csrfProtectionFilter");
	    registration.setEnabled(false);
	    registration.setOrder(1);

	    return registration;
	}
}