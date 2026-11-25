
/**
 * テンプレートのセル情報取得を担当するサービス
 *
 * テンプレートIDをもとにセル情報を取得する。
 */
package com.app.myblogpusher.service.Template;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.myblogpusher.entity.Template.TemplateCellEntity;
import com.app.myblogpusher.repository.Template.TemplateCellRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TemplateCellQueryService {

	private final TemplateCellRepository templateCellRepository;

	/**
	 * テンプレートのセル情報を取得する。
	 */
	public List<TemplateCellEntity> getCells(Long templateId) {
		return templateCellRepository
				.findByTemplateIdOrderByRowNumberAscColumnNumberAsc(templateId);
	}
}
