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

}