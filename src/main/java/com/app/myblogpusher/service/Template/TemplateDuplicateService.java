
/**
 * テンプレートの複製を担当するサービス
 *
 * テンプレート本体・行・列・セルを複製し、
 * 複製先には新しいIDを採番する。
 */
package com.app.myblogpusher.service.Template;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.myblogpusher.entity.Template.TemplateCellEntity;
import com.app.myblogpusher.entity.Template.TemplateColumnEntity;
import com.app.myblogpusher.entity.Template.TemplateEntity;
import com.app.myblogpusher.entity.Template.TemplateRowEntity;
import com.app.myblogpusher.repository.Template.TemplateCellRepository;
import com.app.myblogpusher.repository.Template.TemplateColumnRepository;
import com.app.myblogpusher.repository.Template.TemplateRepository;
import com.app.myblogpusher.repository.Template.TemplateRowRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TemplateDuplicateService {

	private final TemplateRepository templateRepository;
	private final TemplateRowRepository templateRowRepository;
	private final TemplateColumnRepository templateColumnRepository;
	private final TemplateCellRepository templateCellRepository;

	/**
	 * テンプレートをレイアウトごと複製する。
	 */
	public TemplateEntity duplicateTemplate(
			Long userId,
			Long templateId,
			String templateName,
			String templateKey,
			String templateDescription) {

		if (userId == null || templateId == null) {
			throw new IllegalArgumentException("複製対象が不正です。");
		}

		if (templateName == null || templateName.isBlank()
				|| templateKey == null || templateKey.isBlank()) {
			throw new IllegalArgumentException(
					"テンプレート名と識別名を入力してください。");
		}

		if (templateRepository.existsByUserIdAndTemplateKey(
				userId, templateKey.trim())) {
			throw new IllegalArgumentException(
					"このテンプレート識別名は既に使用されています。");
		}

		// 複製元の取得と所有者確認
		TemplateEntity source = templateRepository.findById(templateId)
				.orElseThrow(() -> new IllegalArgumentException(
						"複製対象のテンプレートが存在しません。"));

		if (!userId.equals(source.getUserId())) {
			throw new IllegalArgumentException(
					"このテンプレートを複製する権限がありません。");
		}

		LocalDateTime now = LocalDateTime.now();

		// テンプレート本体を複製
		TemplateEntity target = new TemplateEntity();
		target.setUserId(userId);
		target.setTemplateName(templateName.trim());
		target.setTemplateKey(templateKey.trim());
		target.setTemplateDescription(templateDescription);
		target.setBackgroundColor(source.getBackgroundColor());
		target.setCreateDate(now);
		target.setUpdateDate(now);

		target = templateRepository.save(target);

		Long newTemplateId = target.getTemplateId();

		// 行を複製
		List<TemplateRowEntity> sourceRows = templateRowRepository
				.findByTemplateIdOrderByRowNumberAsc(templateId);

		List<TemplateRowEntity> targetRows = new ArrayList<>();

		for (TemplateRowEntity sourceRow : sourceRows) {

			TemplateRowEntity row = new TemplateRowEntity();
			row.setTemplateId(newTemplateId);
			row.setRowNumber(sourceRow.getRowNumber());
			row.setHeight(sourceRow.getHeight());
			row.setHeightUnit(sourceRow.getHeightUnit());
			row.setCreateDate(now);
			row.setUpdateDate(now);

			targetRows.add(row);
		}

		templateRowRepository.saveAll(targetRows);

		// 列を複製
		List<TemplateColumnEntity> sourceColumns = templateColumnRepository
				.findByTemplateIdOrderByColumnNumberAsc(templateId);

		List<TemplateColumnEntity> targetColumns = new ArrayList<>();

		for (TemplateColumnEntity sourceColumn : sourceColumns) {

			TemplateColumnEntity column = new TemplateColumnEntity();
			column.setTemplateId(newTemplateId);
			column.setColumnNumber(sourceColumn.getColumnNumber());
			column.setWidth(sourceColumn.getWidth());
			column.setWidthUnit(sourceColumn.getWidthUnit());
			column.setCreateDate(now);
			column.setUpdateDate(now);

			targetColumns.add(column);
		}

		templateColumnRepository.saveAll(targetColumns);

		// セルを複製
		List<TemplateCellEntity> sourceCells = templateCellRepository
				.findByTemplateIdOrderByRowNumberAscColumnNumberAsc(templateId);

		List<TemplateCellEntity> targetCells = new ArrayList<>();

		for (TemplateCellEntity sourceCell : sourceCells) {

			TemplateCellEntity cell = new TemplateCellEntity();
			cell.setTemplateId(newTemplateId);
			cell.setRowNumber(sourceCell.getRowNumber());
			cell.setColumnNumber(sourceCell.getColumnNumber());
			cell.setRowSpan(sourceCell.getRowSpan());
			cell.setColumnSpan(sourceCell.getColumnSpan());
			cell.setCellType(sourceCell.getCellType());
			cell.setCellContent(sourceCell.getCellContent());
			cell.setVariableName(sourceCell.getVariableName());
			cell.setImageAssetId(sourceCell.getImageAssetId());
			cell.setImageWidth(sourceCell.getImageWidth());
			cell.setImageHeight(sourceCell.getImageHeight());
			cell.setBackgroundColor(sourceCell.getBackgroundColor());
			cell.setTextColor(sourceCell.getTextColor());
			cell.setTextStrokeColor(sourceCell.getTextStrokeColor());
			cell.setTextStrokeWidth(sourceCell.getTextStrokeWidth());
			cell.setFontSize(sourceCell.getFontSize());
			cell.setFontWeight(sourceCell.getFontWeight());
			cell.setTextAlign(sourceCell.getTextAlign());
			cell.setVerticalAlign(sourceCell.getVerticalAlign());
			cell.setPadding(sourceCell.getPadding());
			cell.setPaddingUnit(sourceCell.getPaddingUnit());
			cell.setBorderConfig(sourceCell.getBorderConfig());
			cell.setCreateDate(now);
			cell.setUpdateDate(now);

			targetCells.add(cell);
		}

		templateCellRepository.saveAll(targetCells);

		return target;
	}
}
