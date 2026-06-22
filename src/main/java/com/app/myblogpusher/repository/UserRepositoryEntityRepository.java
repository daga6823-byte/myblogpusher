package com.app.myblogpusher.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.myblogpusher.entity.UserRepositoryEntity;

public interface UserRepositoryEntityRepository extends JpaRepository<UserRepositoryEntity, Long> {
    List<UserRepositoryEntity> findByUserId(Long userId);
}