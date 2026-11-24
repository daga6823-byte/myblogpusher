/**
 * テンプレートの行情報のDB操作を担当するRepository
 *
 * テンプレートごとの行番号や行の高さを管理する。
 */
package com.app.myblogpusher.repository.Template;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.myblogpusher.entity.Template.TemplateRow;

public interface TemplateRowRepository extends JpaRepository<TemplateRow, Long> {

	/**
	 * テンプレートの行情報を行番号の昇順で取得する。
	 */
	List<TemplateRow> findByTemplateIdOrderByRowNumberAsc(Long templateId);

	/**
	 * 指定した行番号以降の行情報を行番号の降順で取得する。
	 *
	 * 途中挿入時に後方の行から番号を振り直すために使用する。
	 */
	List<TemplateRow> findByTemplateIdAndRowNumberGreaterThanEqualOrderByRowNumberDesc(
			Long templateId, Integer rowNumber);

	/**
	 * 指定したテンプレートの行情報をすべて削除する。
	 */
	void deleteByTemplateId(Long templateId);

	/**
	 * 指定した行番号の行情報を削除する。
	 */
	void deleteByTemplateIdAndRowNumber(Long templateId, Integer rowNumber);
}