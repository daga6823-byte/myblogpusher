package com.app.myblogpusher.controller;

import java.time.LocalDateTime;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
		Long userId = loginUser.getUserId(); // UserMasterのID取得メソッド名に合わせて調整してください

		UserRepositoryForm form = new UserRepositoryForm();
		boolean registered = false;

		var existing = userRepositoryRepository.findByUserId(userId);
		if (existing.isPresent()) {
			UserRepositoryEntity entity = existing.get();
			form.setRepoOwner(entity.getRepoOwner());
			form.setRepoName(entity.getRepoName());
			form.setTokenExpiresAt(entity.getTokenExpiresAt());
			registered = true;
		}

		model.addAttribute("form", form);
		model.addAttribute("registered", registered);
		return "repository/setting";
	}

	@PostMapping("/repository/setting")
	public String saveSetting(@ModelAttribute("form") UserRepositoryForm form, HttpSession session) {
		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		UserRepositoryEntity entity = userRepositoryRepository.findByUserId(userId)
				.orElseGet(UserRepositoryEntity::new);

		boolean isNew = entity.getRepoId() == null;

		entity.setUserId(userId);
		entity.setRepoOwner(form.getRepoOwner());
		entity.setRepoName(form.getRepoName());
		entity.setTokenExpiresAt(form.getTokenExpiresAt());

		if (form.getAccessToken() != null && !form.getAccessToken().isBlank()) {
			EncryptedToken encrypted = tokenCipherService.encrypt(form.getAccessToken());
			entity.setAccessToken(encrypted.cipherText());
			entity.setTokenIv(encrypted.iv());
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