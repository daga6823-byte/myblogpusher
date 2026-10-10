/**
 * Hugoカテゴリーのインデックス情報を取得・管理するリポジトリ
 *
 * ユーザーIDとカテゴリー経路のグループIDを基準に、
 * indexテーブルの検索を担当する。
 */
package com.app.myblogpusher.repository.Index;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.myblogpusher.entity.Index.IndexEntity;

public interface IndexRepository extends JpaRepository<IndexEntity, Long> {

	/**
	 * ユーザーIDとカテゴリー経路のグループIDからインデックスを取得する。
	 */
	Optional<IndexEntity> findByUserIdAndGroupId(Long userId, Long groupId);

	/**
	 * 指定したユーザーのインデックス一覧を取得する。
	 */
	List<IndexEntity> findByUserId(Long userId);

	List<IndexEntity> findByUserIdOrderByUpdateDateDesc(Long userId);

	/**
	 * 指定したカテゴリー経路のグループIDにインデックスが存在するか確認する。
	 */
	boolean existsByUserIdAndGroupId(Long userId, Long groupId);

}
