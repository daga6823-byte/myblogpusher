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

	/**
	 * 予期しない例外が発生した場合の処理。
	 *
	 * アプリケーション内部の例外内容をユーザーへ直接表示せず、
	 * 共通エラー画面へ遷移させる。
	 */
	@ExceptionHandler(Exception.class)
	public String handleException(
			Exception e,
			HttpServletRequest request,
			Model model) {

		System.out.println("=== Unexpected Exception ===");
		System.out.println("method = " + request.getMethod());
		System.out.println("uri = " + request.getRequestURI());
		System.out.println("message = " + e.getMessage());
		e.printStackTrace();

		model.addAttribute("errorMessage",
				"エラーが発生しました。時間をおいて再度お試しください。"
						+ "解決しない場合は管理者にお問い合わせください。");

		return "error";
	}
}