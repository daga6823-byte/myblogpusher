package com.app.myblogpusher.entity.Article;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 記事ごとのテンプレート全体設定を管理するエンティティ
 *
 * 使用するマスターテンプレートと、
 * 背景画像・背景色などの全体設定を保持する。
 * 個々のセルの設定はArticleTemplateCellEntityで管理する。
 */
@Getter
@Setter
@Entity
@Table(name = "article_template")
public class ArticleTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "work_id")
    private Long workId;

    @Column(name = "article_id")
    private Long articleId;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "background_image_asset_id")
    private Long backgroundImageAssetId;

    @Column(name = "background_color")
    private String backgroundColor;

    @Column(name = "create_date")
    private LocalDateTime createDate;

    @Column(name = "update_date")
    private LocalDateTime updateDate;
}