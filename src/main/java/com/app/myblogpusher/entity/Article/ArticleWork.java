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


@Getter
@Setter
@Entity
@Table(name = "article_work")
public class ArticleWork {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_id")
    private Long workId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "category_group_id")
    private Long categoryGroupId;

    @Column(name = "title")
    private String title;

    @Column(name = "slug")
    private String slug;

    @Column(name = "content")
    private String content;

    @Column(name = "article_id")
    private Long articleId;
    
    @Column(name = "status")
    private Integer status;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @Column(name = "create_date")
    private LocalDateTime createDate;

    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @Column(name = "create_user")
    private Long createUser;

    @Column(name = "update_user")
    private Long updateUser;
    
}