/**
 * HTTPリクエスト処理中に発生した未捕捉例外を記録するフィルター
 *
 * ControllerAdviceで処理されず、テンプレート描画などの段階で
 * 発生した例外をApplicationErrorLogServiceへ記録する。
 */
package com.app.myblogpusher.filter;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.app.myblogpusher.service.ApplicationErrorLogService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApplicationErrorLoggingFilter extends OncePerRequestFilter {

    private final ApplicationErrorLogService errorLogService;

    public ApplicationErrorLoggingFilter(
            ApplicationErrorLogService errorLogService) {
        this.errorLogService = errorLogService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            try {
                errorLogService.record(e, request);
            } catch (RuntimeException loggingException) {
                e.addSuppressed(loggingException);
            }

            throw e;
        }
    }
}