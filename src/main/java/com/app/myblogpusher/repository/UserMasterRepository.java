/**
 * ユーザー情報のデータ取得・永続化を担当するRepository
 *
 * ログイン認証、パスワードリセット対象の検索、
 * 暗号化キーが未設定のユーザーの抽出を担当する。
 */
package com.app.myblogpusher.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.myblogpusher.entity.UserMaster;

public interface UserMasterRepository extends JpaRepository<UserMaster, Long> {

	/**
	 * ログインIDからユーザー情報を取得する。
	 *
	 * @param loginId ログインID
	 * @return 該当ユーザー。存在しない場合は空
	 */
	Optional<UserMaster> findByLoginId(String loginId);

	/**
	 * ログインIDとメールアドレスが一致するユーザーを取得する。
	 *
	 * パスワードリセット対象のユーザー確認に使用する。
	 *
	 * @param loginId ログインID
	 * @param email メールアドレス
	 * @return 該当ユーザー。存在しない場合は空
	 */
	Optional<UserMaster> findByLoginIdAndEmail(String loginId, String email);

	/**
	 * 暗号化キーが未設定のユーザーを取得する。
	 *
	 * @return 暗号化キーが未設定のユーザー一覧
	 */
	List<UserMaster> findByCipherKeyIsNull();
}