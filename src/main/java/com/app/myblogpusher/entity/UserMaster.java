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
@Table(name = "user_master")
public class UserMaster {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Long userId;

	@Column(name = "login_id", nullable = false, unique = true)
	private String loginId;

	@Column(name = "password", nullable = false)
	private String password;

	@Column(name = "user_name", nullable = false)
	private String userName;

	@Column(name = "email")
	private String email;

	@Column(name = "reg_ip")
	private String regIp;

	@Column(name = "reg_port")
	private Integer regPort;

	@Column(name = "reg_hostname")
	private String regHostname;

	@Column(name = "user_agent")
	private String userAgent;

	@Column(name = "create_date")
	private LocalDateTime createDate;

	@Column(name = "update_date")
	private LocalDateTime updateDate;

	@Column(name = "create_user")
	private Long createUser;

	@Column(name = "update_user")
	private Long updateUser;

	@Column(name = "role")
	private Integer role;

	@Column(name = "status")
	private String status;

	@Column(name = "cipher_key")
	private String cipherKey;
}