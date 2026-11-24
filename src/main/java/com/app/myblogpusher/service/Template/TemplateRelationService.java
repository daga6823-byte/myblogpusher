/**
 * テンプレートの親子関係管理を担当するサービス
 *
 * 親テンプレートと子テンプレートの関係登録・取得・削除を行う。
 */
package com.app.myblogpusher.service.Template;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.myblogpusher.entity.Template.TemplateRelation;
import com.app.myblogpusher.repository.Template.TemplateRelationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TemplateRelationService {

	private final TemplateRelationRepository templateRelationRepository;

	/**
	 * 親テンプレートに紐付く子テンプレートの関係を取得する。
	 */
	@Transactional(readOnly = true)
	public List<TemplateRelation> getChildren(Long templateId) {
		return templateRelationRepository.findByParentTemplateId(templateId);
	}

	/**
	 * 子テンプレートに紐付く親テンプレートの関係を取得する。
	 */
	@Transactional(readOnly = true)
	public List<TemplateRelation> getParents(Long templateId) {
		return templateRelationRepository.findByChildTemplateId(templateId);
	}

	/**
	 * 親子関係を登録する。
	 */
	public TemplateRelation addRelation(Long parentTemplateId, Long childTemplateId) {

		if (parentTemplateId.equals(childTemplateId)) {
			throw new IllegalArgumentException("自分自身を親テンプレートには指定できません。");
		}

		if (templateRelationRepository.existsByParentTemplateIdAndChildTemplateId(
				parentTemplateId, childTemplateId)) {
			throw new IllegalArgumentException("親子関係は既に登録されています。");
		}

		TemplateRelation relation = new TemplateRelation();
		relation.setParentTemplateId(parentTemplateId);
		relation.setChildTemplateId(childTemplateId);

		return templateRelationRepository.save(relation);
	}

	/**
	 * 親子関係を削除する。
	 */
	public void deleteRelation(Long relationId) {
		templateRelationRepository.deleteById(relationId);
	}
}