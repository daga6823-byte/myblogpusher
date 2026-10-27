
/**
 * CSRF攻撃対策を担当する共通フィルター
 *
 * セッションごとにランダムなCSRFトークンを発行し、
 * Cookieを通じてブラウザーへ公開する。
 * 更新系リクエストでは、HTTPヘッダーまたはフォーム項目の
 * CSRFトークンをセッション内のトークンと照合する。
 *
 * フィルターの登録はWebConfigで明示的に行う。
 */
package com.app.myblogpusher.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class CsrfProtectionFilter extends OncePerRequestFilter {

	public static final String CSRF_TOKEN_SESSION_ATTRIBUTE = "csrfToken";
	public static final String CSRF_TOKEN_REQUEST_ATTRIBUTE = "_csrf";

	private static final String CSRF_TOKEN_COOKIE = "XSRF-TOKEN";
	private static final String CSRF_TOKEN_PARAMETER = "_csrf";
	private static final String CSRF_TOKEN_HEADER = "X-CSRF-TOKEN";

	private static final String[] SAFE_METHODS = {
			"GET", "HEAD", "OPTIONS"
	};

	private final SecureRandom secureRandom = new SecureRandom();

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		// TRACEはCSRF対策とは別に使用を禁止する。
		if ("TRACE".equalsIgnoreCase(request.getMethod())) {
			response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			return;
		}

		boolean safeMethod = isSafeMethod(request.getMethod());

		// GET等では既存セッションがある場合のみ取得する。
		// 更新系リクエストでセッションがなければ新規生成しない。
		HttpSession session = request.getSession(safeMethod);

		String expectedToken = null;

		if (session != null) {
			expectedToken = (String) session.getAttribute(
					CSRF_TOKEN_SESSION_ATTRIBUTE);

			if (expectedToken == null) {
				expectedToken = generateToken();
				session.setAttribute(
						CSRF_TOKEN_SESSION_ATTRIBUTE, expectedToken);
			}

			request.setAttribute(
					CSRF_TOKEN_REQUEST_ATTRIBUTE, expectedToken);

			// JavaScriptから読み取れるCSRFトークンCookieを発行する。
			Cookie csrfCookie = new Cookie(
					CSRF_TOKEN_COOKIE, expectedToken);
			csrfCookie.setHttpOnly(false);
			csrfCookie.setSecure(request.isSecure());
			csrfCookie.setPath("/");
			csrfCookie.setAttribute("SameSite", "Lax");
			response.addCookie(csrfCookie);
		}

		// 更新系リクエストはトークンの一致を必須とする。
		if (!safeMethod) {
			if (session == null) {
				response.sendError(
						HttpServletResponse.SC_FORBIDDEN,
						"CSRF token is missing");
				return;
			}

			String submittedToken = request.getHeader(CSRF_TOKEN_HEADER);

			if (submittedToken == null || submittedToken.isBlank()) {
				submittedToken = request.getParameter(
						CSRF_TOKEN_PARAMETER);
			}

			if (!isValidToken(expectedToken, submittedToken)) {
				response.sendError(
						HttpServletResponse.SC_FORBIDDEN,
						"Invalid CSRF token");
				return;
			}
		}

		filterChain.doFilter(request, response);
	}

	/**
	 * CSRFトークン検証が不要なHTTPメソッドか判定する。
	 */
	private boolean isSafeMethod(String method) {
		for (String safeMethod : SAFE_METHODS) {
			if (safeMethod.equalsIgnoreCase(method)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 暗号学的に安全な乱数を使用してトークンを生成する。
	 */
	private String generateToken() {
		byte[] bytes = new byte[32];
		secureRandom.nextBytes(bytes);

		return Base64.getUrlEncoder()
				.withoutPadding()
				.encodeToString(bytes);
	}

	/**
	 * セッション内のトークンと送信されたトークンを定時間比較する。
	 */
	private boolean isValidToken(
			String expectedToken,
			String submittedToken) {

		if (expectedToken == null
				|| submittedToken == null
				|| submittedToken.isBlank()) {
			return false;
		}

		return MessageDigest.isEqual(
				expectedToken.getBytes(StandardCharsets.UTF_8),
				submittedToken.getBytes(StandardCharsets.UTF_8));
	}
}
