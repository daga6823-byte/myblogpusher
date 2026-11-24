/**
 * テンプレート同士の親子関係のDB操作を担当するRepository
 *
 * 親テンプレートと子テンプレートの関連情報を管理する。
 */
package com.app.myblogpusher.repository.Template;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.myblogpusher.entity.Template.TemplateRelation;

public interface TemplateRelationRepository extends JpaRepository<TemplateRelation, Long> {

	/**
	 * 指定した親テンプレートに属する子テンプレートとの関係を取得する。
	 */
	List<TemplateRelation> findByParentTemplateId(Long parentTemplateId);

	/**
	 * 指定した子テンプレートの親テンプレートとの関係を取得する。
	 */
	List<TemplateRelation> findByChildTemplateId(Long childTemplateId);

	/**
	 * 指定した親子関係が既に登録されているか確認する。
	 */
	boolean existsByParentTemplateIdAndChildTemplateId(
			Long parentTemplateId, Long childTemplateId);
}