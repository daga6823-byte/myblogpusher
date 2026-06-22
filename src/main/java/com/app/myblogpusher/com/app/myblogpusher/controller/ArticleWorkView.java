package com.app.myblogpusher.controller;

import java.time.LocalDateTime;

public class ArticleWorkView {

    private final Long workId;
    private final String title;
    private final String categoryName;
    private final String createUser;
    private final String updateUser;
    private final LocalDateTime createDate;
    private final LocalDateTime updateDate;

    public ArticleWorkView(
    		Long workId, String title, String categoryName,
    		String createUser,String updateUser,
    		LocalDateTime createDate, LocalDateTime updateDate) {
        this.workId = workId;
        this.title = title;
        this.categoryName = categoryName;
        this.createUser = createUser;
        this.updateUser = updateUser;
        this.createDate = createDate;
        this.updateDate = updateDate;
    }

    public Long getWorkId() { return workId; }
    public String getTitle() { return title; }
    public String getCategoryName() { return categoryName; }
    public String getCreateUser() { return createUser; }
    public String getUpdateUser() { return updateUser; }
    public LocalDateTime getCreateDate() { return createDate; }
    public LocalDateTime getUpdateDate() { return updateDate; }
}