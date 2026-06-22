package com.app.myblogpusher.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.app.myblogpusher.entity.ArticleCategory;

public interface ArticleCategoryRepository extends JpaRepository<ArticleCategory, Long> {
	List<ArticleCategory> findByUserId(Long userId);

	Optional<ArticleCategory> findByUserIdAndCategoryName(Long userId, String categoryName);

	@Query("SELECT t.categoryId, COUNT(t) FROM TypoCorrection t WHERE t.categoryId IN :categoryIds GROUP BY t.categoryId")
	List<Object[]> countTypoByCategoryIds(@Param("categoryIds") List<Long> categoryIds);
}