/**
 * image_assetテーブルへのアクセスを担当するリポジトリ
 */

package com.app.myblogpusher.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.app.myblogpusher.entity.ImageAsset;

public interface ImageAssetRepository extends JpaRepository<ImageAsset, Long> {

	List<ImageAsset> findByUserIdOrderByUploadDateDesc(Long userId);

	List<ImageAsset> findByUserIdAndCategoryIdOrderByUploadDateDesc(
			Long userId,
			Long categoryId);

	Page<ImageAsset> findByUserIdOrderByUploadDateDesc(
			Long userId,
			Pageable pageable);

	Page<ImageAsset> findByUserIdAndCategoryIdOrderByUploadDateDesc(
			Long userId,
			Long categoryId,
			Pageable pageable);

}