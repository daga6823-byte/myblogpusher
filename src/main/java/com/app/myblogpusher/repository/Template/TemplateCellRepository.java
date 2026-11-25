/**
 * テンプレートのセル情報のDB操作を担当するRepository
 *
 * セルの配置・結合・内容・画像参照・装飾情報を管理する。
 */
package com.app.myblogpusher.repository.Template;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.myblogpusher.entity.Template.TemplateCellEntity;

public interface TemplateCellRepository extends JpaRepository<TemplateCellEntity, Long> {

	/**
	 * テンプレートのセル情報を行番号・列番号の昇順で取得する。
	 */
	List<TemplateCellEntity> findByTemplateIdOrderByRowNumberAscColumnNumberAsc(Long templateId);

	/**
	 * テンプレートに属するセル情報をすべて削除する。
	 */
	void deleteByTemplateId(Long templateId);

	/**
	 * 指定した行番号以降に配置されているセルを取得する。
	 */
	List<TemplateCellEntity> findByTemplateIdAndRowNumberGreaterThanEqual(
			Long templateId, Integer rowNumber);

	/**
	 * 指定した列番号以降に配置されているセルを取得する。
	 */
	List<TemplateCellEntity> findByTemplateIdAndColumnNumberGreaterThanEqual(
			Long templateId, Integer columnNumber);
}