/**
 * テンプレートの列情報のDB操作を担当するRepository
 *
 * テンプレートごとの列番号や列の幅を管理する。
 */
package com.app.myblogpusher.repository.Template;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.myblogpusher.entity.Template.TemplateColumn;

public interface TemplateColumnRepository extends JpaRepository<TemplateColumn, Long> {

	/**
	 * テンプレートの列情報を列番号の昇順で取得する。
	 */
	List<TemplateColumn> findByTemplateIdOrderByColumnNumberAsc(Long templateId);

	/**
	 * 指定した列番号以降の列情報を列番号の降順で取得する。
	 *
	 * 途中挿入時に後方の列から番号を振り直すために使用する。
	 */
	List<TemplateColumn> findByTemplateIdAndColumnNumberGreaterThanEqualOrderByColumnNumberDesc(
			Long templateId, Integer columnNumber);

	/**
	 * テンプレートに属する列情報をすべて削除する。
	 */
	void deleteByTemplateId(Long templateId);

	/**
	 * 指定した列番号の列情報を削除する。
	 */
	void deleteByTemplateIdAndColumnNumber(Long templateId, Integer columnNumber);
}