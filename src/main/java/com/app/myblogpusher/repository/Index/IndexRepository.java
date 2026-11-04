/**
 * Hugoカテゴリーのインデックス情報を取得・管理するリポジトリ
 *
 * ユーザーIDとカテゴリーIDを基準に、
 * indexテーブルの検索を担当する。
 */
package com.app.myblogpusher.repository.Index;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.myblogpusher.entity.Index.IndexEntity;

public interface IndexRepository extends JpaRepository<IndexEntity, Long> {

	/**
	 * ユーザーIDとカテゴリーIDからインデックスを取得する。
	 */
	Optional<IndexEntity> findByUserIdAndHugoPath(Long userId, String hugoPath);

	/**
	 * 指定したユーザーのインデックス一覧を取得する。
	 */
	List<IndexEntity> findByUserId(Long userId);

	List<IndexEntity> findByUserIdOrderByUpdateDateDesc(Long userId);

	/**
	 * 指定したHugo上のカテゴリー経路にインデックスが存在するか確認する。
	 */
	boolean existsByUserIdAndHugoPath(Long userId, String hugoPath);

}
