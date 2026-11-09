package com.app.myblogpusher.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.myblogpusher.entity.UserRepositoryEntity;

public interface UserRepositoryRepository extends JpaRepository<UserRepositoryEntity, Long> {

	/**
	 * 指定ユーザーに紐づくGitHubリポジトリ情報を取得する
	 *
	 * @param userId ユーザーID
	 * @return GitHubリポジトリ情報
	 */
	Optional<UserRepositoryEntity> findByUserId(Long userId);
}