package com.app.myblogpusher.service.Login;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.repository.UserMasterRepository;

@Service
public class LoginService {

	@Autowired
	private UserMasterRepository userMasterRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private IpGeolocationService ipGeolocationService;

	@Autowired
	private LoginHistoryService loginHistoryService;

	public Optional<UserMaster> findAuthenticatedUser(String loginId, String password) {
		long totalStart = System.nanoTime();
		long stepStart = System.nanoTime();

		Optional<UserMaster> userOpt = userMasterRepository.findByLoginId(loginId);

		logElapsed("ユーザーDB検索", stepStart);

		if (userOpt.isEmpty()) {
			logElapsed("認証処理全体", totalStart);
			return Optional.empty();
		}

		UserMaster user = userOpt.get();

		if ("BANNED".equals(user.getUserStatus())) {
			logElapsed("認証処理全体", totalStart);
			return Optional.empty();
		}

		stepStart = System.nanoTime();
		boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());
		logElapsed("パスワード照合", stepStart);

		if (!passwordMatches) {
			int failedCount = user.getLoginFailedCount() == null
					? 0
					: user.getLoginFailedCount();

			user.setLoginFailedCount(failedCount + 1);

			stepStart = System.nanoTime();
			userMasterRepository.save(user);
			logElapsed("ログイン失敗回数の保存", stepStart);

			logElapsed("認証処理全体", totalStart);
			return Optional.empty();
		}

		// 3回以上の連続失敗があった場合は、
		// 2FA判定が終わるまで失敗回数を保持する。
		if (user.getLoginFailedCount() == null
				|| user.getLoginFailedCount() < 3) {

			user.setLoginFailedCount(0);

			stepStart = System.nanoTime();
			userMasterRepository.save(user);
			logElapsed("ログイン成功時の失敗回数リセット", stepStart);
		}

		logElapsed("認証処理全体", totalStart);
		return Optional.of(user);
	}

	/**
	 * 認証処理の経過時間をミリ秒単位でログ出力する。
	 */
	private void logElapsed(String processName, long startNanos) {
		long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;

		System.out.println(
				"[LoginServiceTiming] " + processName + ": "
						+ elapsedMillis + " ms");
	}

	/**
	 * ログイン元IPアドレスから国コードを取得する。
	 */
	public String findLoginRegion(String ipAddress) {
		return ipGeolocationService.findCountryCode(ipAddress);
	}

	public Optional<UserMaster> findUserForPasswordReset(String loginId, String email) {
		Optional<UserMaster> userOpt = userMasterRepository.findByLoginIdAndEmail(loginId, email);

		if (userOpt.isEmpty()) {
			return Optional.empty();
		}

		UserMaster user = userOpt.get();
		if ("BANNED".equals(user.getUserStatus())) {
			return Optional.empty();
		}

		return Optional.of(user);
	}

	public boolean resetPassword(Long userId, String newPassword) {
		Optional<UserMaster> userOpt = userMasterRepository.findById(userId);

		if (userOpt.isEmpty()) {
			return false;
		}

		UserMaster user = userOpt.get();
		user.setPassword(passwordEncoder.encode(newPassword));
		userMasterRepository.save(user);
		return true;
	}

	/**
	 * ログイン時に2FA認証が必要か判定する。
	 *
	 * 2FAが必要になる条件：
	 * ・登録regionとログイン元regionが異なる
	 * ・IPアドレスが変わっており、前回の2FA認証から1か月以上経過している
	 * ・パスワードを3回連続で間違えている
	 */
	public boolean requiresTwoFactor(
			UserMaster user,
			String ipAddress) {

		// パスワードを3回連続で間違えている場合は2FAを要求する。
		if (user.getLoginFailedCount() != null
				&& user.getLoginFailedCount() >= 3) {
			return true;
		}

		// 前回ログイン情報を取得する。
		var previousLogin = loginHistoryService.findPreviousLogin(user.getUserId());

		if (previousLogin == null) {
			return false;
		}

		// IPが変わっていなければ、月次の2FA対象ではない。
		if (ipAddress == null
				|| ipAddress.equals(previousLogin.getIpAddress())) {
			return false;
		}

		// 最後の2FA認証から1か月以上経過している場合は2FAを要求する。
		if (user.getTwoFactorAuthenticatedAt() == null) {
			return true;
		}

		return !user.getTwoFactorAuthenticatedAt()
				.plusMonths(1)
				.isAfter(java.time.LocalDateTime.now());
	}
}