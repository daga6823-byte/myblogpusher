/**
 * テンプレート情報のDB操作を担当するRepository
 *
 * ユーザーごとのテンプレート情報の登録・取得・更新・削除を行う。
 */
package com.app.myblogpusher.repository.Template;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.myblogpusher.entity.Template.TemplateEntity;

public interface TemplateRepository extends JpaRepository<TemplateEntity, Long> {

	/**
	 * ユーザーが所有するテンプレートを更新日時の降順で取得する。
	 */
	List<TemplateEntity> findByUserIdOrderByUpdateDateDesc(Long userId);

	/**
	 * テンプレート識別キーがユーザー内で既に使用されているか確認する。
	 */
	boolean existsByUserIdAndTemplateKey(Long userId, String templateKey);
}