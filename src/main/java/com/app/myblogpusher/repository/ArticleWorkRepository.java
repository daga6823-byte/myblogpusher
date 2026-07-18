package com.app.myblogpusher.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.myblogpusher.entity.ArticleWork;

public interface ArticleWorkRepository extends JpaRepository<ArticleWork, Long> {
	List<ArticleWork> findByUserIdOrderByUpdateDateDesc(Long userId);
    
    Optional<ArticleWork> findByUserIdAndCategoryIdAndTitleAndContent(
    	    Long userId, Long categoryId, String title, String content);
    
    Optional<ArticleWork> findBySlug(String slug);
    
}