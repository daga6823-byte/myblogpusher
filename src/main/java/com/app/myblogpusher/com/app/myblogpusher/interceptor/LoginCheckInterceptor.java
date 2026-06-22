package com.app.myblogpusher.interceptor;

import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class LoginCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        Object loginUser = request.getSession(false) != null
                ? request.getSession(false).getAttribute("loginUser")
                : null;

        if (loginUser == null) {
            response.sendRedirect("/login");
            return false; // ここでコントローラーへの到達をブロック
        }

        return true;
    }
}