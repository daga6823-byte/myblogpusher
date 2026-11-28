package com.app.myblogpusher.repository.Article;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.myblogpusher.entity.Article.ArticleTemplateEntity;

public interface ArticleTemplateRepository
        extends JpaRepository<ArticleTemplateEntity, Long> {

    /**
     * 下書きに紐づくテンプレート設定を取得する
     */
    Optional<ArticleTemplateEntity> findByUserIdAndWorkId(
            Long userId, Long workId);

    /**
     * 公開記事に紐づくテンプレート設定を取得する
     */
    Optional<ArticleTemplateEntity> findByUserIdAndArticleId(
            Long userId, Long articleId);

    /**
     * 下書きに紐づくテンプレート設定を削除する
     */
    void deleteByUserIdAndWorkId(Long userId, Long workId);

    /**
     * 公開記事に紐づくテンプレート設定を削除する
     */
    void deleteByUserIdAndArticleId(Long userId, Long articleId);
}