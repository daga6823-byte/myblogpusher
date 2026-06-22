package com.app.myblogpusher.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "typo_correction")
public class TypoCorrection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "typo_id")
    private Long typoId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "wrong_word", nullable = false)
    private String wrongWord;

    @Column(name = "correct_word", nullable = false)
    private String correctWord;

    @Column(name = "create_date")
    private LocalDateTime createDate;

    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @Column(name = "create_user")
    private Long createUser;

    @Column(name = "update_user")
    private Long updateUser;

    public Long getTypoId() { return typoId; }
    public void setTypoId(Long typoId) { this.typoId = typoId; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getWrongWord() { return wrongWord; }
    public void setWrongWord(String wrongWord) { this.wrongWord = wrongWord; }

    public String getCorrectWord() { return correctWord; }
    public void setCorrectWord(String correctWord) { this.correctWord = correctWord; }

    public Long getCreateUser() { return createUser; }
    public void setCreateUser(Long createUser) { this.createUser = createUser; }

    public Long getUpdateUser() { return updateUser; }
    public void setUpdateUser(Long updateUser) { this.updateUser = updateUser; }
    
    public LocalDateTime getCreateDate() { return createDate; }
    public void setCreateDate(LocalDateTime createDate) { this.createDate = createDate; }

    public LocalDateTime getUpdateDate() { return updateDate; }
    public void setUpdateDate(LocalDateTime updateDate) { this.updateDate = updateDate; }

}