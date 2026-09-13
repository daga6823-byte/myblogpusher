/**
 * ログイン状態のチェックを担当するInterceptor
 *
 * ログイン済みユーザーだけが通常の画面・処理へ到達できるように、
 * Controllerの実行前にセッションのloginUserを確認する。
 *
 * ログイン元の国籍が登録国籍と異なる場合は、
 * 2FA認証へリダイレクトする。
 *
 * ログイン元regionはログイン処理とは非同期で取得されるため、
 * 取得前（null）の場合はそのまま画面遷移を許可する。
 *
 * ログイン前でも利用するログイン画面・2FA認証処理については、
 * 個別にチェック対象外として扱う。
 */

package com.app.myblogpusher.interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;

import com.app.myblogpusher.entity.LoginHistory;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.service.Login.LoginHistoryService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class LoginCheckInterceptor implements HandlerInterceptor {

	@Autowired
	private LoginHistoryService loginHistoryService;

	@Override
	public boolean preHandle(
			HttpServletRequest request,
			HttpServletResponse response,
			Object handler) throws Exception {

		String requestUri = request.getRequestURI();

		// 2FA認証・初期設定中は、まだloginUserがセッションに存在しないため、
		// ログインチェックの対象外とする。
		if (requestUri.equals("/login/2fa/setup")
				|| requestUri.equals("/login/2fa")) {

			return true;
		}

		HttpSession session = request.getSession(false);

		Object loginUser = session != null
				? session.getAttribute("loginUser")
				: null;

		if (loginUser == null) {

			response.sendRedirect("/login");

			return false; // ここでコントローラーへの到達をブロック
		}

		UserMaster user = (UserMaster) loginUser;

		// すでに今回のログインでregion確認を完了している場合は、
		// 毎回2FAへリダイレクトしない。
		Object regionVerified = session.getAttribute("loginRegionVerified");

		if (Boolean.TRUE.equals(regionVerified)) {
			return true;
		}

		// 最新のログイン履歴を取得する。
		// ログイン直後はregionがnullの場合があるため、
		// 非同期取得が完了するまでは通常どおり画面遷移を許可する。
		LoginHistory history = loginHistoryService.findLatestLogin(
				user.getUserId());

		if (history == null
				|| history.getRegion() == null
				|| history.getRegion().isBlank()) {

			return true;
		}

		// 登録国籍とログイン元国を比較する。
		// 一致している場合は、今回のログインについて確認済みとする。
		if (user.getRegion() != null
				&& user.getRegion().equalsIgnoreCase(history.getRegion())) {

			session.setAttribute("loginRegionVerified", true);

			return true;
		}

		// 登録国籍とログイン元国が異なる場合は2FAを要求する。
		// 現在のloginUserを解除し、2FA認証待ちの状態へ切り替える。
		session.removeAttribute("loginUser");

		session.setAttribute("twoFactorUserId", user.getUserId());
		session.setAttribute("twoFactorIpAddress", history.getIpAddress());
		session.setAttribute("twoFactorRegion", history.getRegion());
		session.setAttribute("twoFactorUserAgent", history.getUserAgent());

		response.sendRedirect("/login/2fa");

		return false;
	}
}