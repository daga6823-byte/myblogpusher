/**
 * image_assetテーブルへのアクセスを担当するリポジトリ
 */

package com.app.myblogpusher.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.app.myblogpusher.entity.ImageAsset;

public interface ImageAssetRepository extends JpaRepository<ImageAsset, Long> {

	List<ImageAsset> findByUserIdOrderByUploadDateDesc(Long userId);

	/**
	 * ユーザーの画像に登録されたフォルダー名を重複なしで取得する。
	 */
	@Query("SELECT DISTINCT i.folderName FROM ImageAsset i "
			+ "WHERE i.userId = :userId "
			+ "AND i.folderName IS NOT NULL "
			+ "AND i.folderName <> '' "
			+ "ORDER BY i.folderName")
	List<String> findDistinctFolderNamesByUserId(
			@Param("userId") Long userId);

	Page<ImageAsset> findByUserIdOrderByUploadDateDesc(
			Long userId,
			Pageable pageable);

	Page<ImageAsset> findByUserIdAndFolderNameOrderByUploadDateDesc(
			Long userId,
			String folderName,
			Pageable pageable);

	List<ImageAsset> findByUserIdAndFolderName(
			Long userId,
			String folderName);

}