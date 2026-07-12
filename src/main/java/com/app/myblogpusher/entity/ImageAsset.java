/**
 * Supabase Storageにアップロードした画像1枚ごとの記録を保持するエンティティ
 * フォルダがSupabase側で見えなくなっても、どのカテゴリー・記事のために
 * どのパスにアップロードしたかをDB側で追跡できるようにする
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
@Table(name = "image_asset")
public class ImageAsset {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "image_id")
	private Long imageId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "category_id")
	private Long categoryId;

	@Column(name = "work_id")
	private Long workId;

	@Column(name = "folder_name", nullable = false)
	private String folderName;

	@Column(name = "file_name", nullable = false)
	private String fileName;

	@Column(name = "storage_path", nullable = false)
	private String storagePath;

	@Column(name = "upload_date")
	private LocalDateTime uploadDate;

	@Column(name = "create_user")
	private Long createUser;

	@Column(name = "update_user")
	private Long updateUser;

	@Column(name = "create_date")
	private LocalDateTime createDate;

	@Column(name = "update_date")
	private LocalDateTime updateDate;
}