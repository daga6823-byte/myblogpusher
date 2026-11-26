
/**
 * 記事ごとのテンプレートセル情報を管理するサービス
 *
 * 編集中の記事のセル情報の取得・保存と、
 * 公開時にArticleWorkからArticleへの引き継ぎを担当する。
 *
 * セルの配置や装飾などのマスター情報は変更しない。
 * セル情報はユーザー単位で管理する。
 */
package com.app.myblogpusher.service.Article;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.myblogpusher.entity.Article.ArticleTemplateCellEntity;
import com.app.myblogpusher.repository.Article.ArticleTemplateCellRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArticleTemplateCellService {

    private final ArticleTemplateCellRepository articleTemplateCellRepository;

    /**
     * 編集中の記事のセル情報を取得する
     */
    @Transactional(readOnly = true)
    public List<ArticleTemplateCellEntity> findByWorkId(
            Long userId,
            Long workId) {

        return articleTemplateCellRepository
                .findByUserIdAndWorkIdOrderByIdAsc(userId, workId);
    }

    /**
     * 編集中の記事のセル情報を置き換えて保存する
     *
     * 既存のセル情報を削除してから、
     * 新しいセル情報をユーザー・作業・テンプレートに紐付ける。
     */
    @Transactional
    public void saveWorkCells(
            Long userId,
            Long workId,
            Long templateId,
            List<ArticleTemplateCellEntity> cells) {

        if (userId == null || workId == null) {
            throw new IllegalArgumentException(
                    "ユーザーIDと作業IDは必須です。");
        }

        if (cells == null) {
            throw new IllegalArgumentException(
                    "セル情報が指定されていません。");
        }

        if (!cells.isEmpty() && templateId == null) {
            throw new IllegalArgumentException(
                    "テンプレート未選択の記事にはセル情報を保存できません。");
        }

        // 既存のセル情報を削除する。
        articleTemplateCellRepository
                .deleteByUserIdAndWorkId(userId, workId);

        LocalDateTime now = LocalDateTime.now();

        for (ArticleTemplateCellEntity cell : cells) {

            if (cell == null) {
                throw new IllegalArgumentException(
                        "セル情報にnullが含まれています。");
            }

            // リクエスト側から所有者や関連IDを指定させない。
            cell.setId(null);
            cell.setUserId(userId);
            cell.setWorkId(workId);
            cell.setArticleId(null);
            cell.setTemplateId(templateId);

            cell.setCreateDate(now);
            cell.setUpdateDate(now);
        }

        if (!cells.isEmpty()) {
            articleTemplateCellRepository.saveAll(cells);
        }
    }

    /**
     * 公開済み記事のセル情報を取得する
     */
    @Transactional(readOnly = true)
    public List<ArticleTemplateCellEntity> findByArticleId(
            Long userId,
            Long articleId) {

        return articleTemplateCellRepository
                .findByUserIdAndArticleIdOrderByIdAsc(userId, articleId);
    }

    /**
     * 公開時に編集用セル情報を記事側へ引き継ぐ
     *
     * 既存記事のセル情報を置き換えたうえで、
     * work_idからarticle_idへ所有先を変更する。
     */
    @Transactional
    public void transferWorkCellsToArticle(
            Long userId,
            Long workId,
            Long articleId) {

        List<ArticleTemplateCellEntity> cells =
                articleTemplateCellRepository
                        .findByUserIdAndWorkIdOrderByIdAsc(userId, workId);

        // 既存記事の古いセル情報を削除する。
        articleTemplateCellRepository
                .deleteByUserIdAndArticleId(userId, articleId);

        LocalDateTime now = LocalDateTime.now();

        for (ArticleTemplateCellEntity cell : cells) {
            cell.setWorkId(null);
            cell.setArticleId(articleId);
            cell.setUpdateDate(now);
        }

        // work_idを解除し、article_idへ引き継ぐ。
        if (!cells.isEmpty()) {
            articleTemplateCellRepository.saveAll(cells);
        }
    }

    /**
     * 編集中の記事のセル情報を削除する
     */
    @Transactional
    public void deleteWorkCells(Long userId, Long workId) {
        articleTemplateCellRepository
                .deleteByUserIdAndWorkId(userId, workId);
    }
}
