package com.app.myblogpusher.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.app.myblogpusher.entity.TypoCorrection;

public interface TypoCorrectionRepository extends JpaRepository<TypoCorrection, Long> {
	List<TypoCorrection> findByCategoryIdOrCategoryIdIsNull(Long categoryId);

	boolean existsByCategoryIdAndWrongWordAndCorrectWord(Long categoryId, String wrongWord, String correctWord);

	boolean existsByCategoryIdIsNullAndWrongWordAndCorrectWord(String wrongWord, String correctWord);

	@Query("SELECT t.categoryId, COUNT(t) FROM TypoCorrection t WHERE t.categoryId IN :categoryIds GROUP BY t.categoryId")
	List<Object[]> countByCategoryIds(@Param("categoryIds") List<Long> categoryIds);

	List<TypoCorrection> findByCreateUser(Long userId);

	@Query("""
			SELECT t
			FROM TypoCorrection t
			WHERE t.createUser = :userId
			AND (
				:keyword = ''
				OR t.wrongWord LIKE %:keyword%
				OR t.correctWord LIKE %:keyword%
			)
			AND (
				:categoryId IS NULL
				OR (:categoryId = 0 AND t.categoryId IS NULL)
				OR (:categoryId > 0 AND t.categoryId = :categoryId)
			)
			""")
	Page<TypoCorrection> searchDictionary(
			@Param("userId") Long userId,
			@Param("keyword") String keyword,
			@Param("categoryId") Long categoryId,
			Pageable pageable);

}