/**
 * ログイン履歴の管理を担当するService
 *
 * セキュリティ判定に使用するログイン履歴を保存し、
 * 1ユーザーにつき直近2件だけ保持する。
 */

package com.app.myblogpusher.service.Login;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.LoginHistory;
import com.app.myblogpusher.repository.LoginHistoryRepository;

@Service
public class LoginHistoryService {

	@Autowired
	private LoginHistoryRepository loginHistoryRepository;

	@Autowired
	private LoginHistoryCleanupAsyncService loginHistoryCleanupAsyncService;

	/**
	 * ログイン成功時の履歴を保存する。
	 *
	 * 保存後、同一ユーザーの古い履歴を削除して
	 * 最大2件だけ残す。
	 */
	public Long recordLogin(
			Long userId,
			String ipAddress,
			String region,
			String userAgent) {

		LoginHistory history = new LoginHistory();

		history.setUserId(userId);
		history.setIpAddress(ipAddress);
		history.setRegion(region);
		history.setUserAgent(userAgent);
		history.setLoginDate(LocalDateTime.now());

		history = loginHistoryRepository.save(history);

		// 古い履歴の整理はログイン処理を止めないよう非同期で実行する。
		loginHistoryCleanupAsyncService.cleanupAsync(userId);

		return history.getId();
	}

	/**
	 * 非同期で取得したログイン元regionを履歴へ反映する。
	 */
	public void updateRegion(Long historyId, String region) {

		if (historyId == null || region == null || region.isBlank()) {
			return;
		}

		loginHistoryRepository.findById(historyId).ifPresent(history -> {
			history.setRegion(region);
			loginHistoryRepository.save(history);
		});
	}

	/**
	 * 最新のログイン履歴を取得する。
	 */
	public LoginHistory findLatestLogin(Long userId) {

		return loginHistoryRepository
				.findFirstByUserIdOrderByLoginDateDesc(userId)
				.orElse(null);
	}

	/**
	 * 直前のログイン履歴を取得する。
	 */
	public LoginHistory findPreviousLogin(Long userId) {

		List<LoginHistory> histories = loginHistoryRepository
				.findByUserIdOrderByLoginDateDesc(userId);

		if (histories.size() < 2) {
			return null;
		}

		return histories.get(1);
	}
}