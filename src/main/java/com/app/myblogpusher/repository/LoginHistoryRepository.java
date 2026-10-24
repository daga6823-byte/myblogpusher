/**
 * ログイン履歴の取得・保存を担当するRepository
 *
 * セキュリティ判定に使用する直近のログイン履歴を管理する。
 */

package com.app.myblogpusher.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.app.myblogpusher.entity.LoginHistory;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

	List<LoginHistory> findByUserIdOrderByLoginDateDesc(Long userId);

	List<LoginHistory> findTop101ByUserIdOrderByLoginDateDesc(Long userId);

	/**
	 * 指定ユーザーの最新のログイン履歴を1件だけ取得する。
	 */
	Optional<LoginHistory> findFirstByUserIdOrderByLoginDateDesc(Long userId);

	/**
	 * 各ユーザーの最新100件を除き、古いログイン履歴を一括削除する。
	 *
	 * @return 削除件数
	 */
	@Modifying
	@Transactional
	@Query(value = """
			DELETE FROM login_history
			WHERE id IN (
			    SELECT id
			    FROM (
			        SELECT id,
			               ROW_NUMBER() OVER (
			                   PARTITION BY user_id
			                   ORDER BY login_date DESC, id DESC
			               ) AS row_num
			        FROM login_history
			    ) ranked
			    WHERE row_num > 100
			)
			""", nativeQuery = true)
	int deleteOlderThanLatest100PerUser();
}