
/**
 * APIリクエストのレート制限を担当するインターセプター
 *
 * @RestController または @ResponseBody が付いたハンドラーを自動判定し、
 * IPアドレス単位で秒間・分間のリクエスト回数を制限する。
 * 通常の画面遷移と、明示的に除外したエンドポイントは制限対象外とする。
 *
 * 制限回数はインスタンス内のメモリで管理するため、
 * 複数インスタンス間では共有されない。
 */
package com.app.myblogpusher.interceptor;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiRateLimitInterceptor implements HandlerInterceptor {

	private static final long SECOND_LIMIT_MILLIS = 1_000L;
	private static final long MINUTE_LIMIT_MILLIS = 60_000L;
	private static final int MAX_REQUESTS_PER_SECOND = 1;
	private static final int MAX_REQUESTS_PER_MINUTE = 60;

	/**
	 * レート制限を適用しないエンドポイント
	 *
	 * ヘルスチェックと、画面表示時に自動取得する記事リンク用APIを除外する。
	 */
	private static final Set<String> EXCLUDED_PATHS = Set.of(
			"/health",
			"/article/link/articles",
			"/article/link/categories");

	/**
	 * IPアドレスごとのリクエスト履歴
	 */
	private final ConcurrentMap<String, RequestHistory> requestHistories = new ConcurrentHashMap<>();

	/**
	 * APIリクエストを判定し、制限超過時はHTTP 429を返す。
	 */
	@Override
	public boolean preHandle(
			HttpServletRequest request,
			HttpServletResponse response,
			Object handler) throws IOException {

		if (!(handler instanceof HandlerMethod handlerMethod)) {
			return true;
		}

		if (!isApiHandler(handlerMethod)) {
			return true;
		}

		String path = getRequestPath(request);

		if (EXCLUDED_PATHS.contains(path)
				|| "OPTIONS".equalsIgnoreCase(request.getMethod())) {
			return true;
		}

		// プロキシ由来のヘッダーを無条件に信用せず、接続元アドレスを使用する。
		String clientIp = request.getRemoteAddr();

		if (clientIp == null || clientIp.isBlank()) {
			response.sendError(
					HttpServletResponse.SC_FORBIDDEN,
					"Client address could not be determined");
			return false;
		}

		long now = System.currentTimeMillis();

		RequestHistory history = requestHistories.computeIfAbsent(
				clientIp, key -> new RequestHistory());

		synchronized (history) {
			history.removeExpiredRequests(now);

			if (history.isRateLimitExceeded(now)) {
				response.setStatus(429);
				response.setContentType("application/json");
				response.setCharacterEncoding("UTF-8");
				response.getWriter().write(
						"{\"error\":\"Too many requests\"}");
				return false;
			}

			history.addRequest(now);
		}

		return true;
	}

	/**
	 * ハンドラーがAPIとして定義されているか判定する。
	 *
	 * クラスに@RestControllerがある場合、またはメソッドに
	 * @ResponseBodyがある場合をAPIとして扱う。
	 */
	private boolean isApiHandler(HandlerMethod handlerMethod) {
		Class<?> beanType = handlerMethod.getBeanType();

		return AnnotatedElementUtils.hasAnnotation(
				beanType, RestController.class)
				|| AnnotatedElementUtils.hasAnnotation(
						handlerMethod.getMethod(), ResponseBody.class);
	}

	/**
	 * コンテキストパスを除いたリクエストパスを取得する。
	 */
	private String getRequestPath(HttpServletRequest request) {
		String contextPath = request.getContextPath();
		String requestUri = request.getRequestURI();

		if (contextPath != null
				&& !contextPath.isEmpty()
				&& requestUri.startsWith(contextPath)) {
			return requestUri.substring(contextPath.length());
		}

		return requestUri;
	}

	/**
	 * 1つのIPアドレスに対するリクエスト時刻を管理する。
	 */
	private static class RequestHistory {

		private final Deque<Long> requestTimes = new ArrayDeque<>();

		/**
		 * 直近60秒より古いリクエスト履歴を削除する。
		 */
		private void removeExpiredRequests(long now) {
			while (!requestTimes.isEmpty()
					&& now - requestTimes.peekFirst() >= MINUTE_LIMIT_MILLIS) {
				requestTimes.removeFirst();
			}
		}

		/**
		 * 直近1秒以内のリクエスト、または直近60秒の上限を確認する。
		 */
		private boolean isRateLimitExceeded(long now) {
			if (!requestTimes.isEmpty()
					&& now - requestTimes.peekLast() < SECOND_LIMIT_MILLIS) {
				return true;
			}

			return requestTimes.size() >= MAX_REQUESTS_PER_MINUTE;
		}

		/**
		 * 許可したリクエストの時刻を記録する。
		 */
		private void addRequest(long now) {
			requestTimes.addLast(now);
		}
	}
}
