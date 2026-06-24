package com.app.myblogpusher.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.myblogpusher.entity.UserMaster;

public interface UserMasterRepository extends JpaRepository<UserMaster, Long> {
	Optional<UserMaster> findByLoginId(String loginId);

	Optional<UserMaster> findByLoginIdAndEmail(String loginId, String email);
}