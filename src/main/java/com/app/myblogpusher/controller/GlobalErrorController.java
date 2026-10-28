
/**
 * アプリケーション共通のエラー処理を担当するコントローラー
 *
 * 画面処理で発生した未処理例外を共通エラー画面へ集約する。
 * REST APIではHTML画面へ切り替えず、HTTPエラー応答を返す。
 */
package com.app.myblogpusher.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalErrorController {

    /**
     * 未処理例外を共通エラー画面またはAPIエラー応答へ変換する。
     */
    @ExceptionHandler(Exception.class)
    public Object handleException(
            Exception exception,
            HttpServletRequest request,
            Model model) {

        // APIの例外はJSON形式のエラー応答として返す。
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "INTERNAL_SERVER_ERROR",
                            "message", "サーバー内部でエラーが発生しました"));
        }

        // 画面の例外は既存の共通エラー画面へ集約する。
        model.addAttribute(
                "errorMessage",
                "処理中にエラーが発生しました。時間をおいて再度お試しください。");
        model.addAttribute(
                "errorReason",
                exception.getMessage() != null
                        ? exception.getMessage()
                        : exception.getClass().getSimpleName());

        return "error";
    }

    /**
     * リクエストがAPI向けか判定する。
     */
    private boolean isApiRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String requestedWith = request.getHeader("X-Requested-With");
        String uri = request.getRequestURI();

        return (accept != null && accept.contains("application/json"))
                || "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || uri.startsWith(request.getContextPath() + "/api/");
    }
}
