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
@Table(name = "english_dictionary")
public class EnglishDictionary {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "japanese", nullable = false, unique = true)
	private String japanese;

	@Column(name = "english", nullable = false)
	private String english;

	@Column(name = "create_user")
	private Long createUser;

	@Column(name = "update_user")
	private Long updateUser;

	@Column(name = "create_date", nullable = false)
	private LocalDateTime createDate;

	@Column(name = "update_date", nullable = false)
	private LocalDateTime updateDate;
}