/**
 * ログイン履歴を管理するエンティティ
 *
 * セキュリティ判定に必要な直近のログイン情報を保持する。
 * 1ユーザーにつき最大2件（現在・直前）の履歴を使用する。
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
@Table(name = "login_history")
public class LoginHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "ip_address")
	private String ipAddress;

	@Column(name = "region")
	private String region;

	@Column(name = "user_agent")
	private String userAgent;

	@Column(name = "login_date", nullable = false)
	private LocalDateTime loginDate;
}