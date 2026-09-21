/**
 * アクセストークン有効期限のお知らせを全画面共通でModelへ設定するAdvice
 *
 * ログインユーザーのuser_repositoryに登録された
 * tokenExpiresAtを確認し、期限が近い場合だけ
 * accessTokenNoticeをModelへ追加する。
 */
package com.app.myblogpusher.handler;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.repository.UserRepositoryRepository;

import jakarta.servlet.http.HttpSession;

@ControllerAdvice
public class AccessTokenNoticeAdvice {

	@Autowired
	private UserRepositoryRepository userRepositoryRepository;

	/**
	 * 全Controller共通でアクセストークンの有効期限を確認する。
	 *
	 * ログインしていない場合や有効期限が設定されていない場合は、
	 * お知らせをModelへ追加しない。
	 */
	@ModelAttribute
	public void addAccessTokenNotice(
			HttpSession session,
			Model model) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			return;
		}

		Optional<UserRepositoryEntity> repository = userRepositoryRepository.findByUserId(
				loginUser.getUserId());

		if (repository.isEmpty()) {
			return;
		}

		LocalDate expiresAt = repository.get().getTokenExpiresAt();

		if (expiresAt == null) {
			return;
		}

		long remainingDays = ChronoUnit.DAYS.between(
				LocalDate.now(),
				expiresAt);

		if (remainingDays <= 7) {

			model.addAttribute(
					"accessTokenNotice",
					new AccessTokenNotice(
							"red",
							remainingDays < 0
									? "お知らせ：アクセストークンの有効期限が切れています"
									: "お知らせ：アクセストークンの有効期限が1週間を切っています"));

		} else if (remainingDays <= 14) {

			model.addAttribute(
					"accessTokenNotice",
					new AccessTokenNotice(
							"yellow",
							"お知らせ：アクセストークンの有効期限が2週間を切っています"));

		} else if (remainingDays <= 30) {

			model.addAttribute(
					"accessTokenNotice",
					new AccessTokenNotice(
							"green",
							"お知らせ：アクセストークンの有効期限が1カ月を切っています"));
		}
	}

	/**
	 * アクセストークン有効期限のお知らせに表示する情報を保持する。
	 *
	 * typeは画面上の表示色、
	 * messageはユーザーへ表示するお知らせ本文を表す。
	 */
	public record AccessTokenNotice(
			String type,
			String message) {
	}
}