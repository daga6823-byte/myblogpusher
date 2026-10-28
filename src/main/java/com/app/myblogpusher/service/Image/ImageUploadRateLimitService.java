/**
 * 画像アップロードのレート制限を担当するサービス
 *
 * ユーザー単位でリクエスト履歴を管理し、
 * 連続アップロードおよび短時間の大量リクエストを制限する。
 */
package com.app.myblogpusher.service.Image;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Service;

@Service
public class ImageUploadRateLimitService {

	private static final long MIN_INTERVAL_MILLIS = 1_000L;
	private static final long WINDOW_MILLIS = 60_000L;
	private static final int MAX_REQUESTS_PER_WINDOW = 10;

	private final ConcurrentMap<Long, Deque<Long>> requestHistory = new ConcurrentHashMap<>();

	/**
	 * アップロードリクエストを受け付けられるか判定する。
	 *
	 * 1秒に1リクエスト、1分間に最大10リクエストを許可する。
	 */
	public boolean isAllowed(Long userId) {
		long now = System.currentTimeMillis();

		Deque<Long> requests = requestHistory.computeIfAbsent(
				userId,
				key -> new ArrayDeque<>());

		synchronized (requests) {
			while (!requests.isEmpty()
					&& now - requests.peekFirst() >= WINDOW_MILLIS) {
				requests.removeFirst();
			}

			if (!requests.isEmpty()
					&& now - requests.peekLast() < MIN_INTERVAL_MILLIS) {
				return false;
			}

			if (requests.size() >= MAX_REQUESTS_PER_WINDOW) {
				return false;
			}

			requests.addLast(now);
			return true;
		}
	}
}