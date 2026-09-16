/**
 * アプリケーション全体の例外を捕捉し、
 * 発生したリクエスト情報をログへ出力する例外ハンドラー。
 */
package com.app.myblogpusher.handler;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * JSONリクエストの読み込みに失敗した場合の処理。
	 *
	 * 400の発生元URIと例外内容を記録して、
	 * どのエンドポイントでエラーになっているかを特定する。
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<String> handleHttpMessageNotReadable(
			HttpMessageNotReadableException e,
			HttpServletRequest request) {

		System.out.println("=== HttpMessageNotReadableException ===");
		System.out.println("method = " + request.getMethod());
		System.out.println("uri = " + request.getRequestURI());
		System.out.println("query = " + request.getQueryString());
		System.out.println("contentType = " + request.getContentType());
		System.out.println("message = " + e.getMessage());
		System.out.println("cause = " + e.getCause());

		return ResponseEntity.badRequest().body("Bad Request");
	}
}