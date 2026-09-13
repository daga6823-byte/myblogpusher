/**
 * 古いログイン履歴の非同期整理を担当するService
 *
 * ログイン処理の応答速度に影響しないよう、
 * 同一ユーザーの古いログイン履歴をバックグラウンドで削除する。
 *
 * 1ユーザーにつき直近2件だけを保持する。
 */
package com.app.myblogpusher.service.Login;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.LoginHistory;
import com.app.myblogpusher.repository.LoginHistoryRepository;

@Service
public class LoginHistoryCleanupAsyncService {

	@Autowired
	private LoginHistoryRepository loginHistoryRepository;

	/**
	 * 古いログイン履歴を非同期で削除する。
	 */
	public void cleanupAsync(Long userId) {

		CompletableFuture.runAsync(() -> {

			List<LoginHistory> histories = loginHistoryRepository
					.findByUserIdOrderByLoginDateDesc(userId);

			for (int i = 2; i < histories.size(); i++) {
				loginHistoryRepository.delete(histories.get(i));
			}
		});
	}
}