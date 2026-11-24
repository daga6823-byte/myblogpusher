/**
 * テンプレートの基本管理を担当するサービス
 *
 * テンプレートの登録・取得・更新・削除を行う。
 * ユーザーごとの所有権を確認し、他ユーザーのテンプレート操作を防止する。
 */
package com.app.myblogpusher.service.Template;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.myblogpusher.entity.Template.Template;
import com.app.myblogpusher.repository.Template.TemplateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TemplateService {

	private final TemplateRepository templateRepository;

	/**
	 * ユーザーのテンプレート一覧を取得する。
	 */
	@Transactional(readOnly = true)
	public List<Template> getTemplates(Long userId) {
		return templateRepository.findByUserIdOrderByUpdateDateDesc(userId);
	}

	/**
	 * ユーザーが所有するテンプレートを取得する。
	 */
	@Transactional(readOnly = true)
	public Template getTemplate(Long userId, Long templateId) {
		Template template = templateRepository.findById(templateId)
				.orElseThrow(() -> new IllegalArgumentException("テンプレートが見つかりません。"));

		if (!template.getUserId().equals(userId)) {
			throw new IllegalArgumentException("テンプレートを操作する権限がありません。");
		}

		return template;
	}

	/**
	 * 新しいテンプレートを登録する。
	 */
	public Template createTemplate(Template template) {

		if (templateRepository.existsByUserIdAndTemplateKey(
				template.getUserId(), template.getTemplateKey())) {
			throw new IllegalArgumentException("テンプレート識別キーが重複しています。");
		}

		LocalDateTime now = LocalDateTime.now();
		template.setCreateDate(now);
		template.setUpdateDate(now);

		return templateRepository.save(template);
	}

	/**
	 * テンプレートの基本情報を更新する。
	 */
	public Template updateTemplate(Long userId, Template template) {

		Template existing = getTemplate(userId, template.getTemplateId());

		if (!existing.getTemplateKey().equals(template.getTemplateKey())
				&& templateRepository.existsByUserIdAndTemplateKey(
						userId, template.getTemplateKey())) {
			throw new IllegalArgumentException("テンプレート識別キーが重複しています。");
		}

		existing.setTemplateName(template.getTemplateName());
		existing.setTemplateKey(template.getTemplateKey());
		existing.setTemplateDescription(template.getTemplateDescription());
		existing.setBackgroundColor(template.getBackgroundColor());
		existing.setUpdateDate(LocalDateTime.now());

		return templateRepository.save(existing);
	}

	/**
	 * テンプレートを削除する。
	 */
	public void deleteTemplate(Long userId, Long templateId) {
		Template template = getTemplate(userId, templateId);
		templateRepository.delete(template);
	}
}