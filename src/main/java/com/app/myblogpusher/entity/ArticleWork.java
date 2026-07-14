package com.app.myblogpusher.entity;

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

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "title")
    private String title;

    @Column(name = "slug")
    private String slug;

    @Column(name = "content")
    private String content;

    @Column(name = "article_id")
    private Long articleId;
    
    @Column(name = "create_date")
    private LocalDateTime createDate;

    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @Column(name = "create_user")
    private Long createUser;

    @Column(name = "update_user")
    private Long updateUser;
    
}