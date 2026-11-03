/**
 * アプリケーションエラーログのDB操作を担当するRepository
 */
package com.app.myblogpusher.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.myblogpusher.entity.ApplicationErrorLog;

@Repository
public interface ApplicationErrorLogRepository
		extends JpaRepository<ApplicationErrorLog, Long> {
}