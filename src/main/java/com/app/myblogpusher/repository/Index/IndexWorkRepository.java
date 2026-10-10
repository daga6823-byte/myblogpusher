/**
 * Hugoカテゴリーのインデックス編集作業を取得・管理するリポジトリ
 *
 * ユーザーIDとカテゴリー経路のグループIDを基準に、
 * index_workテーブルの検索を担当する。
 */
package com.app.myblogpusher.repository.Index;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.myblogpusher.entity.Index.IndexWork;

public interface IndexWorkRepository extends JpaRepository<IndexWork, Long> {

	/**
	 * ユーザーとカテゴリー経路から編集作業を取得する。
	 */
	Optional<IndexWork> findByUserIdAndGroupId(Long userId, Long groupId);

	/**
	 * ユーザーの編集作業一覧を取得する。
	 */
	java.util.List<IndexWork> findByUserId(Long userId);
}