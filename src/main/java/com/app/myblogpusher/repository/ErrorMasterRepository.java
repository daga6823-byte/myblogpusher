package com.app.myblogpusher.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.myblogpusher.entity.ErrorMaster;

@Repository
public interface ErrorMasterRepository extends JpaRepository<ErrorMaster, Long> {

	Optional<ErrorMaster> findByErrorCodeAndEnabledTrue(String errorCode);
}