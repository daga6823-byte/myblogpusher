/**
 * 非同期処理で発生した未捕捉例外の記録を担当する設定クラス
 *
 * @Async メソッドから外へ漏れた例外を捕捉し、
 * ApplicationErrorLogServiceを通じてDBへ記録する。
 */
package com.app.myblogpusher.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;

import com.app.myblogpusher.service.ApplicationErrorLogService;

@Configuration
public class AsyncConfig implements AsyncConfigurer {

    private final ApplicationErrorLogService applicationErrorLogService;

    public AsyncConfig(
            ApplicationErrorLogService applicationErrorLogService) {
        this.applicationErrorLogService = applicationErrorLogService;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {

        return (exception, method, params) -> {

            String processName = method.getDeclaringClass().getSimpleName()
                    + "." + method.getName();

            applicationErrorLogService.recordAsync(
                    exception,
                    null,
                    processName + " / args=" + java.util.Arrays.deepToString(params));
        };
    }
}