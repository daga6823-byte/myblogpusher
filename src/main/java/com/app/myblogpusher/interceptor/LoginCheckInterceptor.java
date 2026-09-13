/**
 * ログイン状態のチェックを担当するInterceptor
 *
 * ログイン済みユーザーだけが通常の画面・処理へ到達できるように、
 * Controllerの実行前にセッションのloginUserを確認する。
 *
 * ログイン前でも利用するログイン画面・2FA認証処理については、
 * 個別にチェック対象外として扱う。
 */
package com.app.myblogpusher.interceptor;

import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class LoginCheckInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

	    String requestUri = request.getRequestURI();

	    // 2FA認証・初期設定中は、まだloginUserがセッションに存在しないため、
	    // ログインチェックの対象外とする。
	    if (requestUri.equals("/login/2fa/setup")
	            || requestUri.equals("/login/2fa")) {
	        return true;
	    }

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