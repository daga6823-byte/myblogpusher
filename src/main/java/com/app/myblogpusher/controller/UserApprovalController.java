
/**
 * ユーザー管理機能を担当するコントローラー
 *
 * 登録済みユーザーの一覧表示とロール変更を担当する。
 * roleが2以上のユーザーだけが他ユーザーの権限を変更できる。
 * ログイン中のユーザー自身は管理対象から除外する。
 */
package com.app.myblogpusher.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.repository.UserMasterRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class UserApprovalController {

	@Autowired
	private UserMasterRepository userMasterRepository;

	/**
	 * ユーザー管理画面を表示する。
	 *
	 * 管理権限を持つユーザーに限り、
	 * 自分以外の登録済みユーザーを一覧表示する。
	 */
	@GetMapping("/admin/users")
	public String showUsers(
			HttpSession session,
			Model model) {

		UserMaster loginUser = getLoginUser(session);

		// ログインしていない場合はログイン画面へ戻す。
		if (loginUser == null) {
			return "redirect:/login";
		}

		// roleが2未満の場合はユーザー管理画面を利用させない。
		if (loginUser.getRole() == null
				|| loginUser.getRole() < 2) {
			return "redirect:/home";
		}

		// ログイン中のユーザー自身を除外する。
		List<UserMaster> users = userMasterRepository.findAll()
				.stream()
				.filter(user -> !user.getUserId()
						.equals(loginUser.getUserId()))
				.toList();

		model.addAttribute("users", users);

		return "admin/user_management";
	}

	/**
	 * 指定したユーザーのロールを変更する。
	 *
	 * roleが2以上のユーザーだけが変更できる。
	 * 自分自身のロール変更は禁止する。
	 */
	@PostMapping("/admin/users/{userId}/role")
	public String updateRole(
			@PathVariable Long userId,
			@RequestParam Integer role,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		UserMaster loginUser = getLoginUser(session);

		// ログインしていない場合はログイン画面へ戻す。
		if (loginUser == null) {
			return "redirect:/login";
		}

		// roleが2未満の場合は変更を拒否する。
		if (loginUser.getRole() == null
				|| loginUser.getRole() < 2) {
			return "redirect:/home";
		}

		// 自分自身のロール変更は禁止する。
		if (loginUser.getUserId().equals(userId)) {
			redirectAttributes.addFlashAttribute(
					"errorMessage",
					"自分自身の権限は変更できません。");

			return "redirect:/admin/users";
		}

		// 設定可能なロールは0・1・2に限定する。
		if (role < 0 || role > 2) {
			redirectAttributes.addFlashAttribute(
					"errorMessage",
					"指定された権限は設定できません。");

			return "redirect:/admin/users";
		}

		UserMaster targetUser = userMasterRepository
				.findById(userId)
				.orElse(null);

		if (targetUser == null) {
			redirectAttributes.addFlashAttribute(
					"errorMessage",
					"対象ユーザーが見つかりません。");

			return "redirect:/admin/users";
		}

		// ユーザーのロールを更新する。
		targetUser.setRole(role);

		// ロール0は承認待ち、それ以外は承認済みとする。
		targetUser.setUserStatus(
				role == 0 ? "PENDING" : "ACTIVE");

		userMasterRepository.save(targetUser);

		redirectAttributes.addFlashAttribute(
				"message",
				"ユーザーの権限を変更しました。");

		return "redirect:/admin/users";
	}

	/**
	 * セッションからログイン中のユーザーを取得する。
	 */
	private UserMaster getLoginUser(HttpSession session) {

		Object loginUser = session.getAttribute("loginUser");

		if (loginUser instanceof UserMaster) {
			return (UserMaster) loginUser;
		}

		return null;
	}
}
