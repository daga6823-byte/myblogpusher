package com.app.myblogpusher.repository;

import java.util.List;

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
}