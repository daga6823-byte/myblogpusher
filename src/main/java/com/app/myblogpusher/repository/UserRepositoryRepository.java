package com.app.myblogpusher.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.myblogpusher.entity.UserRepositoryEntity;

public interface UserRepositoryRepository extends JpaRepository<UserRepositoryEntity, Long> {
    Optional<UserRepositoryEntity> findByUserId(Long userId);
}