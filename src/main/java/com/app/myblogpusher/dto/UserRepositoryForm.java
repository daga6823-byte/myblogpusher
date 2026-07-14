package com.app.myblogpusher.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRepositoryForm {

	@NotBlank(message = "GitHubユーザー名は必須です")
	private String repoOwner;

	@NotBlank(message = "リポジトリ名は必須です")
	private String repoName;

	private String accessToken; // 新規時のみ必須判定はコントローラーで行う

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate tokenExpiresAt;
	
	private String storageBaseUrl;
}