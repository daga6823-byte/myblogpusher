package com.app.myblogpusher.controller;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.app.myblogpusher.dto.UserRepositoryForm;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.repository.UserRepositoryRepository;
import com.app.myblogpusher.service.TokenCipherService;
import com.app.myblogpusher.service.TokenCipherService.EncryptedToken;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class RepositorySettingController {

	private final UserRepositoryRepository userRepositoryRepository;
	private final TokenCipherService tokenCipherService;

	public RepositorySettingController(UserRepositoryRepository userRepositoryRepository,
			TokenCipherService tokenCipherService) {
		this.userRepositoryRepository = userRepositoryRepository;
		this.tokenCipherService = tokenCipherService;
	}

	@GetMapping("/repository/setting")
	public String showSettingForm(Model model, HttpSession session) {
		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		// ログイン状態確認
		if (loginUser == null) {
			return "redirect:/login";
		}

		Long userId = loginUser.getUserId();

		UserRepositoryForm form = new UserRepositoryForm();
		boolean registered = false;

		Optional<UserRepositoryEntity> existing = userRepositoryRepository.findByUserId(userId);
		if (existing.isPresent()) {
			UserRepositoryEntity entity = existing.get();
			form.setRepoOwner(entity.getRepoOwner());
			form.setRepoName(entity.getRepoName());
			form.setTokenExpiresAt(entity.getTokenExpiresAt());
			form.setStorageBaseUrl(entity.getStorageBaseUrl());
			registered = true;
		}

		model.addAttribute("form", form);
		model.addAttribute("registered", registered);
		return "repository_setting";
	}

	@PostMapping("/repository/setting")
	public String saveSetting(@Valid @ModelAttribute("form") UserRepositoryForm form,
			BindingResult bindingResult,
			HttpSession session,
			Model model) {
		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		// ログイン状態確認
		if (loginUser == null) {
			return "redirect:/login";
		}

		Long userId = loginUser.getUserId();

		// 既存レコード取得
		Optional<UserRepositoryEntity> existing = userRepositoryRepository.findByUserId(userId);
		boolean isNew = existing.isEmpty();

		// 新規登録時のアクセストークン必須チェック
		if (isNew && (form.getAccessToken() == null || form.getAccessToken().isBlank())) {
			bindingResult.rejectValue("accessToken", "NotBlank", "新規登録時はアクセストークンが必須です");
		}

		// バリデーションエラーがあれば画面に戻す
		if (bindingResult.hasErrors()) {
			model.addAttribute("registered", !isNew);
			return "repository_setting";
		}

		UserRepositoryEntity entity = existing.orElseGet(UserRepositoryEntity::new);

		entity.setUserId(userId);
		entity.setRepoOwner(form.getRepoOwner());
		entity.setRepoName(form.getRepoName());
		entity.setTokenExpiresAt(form.getTokenExpiresAt());
		entity.setStorageBaseUrl(form.getStorageBaseUrl());

		// トークンが入力された場合のみ暗号化して更新
		if (form.getAccessToken() != null && !form.getAccessToken().isBlank()) {
			try {
				EncryptedToken encrypted = tokenCipherService.encrypt(form.getAccessToken(), loginUser.getCipherKey());
				entity.setAccessToken(encrypted.cipherText());
				entity.setTokenIv(encrypted.iv());
			} catch (Exception e) {
				model.addAttribute("error", "トークンの暗号化に失敗しました");
				model.addAttribute("registered", !isNew);
				return "repository_setting";
			}
		}

		LocalDateTime now = LocalDateTime.now();
		if (isNew) {
			entity.setCreateDate(now);
			entity.setCreateUser(userId);
		}
		entity.setUpdateDate(now);
		entity.setUpdateUser(userId);

		userRepositoryRepository.save(entity);

		return "redirect:/repository/setting?saved";
	}
}