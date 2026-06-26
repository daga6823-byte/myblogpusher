package com.app.myblogpusher.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;


public class UserRepositoryForm {

	@NotBlank(message = "GitHubユーザー名は必須です")
	private String repoOwner;

	@NotBlank(message = "リポジトリ名は必須です")
	private String repoName;

	private String accessToken; // 新規時のみ必須判定はコントローラーで行う

	private LocalDate tokenExpiresAt;

	public String getRepoOwner() {
		return repoOwner;
	}

	public void setRepoOwner(String repoOwner) {
		this.repoOwner = repoOwner;
	}

	public String getRepoName() {
		return repoName;
	}

	public void setRepoName(String repoName) {
		this.repoName = repoName;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public LocalDate getTokenExpiresAt() {
		return tokenExpiresAt;
	}

	public void setTokenExpiresAt(LocalDate tokenExpiresAt) {
		this.tokenExpiresAt = tokenExpiresAt;
	}
}