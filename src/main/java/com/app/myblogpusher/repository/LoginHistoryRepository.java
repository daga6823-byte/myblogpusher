/**
 * ログイン履歴の取得・保存を担当するRepository
 *
 * セキュリティ判定に使用する直近のログイン履歴を管理する。
 */

package com.app.myblogpusher.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.myblogpusher.entity.LoginHistory;

@Repository
public interface LoginHistoryRepository
		extends JpaRepository<LoginHistory, Long> {

	List<LoginHistory> findByUserIdOrderByLoginDateDesc(Long userId);
}