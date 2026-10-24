/**
 * 古いログイン履歴の非同期整理を担当するService
 *
 * ログイン処理の応答速度に影響しないよう、
 * 同一ユーザーの古いログイン履歴をバックグラウンドで削除する。
 *
 * 1ユーザーにつき直近2件だけを保持する。
 */
package com.app.myblogpusher.service.Login;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.repository.LoginHistoryRepository;

@Service
public class LoginHistoryCleanupAsyncService {

	@Autowired
	private LoginHistoryRepository loginHistoryRepository;

	/**
	 * 毎日午前3時に、各ユーザーの古いログイン履歴を一括整理する。
	 *
	 * 各ユーザーの最新100件を保持し、それより古い履歴を削除する。
	 */
	@Scheduled(cron = "0 0 3 * * *")
	public void cleanupOldHistories() {
		int deletedCount = loginHistoryRepository.deleteOlderThanLatest100PerUser();

		System.out.println(
				"[LoginHistoryCleanup] 古いログイン履歴の削除件数: "
						+ deletedCount);
	}
}