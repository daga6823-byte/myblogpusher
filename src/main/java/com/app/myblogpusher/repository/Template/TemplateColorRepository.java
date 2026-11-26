/**
 * テンプレートの色履歴のDB操作を担当するRepository
 *
 * ユーザーごとに使用したカラーコードを管理する。
 */
package com.app.myblogpusher.repository.Template;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.myblogpusher.entity.Template.TemplateColorEntity;

public interface TemplateColorRepository extends JpaRepository<TemplateColorEntity, Long> {

	/**
	 * ユーザーの色履歴を登録日時の降順で取得する。
	 */
	List<TemplateColorEntity> findByUserIdOrderByCreateDateDesc(Long userId);

	/**
	 * ユーザーが同じカラーコードを既に登録しているか確認する。
	 *
	 * 大文字・小文字を区別しない。
	 */
	boolean existsByUserIdAndColorCodeIgnoreCase(Long userId, String colorCode);
}