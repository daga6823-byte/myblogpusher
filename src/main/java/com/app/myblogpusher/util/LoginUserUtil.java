/**
 * セッションからログインユーザーを取得するユーティリティ
 *
 * コントローラーごとに重複していたセッション属性"loginUser"の取り出しを一箇所にまとめる。
 */
package com.app.myblogpusher.util;

import com.app.myblogpusher.entity.UserMaster;

import jakarta.servlet.http.HttpSession;

public final class LoginUserUtil {

	private static final String LOGIN_USER_KEY = "loginUser";

	private LoginUserUtil() {
	}

	/**
	 * セッションからログインユーザーを取得する。
	 *
	 * @return ログインユーザー。未ログインの場合はnull
	 */
	public static UserMaster getLoginUser(HttpSession session) {
		return (UserMaster) session.getAttribute(LOGIN_USER_KEY);
	}
}