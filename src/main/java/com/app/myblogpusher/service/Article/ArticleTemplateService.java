package com.app.myblogpusher.service.Article;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.myblogpusher.entity.Article.ArticleTemplateEntity;
import com.app.myblogpusher.repository.Article.ArticleRepository;
import com.app.myblogpusher.repository.Article.ArticleTemplateRepository;
import com.app.myblogpusher.service.Template.TemplateService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArticleTemplateService {

    private final ArticleTemplateRepository articleTemplateRepository;
    private final ArticleWorkService articleWorkService;
    private final TemplateService templateService;
    private final ArticleRepository articleRepository;

    /**
     * 下書きに紐づくテンプレート全体設定を取得する
     */
    @Transactional(readOnly = true)
    public Optional<ArticleTemplateEntity> findByWorkId(Long userId, Long workId) {
        return articleTemplateRepository.findByUserIdAndWorkId(userId, workId);
    }

    /**
     * 公開記事に紐づくテンプレート全体設定を取得する
     */
    @Transactional(readOnly = true)
    public Optional<ArticleTemplateEntity> findByArticleId(Long userId, Long articleId) {
        return articleTemplateRepository.findByUserIdAndArticleId(userId, articleId);
    }

    /**
     * 下書きに紐づくテンプレート全体設定を保存する
     */
    @Transactional
    public ArticleTemplateEntity saveWorkTemplate(
            Long userId,
            Long workId,
            Long templateId,
            Long backgroundImageAssetId,
            String backgroundColor) {

        if (userId == null || workId == null || templateId == null) {
            throw new IllegalArgumentException("ユーザーID・下書きID・テンプレートIDは必須です。");
        }

        // 下書きの所有者を確認する
        articleWorkService.findById(workId, userId);

        // 使用するマスターテンプレートの所有者を確認する
        templateService.getTemplate(userId, templateId);

        LocalDateTime now = LocalDateTime.now();

        ArticleTemplateEntity entity = articleTemplateRepository
                .findByUserIdAndWorkId(userId, workId)
                .orElseGet(ArticleTemplateEntity::new);

        if (entity.getId() == null) {
            entity.setUserId(userId);
            entity.setWorkId(workId);
            entity.setCreateDate(now);
        }

        entity.setArticleId(null);
        entity.setTemplateId(templateId);
        entity.setBackgroundImageAssetId(backgroundImageAssetId);
        entity.setBackgroundColor(backgroundColor);
        entity.setUpdateDate(now);

        return articleTemplateRepository.save(entity);
    }
    
    /**
     * 公開済み記事に紐づくテンプレート全体設定を保存する
     */
    @Transactional
    public ArticleTemplateEntity saveArticleTemplate(
            Long userId,
            Long articleId,
            Long templateId,
            Long backgroundImageAssetId,
            String backgroundColor) {

        if (userId == null || articleId == null || templateId == null) {
            throw new IllegalArgumentException(
                    "ユーザーID・記事ID・テンプレートIDは必須です。");
        }

        // 記事の所有者を確認する
        articleRepository.findByUserIdAndArticleId(userId, articleId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "指定された記事が存在しないか、アクセス権がありません。"));

        // 使用するマスターテンプレートの所有者を確認する
        templateService.getTemplate(userId, templateId);

        LocalDateTime now = LocalDateTime.now();

        ArticleTemplateEntity entity = articleTemplateRepository
                .findByUserIdAndArticleId(userId, articleId)
                .orElseGet(ArticleTemplateEntity::new);

        if (entity.getId() == null) {
            entity.setUserId(userId);
            entity.setArticleId(articleId);
            entity.setCreateDate(now);
        }

        entity.setWorkId(null);
        entity.setTemplateId(templateId);
        entity.setBackgroundImageAssetId(backgroundImageAssetId);
        entity.setBackgroundColor(backgroundColor);
        entity.setUpdateDate(now);

        return articleTemplateRepository.save(entity);
    }

    /**
     * 公開時に下書きのテンプレート設定を記事へ引き継ぐ
     */
    @Transactional
    public void transferWorkTemplateToArticle(
            Long userId,
            Long workId,
            Long articleId) {

        if (userId == null || workId == null || articleId == null) {
            throw new IllegalArgumentException("ユーザーID・下書きID・記事IDは必須です。");
        }

        Optional<ArticleTemplateEntity> optional = articleTemplateRepository
                .findByUserIdAndWorkId(userId, workId);

        if (optional.isEmpty()) {
            return;
        }

        ArticleTemplateEntity entity = optional.get();

        // 引き継ぎ先の記事に既存設定があれば置き換える
        articleTemplateRepository.deleteByUserIdAndArticleId(userId, articleId);

        entity.setWorkId(null);
        entity.setArticleId(articleId);
        entity.setUpdateDate(LocalDateTime.now());

        articleTemplateRepository.save(entity);
    }
}