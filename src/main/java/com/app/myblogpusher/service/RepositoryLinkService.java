package com.app.myblogpusher.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.repository.UserRepositoryEntityRepository;

@Service
public class RepositoryLinkService {

	@Autowired
	private UserRepositoryEntityRepository repository;

	/**
	 * トークンの保存処理。
	 * 暗号化を導入する際はこのメソッド内でのみ変更する。
	 */
	public void saveToken(Long userId, String repoOwner, String repoName,
			String rawToken, java.time.LocalDate expiresAt) {

		UserRepositoryEntity entity = new UserRepositoryEntity();
		entity.setUserId(userId);
		entity.setRepoOwner(repoOwner);
		entity.setRepoName(repoName);
		entity.setAccessToken(rawToken); // 後で暗号化処理を挟む箇所
		entity.setTokenExpiresAt(expiresAt);
		entity.setCreateUser(userId);
		entity.setUpdateUser(userId);
		entity.setCreateDate(LocalDateTime.now());
		entity.setUpdateDate(LocalDateTime.now());

		repository.save(entity);
	}

	/**
	 * トークンの取得処理。
	 * 暗号化を導入する際はこのメソッド内で復号する。
	 */
	public String getDecryptedToken(UserRepositoryEntity entity) {
		return entity.getAccessToken(); // 後で復号処理を挟む箇所
	}

	public List<UserRepositoryEntity> findByUserId(Long userId) {
		return repository.findByUserId(userId);
	}
}