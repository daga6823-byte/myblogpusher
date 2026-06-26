package com.app.myblogpusher.dto;

import java.time.LocalDate;

public class UserRepositoryForm {

	private String repoOwner;
	private String repoName;
	private String accessToken; // 入力時のみ平文。未入力なら「変更しない」扱い
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