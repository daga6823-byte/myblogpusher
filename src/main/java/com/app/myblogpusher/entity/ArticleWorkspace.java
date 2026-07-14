package com.app.myblogpusher.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity
@Table(name = "article_workspace")
public class ArticleWorkspace {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "title")
    private String title;

    @Column(name = "content")
    private String content;

    @Column(name = "update_date")
    private LocalDateTime updateDate;

}