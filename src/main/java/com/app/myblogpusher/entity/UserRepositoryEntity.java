package com.app.myblogpusher.entity;

import java.time.LocalDate;
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
@Table(name = "user_repository")
public class UserRepositoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "repo_id")
    private Long repoId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "repo_owner", nullable = false)
    private String repoOwner;

    @Column(name = "repo_name", nullable = false)
    private String repoName;

    @Column(name = "access_token", nullable = false)
    private String accessToken;

    @Column(name = "token_iv", nullable = false)
    private String tokenIv;

    @Column(name = "storage_base_url")
    private String storageBaseUrl;

    @Column(name = "token_expires_at")
    private LocalDate tokenExpiresAt;

    @Column(name = "create_date")
    private LocalDateTime createDate;

    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @Column(name = "create_user")
    private Long createUser;

    @Column(name = "update_user")
    private Long updateUser;
}