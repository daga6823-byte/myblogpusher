
/**
 * CSRF攻撃対策を担当する共通フィルター
 *
 * セッションごとにCSRFトークンを発行し、更新系リクエストで
 * リクエストパラメーターまたはHTTPヘッダーのトークンを検証する。
 *
 * トークンのない更新系リクエストや、トークンが一致しないリクエストは拒否する。
 * フォームとJavaScriptからトークンを送信する仕組みは別途共通化する。
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

	/** セッションに保存するCSRFトークンの属性名 */
	public static final String CSRF_TOKEN_SESSION_ATTRIBUTE = "csrfToken";

	/** リクエストから取得できるCSRFトークンの属性名 */
	public static final String CSRF_TOKEN_REQUEST_ATTRIBUTE = "_csrf";

	/** フォーム送信で使用するパラメーター名 */
	private static final String CSRF_TOKEN_PARAMETER = "_csrf";

	/** JavaScriptなどから送信するHTTPヘッダー名 */
	private static final String CSRF_TOKEN_HEADER = "X-CSRF-TOKEN";

	private static final String[] SAFE_METHODS = {
			"GET", "HEAD", "OPTIONS", "TRACE"
	};

	private final SecureRandom secureRandom = new SecureRandom();

	/**
	 * CSRFトークンを発行し、更新系リクエストを検証する。
	 */
	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		HttpSession session = request.getSession(
				isSafeMethod(request.getMethod()));

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
			Cookie csrfCookie = new Cookie("XSRF-TOKEN", expectedToken);
			csrfCookie.setHttpOnly(false);
			csrfCookie.setSecure(request.isSecure());
			csrfCookie.setPath("/");
			csrfCookie.setAttribute("SameSite", "Lax");
			response.addCookie(csrfCookie);
		}

		if (!isSafeMethod(request.getMethod())) {
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
	 * 読み取り専用メソッドかどうかを判定する。
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
	 * 暗号学的に安全な乱数からCSRFトークンを生成する。
	 */
	private String generateToken() {
		byte[] bytes = new byte[32];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder()
				.withoutPadding()
				.encodeToString(bytes);
	}

	/**
	 * トークンの存在と一致を定数時間比較で検証する。
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
