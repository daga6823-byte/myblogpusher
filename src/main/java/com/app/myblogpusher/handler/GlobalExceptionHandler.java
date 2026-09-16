/**
 * アプリケーション全体で発生するHTTPリクエスト系例外を捕捉し、
 * 共通エラー画面へ遷移させる例外ハンドラー。
 */
package com.app.myblogpusher.handler;

import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * 必須のリクエストパラメータが存在しない場合の処理。
	 *
	 * Controllerの@RequestParam不足による400を共通エラー画面へ遷移させる。
	 */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public String handleMissingServletRequestParameter(
			MissingServletRequestParameterException e,
			HttpServletRequest request,
			Model model) {

		System.out.println("=== MissingServletRequestParameterException ===");
		System.out.println("method = " + request.getMethod());
		System.out.println("uri = " + request.getRequestURI());
		System.out.println("parameter = " + e.getParameterName());
		System.out.println("message = " + e.getMessage());

		model.addAttribute("errorMessage",
				"必要なパラメータが送信されていないため、処理を続行できませんでした。");

		return "error";
	}
}