/**
 * テンプレートのレイアウト管理を統括するファサード
 *
 * 行・列・セルそれぞれのレイアウト管理サービスへ処理を委譲する。
 * レイアウト管理の呼び出し窓口を一本化する。
 */
package com.app.myblogpusher.facade;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.myblogpusher.entity.Template.TemplateCellEntity;
import com.app.myblogpusher.entity.Template.TemplateColumnEntity;
import com.app.myblogpusher.entity.Template.TemplateRowEntity;
import com.app.myblogpusher.service.Template.TemplateCellMergeService;
import com.app.myblogpusher.service.Template.TemplateCellQueryService;
import com.app.myblogpusher.service.Template.TemplateCellSaveService;
import com.app.myblogpusher.service.Template.TemplateColumnLayoutService;
import com.app.myblogpusher.service.Template.TemplateLayoutDeleteService;
import com.app.myblogpusher.service.Template.TemplateRowLayoutService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TemplateLayoutFacadeService {

	private final TemplateRowLayoutService templateRowLayoutService;
	private final TemplateColumnLayoutService templateColumnLayoutService;
	private final TemplateCellQueryService templateCellQueryService;
	private final TemplateCellSaveService templateCellSaveService;
	private final TemplateCellMergeService templateCellMergeService;
	private final TemplateLayoutDeleteService templateLayoutDeleteService;

	/**
	 * 行情報を取得する。
	 */
	@Transactional(readOnly = true)
	public List<TemplateRowEntity> getRows(Long templateId) {
		return templateRowLayoutService.getRows(templateId);
	}

	/**
	 * 列情報を取得する。
	 */
	@Transactional(readOnly = true)
	public List<TemplateColumnEntity> getColumns(Long templateId) {
		return templateColumnLayoutService.getColumns(templateId);
	}

	/**
	 * セル情報を取得する。
	 */
	@Transactional(readOnly = true)
	public List<TemplateCellEntity> getCells(Long templateId) {
		return templateCellQueryService.getCells(templateId);
	}

	/**
	 * 行情報を保存する。
	 */
	public TemplateRowEntity saveRow(TemplateRowEntity row) {
		return templateRowLayoutService.saveRow(row);
	}

	/**
	 * 行の高さを更新する。
	 */
	public TemplateRowEntity updateRowSize(
			Long templateId,
			Integer rowNumber,
			BigDecimal height,
			String heightUnit) {

		return templateRowLayoutService.updateRowSize(
				templateId, rowNumber, height, heightUnit);
	}

	/**
	 * 隣接する2行の高さを一括更新する。
	 *
	 * 2行分の更新を同一トランザクションで実行する。
	 */
	public void updateRowSizes(
			Long templateId,
			Integer firstRowNumber,
			BigDecimal firstHeight,
			Integer secondRowNumber,
			BigDecimal secondHeight) {

		templateRowLayoutService.updateRowSize(
				templateId,
				firstRowNumber,
				firstHeight,
				"px");

		templateRowLayoutService.updateRowSize(
				templateId,
				secondRowNumber,
				secondHeight,
				"px");
	}

	/**
	 * 列情報を保存する。
	 */
	public TemplateColumnEntity saveColumn(TemplateColumnEntity column) {
		return templateColumnLayoutService.saveColumn(column);
	}

	/**
	 * 列の幅を更新する。
	 */
	public TemplateColumnEntity updateColumnSize(
			Long templateId,
			Integer columnNumber,
			BigDecimal width,
			String widthUnit) {

		return templateColumnLayoutService.updateColumnSize(
				templateId, columnNumber, width, widthUnit);
	}
	
	/**
	 * 隣接する2列の幅を一括更新する。
	 *
	 * 2列分の更新を同一トランザクションで実行する。
	 */
	public void updateColumnSizes(
			Long templateId,
			Integer firstColumnNumber,
			BigDecimal firstWidth,
			Integer secondColumnNumber,
			BigDecimal secondWidth) {

		templateColumnLayoutService.updateColumnSize(
				templateId,
				firstColumnNumber,
				firstWidth,
				"px");

		templateColumnLayoutService.updateColumnSize(
				templateId,
				secondColumnNumber,
				secondWidth,
				"px");
	}

	/**
	 * セル情報を保存する。
	 */
	public TemplateCellEntity saveCell(TemplateCellEntity cell) {
		return templateCellSaveService.saveCell(cell);
	}

	/**
	 * 行を削除する。
	 */
	public void deleteRow(Long templateId, Integer rowNumber) {
		templateRowLayoutService.deleteRow(templateId, rowNumber);
	}

	/**
	 * 列を削除する。
	 */
	public void deleteColumn(Long templateId, Integer columnNumber) {
		templateColumnLayoutService.deleteColumn(templateId, columnNumber);
	}

	/**
	 * セルを結合する。
	 */
	public TemplateCellEntity mergeCells(
			Long templateId,
			Integer startRow,
			Integer startColumn,
			Integer rowSpan,
			Integer columnSpan) {

		return templateCellMergeService.mergeCells(
				templateId, startRow, startColumn, rowSpan, columnSpan);
	}

	/**
	 * 結合セルを分割する。
	 */
	public void splitCell(Long templateId, Long cellId) {
		templateCellMergeService.splitCell(templateId, cellId);
	}

	/**
	 * レイアウト情報をすべて削除する。
	 */
	public void deleteLayout(Long templateId) {
		templateLayoutDeleteService.deleteLayout(templateId);
	}

	/**
	 * 行を挿入する。
	 */
	public TemplateRowEntity insertRow(Long templateId, Integer rowNumber) {
		return templateRowLayoutService.insertRow(templateId, rowNumber);
	}

	/**
	 * 列を挿入する。
	 */
	public TemplateColumnEntity insertColumn(Long templateId, Integer columnNumber) {
		return templateColumnLayoutService.insertColumn(templateId, columnNumber);
	}
}