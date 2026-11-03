/**
 * アプリケーションで発生したエラー情報を保持するEntity
 *
 * 例外の発生状況や原因を後から調査できるよう、
 * ユーザー、リクエスト、例外の詳細情報を記録する。
 */
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
@Table(name = "application_error_log")
public class ApplicationErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "request_uri", length = 1000)
    private String requestUri;

    @Column(name = "http_method", length = 20)
    private String httpMethod;

    @Column(name = "exception_class", nullable = false, length = 500)
    private String exceptionClass;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(name = "create_date", nullable = false)
    private LocalDateTime createDate;
}