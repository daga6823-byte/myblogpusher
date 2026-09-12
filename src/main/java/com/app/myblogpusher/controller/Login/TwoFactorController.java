/**

* Google Authenticatorによる2FA認証を担当するコントローラー
*
* 初回ログイン時のGoogle Authenticator設定と、
* 既に設定済みのユーザーに対するTOTP認証を担当する。
  */
package com.app.myblogpusher.controller.Login;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.repository.UserMasterRepository;
import com.app.myblogpusher.repository.UserRepositoryRepository;
import com.app.myblogpusher.service.PublishedArticleService;
import com.app.myblogpusher.service.Article.ArticleWorkspaceService;
import com.app.myblogpusher.service.Login.LoginHistoryService;
import com.app.myblogpusher.service.Login.TwoFactorService;

import jakarta.servlet.http.HttpSession;

@Controller
public class TwoFactorController {

	@Autowired
	private UserMasterRepository userMasterRepository;

	@Autowired
	private TwoFactorService twoFactorService;

	@Autowired
	private LoginHistoryService loginHistoryService;

//	@Autowired
//	private SecurityMailService securityMailService;

	@Autowired
	private ArticleWorkspaceService workspaceService;

	@Autowired
	private UserRepositoryRepository userRepositoryRepository;

	@Autowired
	private PublishedArticleService publishedArticleService;

	/**
	 * 初回ログイン時のGoogle Authenticator設定画面を表示する。
	 */
	@GetMapping("/login/2fa/setup")
	public String setupForm(
	        HttpSession session,
	        Model model) {

	    System.out.println("2FA SETUP GET: sessionId=" + session.getId()
	            + ", userId=" + session.getAttribute("twoFactorUserId"));

	    Long userId = (Long) session.getAttribute("twoFactorUserId");

		if (userId == null) {
			return "redirect:/login";
		}

		Optional<UserMaster> userOpt = userMasterRepository.findById(userId);

		if (userOpt.isEmpty()) {
			session.removeAttribute("twoFactorUserId");
			return "redirect:/login";
		}

		UserMaster user = userOpt.get();

		// TOTP秘密鍵が存在しない場合は2FA設定を続行できない。
		if (user.getTwoFactorSecret() == null
				|| user.getTwoFactorSecret().isBlank()) {
			session.removeAttribute("twoFactorUserId");
			return "redirect:/login";
		}

		String qrCode = twoFactorService.generateQrCode(
				user.getTwoFactorSecret(),
				user.getLoginId());

		model.addAttribute("qrCode", qrCode);

		return "Login/login_2fa_setup";
	}

	/**
	 * 初回のGoogle Authenticator設定を完了する。
	 */
	@PostMapping("/login/2fa/setup")
	public String setup(
			@RequestParam String code,
			HttpSession session,
			Model model) {

		Long userId = (Long) session.getAttribute("twoFactorUserId");

		if (userId == null) {
			return "redirect:/login";
		}

		Optional<UserMaster> userOpt = userMasterRepository.findById(userId);

		if (userOpt.isEmpty()) {
			session.removeAttribute("twoFactorUserId");
			return "redirect:/login";
		}

		UserMaster user = userOpt.get();

		if (!twoFactorService.verifyCode(
				user.getTwoFactorSecret(),
				code)) {

			model.addAttribute(
					"error",
					"認証コードが正しくありません。もう一度入力してください。");

			String qrCode = twoFactorService.generateQrCode(
					user.getTwoFactorSecret(),
					user.getLoginId());

			model.addAttribute("qrCode", qrCode);

			return "Login/login_2fa_setup";
		}

		// 初回2FA認証が成功した日時を記録する。
		user.setTwoFactorAuthenticatedAt(LocalDateTime.now());

		// 初回認証成功時は連続パスワード失敗回数もリセットする。
		user.setLoginFailedCount(0);

		userMasterRepository.save(user);

		completeLogin(user, session);

		return "redirect:/home";
	}

	/**
	 * 既に2FA設定済みのユーザーに認証コード入力画面を表示する。
	 */
	@GetMapping("/login/2fa")
	public String twoFactorForm(HttpSession session) {

		Long userId = (Long) session.getAttribute("twoFactorUserId");

		if (userId == null) {
			return "redirect:/login";
		}

		return "Login/login_2fa";
	}

	/**
	 * 既に設定済みのGoogle Authenticatorコードを検証する。
	 */
	@PostMapping("/login/2fa")
	public String verify(
			@RequestParam String code,
			HttpSession session,
			Model model) {

		Long userId = (Long) session.getAttribute("twoFactorUserId");

		if (userId == null) {
			return "redirect:/login";
		}

		Optional<UserMaster> userOpt = userMasterRepository.findById(userId);

		if (userOpt.isEmpty()) {
			session.removeAttribute("twoFactorUserId");
			return "redirect:/login";
		}

		UserMaster user = userOpt.get();

		if (!twoFactorService.verifyCode(
				user.getTwoFactorSecret(),
				code)) {

			// 2FA認証失敗をユーザーの登録メールアドレスへ通知する。
//			securityMailService.sendTwoFactorFailureNotice(
//					user,
//					(String) session.getAttribute("twoFactorIpAddress"),
//					(String) session.getAttribute("twoFactorRegion"));

			// 認証失敗後は2FA用のセッション情報を破棄してログインを拒否する。
			session.removeAttribute("twoFactorUserId");
			session.removeAttribute("twoFactorIpAddress");
			session.removeAttribute("twoFactorRegion");
			session.removeAttribute("twoFactorUserAgent");

			model.addAttribute(
					"error",
					"2FA認証に失敗しました。セキュリティ上、ログインを中止しました。");

			return "Login/security_alert";
		}

		// 2FA認証成功日時を更新する。
		user.setTwoFactorAuthenticatedAt(LocalDateTime.now());

		// 3回連続のパスワード入力失敗による2FAの場合も、
		// 認証成功後に失敗回数をリセットする。
		user.setLoginFailedCount(0);

		userMasterRepository.save(user);

		completeLogin(user, session);

		return "redirect:/home";
	}

	/**
	 * 2FA認証成功後の通常ログイン処理を完了する。
	 *
	 * LoginControllerと同じログイン完了処理をここでも実行し、
	 * 2FA経由でも通常ログインと同じ状態にする。
	 */
	private void completeLogin(
			UserMaster user,
			HttpSession session) {

		String ipAddress = (String) session.getAttribute("twoFactorIpAddress");

		String region = (String) session.getAttribute("twoFactorRegion");

		String userAgent = (String) session.getAttribute("twoFactorUserAgent");

		// ログイン前に前のセッションのワークスペースをクリアする。
		workspaceService.delete(user.getUserId());

		loginHistoryService.recordLogin(
				user.getUserId(),
				ipAddress,
				region,
				userAgent);

		// 2FA認証完了後に通常のログインセッションを設定する。
		session.setAttribute("loginUser", user);

		// 2FA経由でも投稿済み記事一覧を非同期で先読みする。
		userRepositoryRepository.findByUserId(user.getUserId())
				.ifPresent(repo -> publishedArticleService.syncArticles(
						repo,
						user.getCipherKey(),
						user.getUserId()));

		// 2FA判定用に一時保存していたセッション情報を削除する。
		session.removeAttribute("twoFactorUserId");
		session.removeAttribute("twoFactorIpAddress");
		session.removeAttribute("twoFactorRegion");
		session.removeAttribute("twoFactorUserAgent");
	}
}
