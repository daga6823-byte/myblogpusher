/**
 * テンプレートのレイアウト管理を統括するファサード
 *
 * 行・列・セルそれぞれのレイアウト管理サービスへ処理を委譲する。
 * レイアウト管理の呼び出し窓口を一本化する。
 */
package com.app.myblogpusher.facade;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.myblogpusher.entity.Template.TemplateCell;
import com.app.myblogpusher.entity.Template.TemplateColumn;
import com.app.myblogpusher.entity.Template.TemplateRow;
import com.app.myblogpusher.service.Template.TemplateCellLayoutService;
import com.app.myblogpusher.service.Template.TemplateColumnLayoutService;
import com.app.myblogpusher.service.Template.TemplateRowLayoutService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TemplateLayoutFacadeService {

	private final TemplateRowLayoutService templateRowLayoutService;
	private final TemplateColumnLayoutService templateColumnLayoutService;
	private final TemplateCellLayoutService templateCellLayoutService;

	/**
	 * 行情報を取得する。
	 */
	@Transactional(readOnly = true)
	public List<TemplateRow> getRows(Long templateId) {
		return templateRowLayoutService.getRows(templateId);
	}

	/**
	 * 列情報を取得する。
	 */
	@Transactional(readOnly = true)
	public List<TemplateColumn> getColumns(Long templateId) {
		return templateColumnLayoutService.getColumns(templateId);
	}

	/**
	 * セル情報を取得する。
	 */
	@Transactional(readOnly = true)
	public List<TemplateCell> getCells(Long templateId) {
		return templateCellLayoutService.getCells(templateId);
	}

	/**
	 * 行情報を保存する。
	 */
	public TemplateRow saveRow(TemplateRow row) {
		return templateRowLayoutService.saveRow(row);
	}

	/**
	 * 列情報を保存する。
	 */
	public TemplateColumn saveColumn(TemplateColumn column) {
		return templateColumnLayoutService.saveColumn(column);
	}

	/**
	 * セル情報を保存する。
	 */
	public TemplateCell saveCell(TemplateCell cell) {
		return templateCellLayoutService.saveCell(cell);
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
	public TemplateCell mergeCells(
			Long templateId,
			Integer startRow,
			Integer startColumn,
			Integer rowSpan,
			Integer columnSpan) {

		return templateCellLayoutService.mergeCells(
				templateId, startRow, startColumn, rowSpan, columnSpan);
	}

	/**
	 * 結合セルを分割する。
	 */
	public void splitCell(Long templateId, Long cellId) {
		templateCellLayoutService.splitCell(templateId, cellId);
	}

	/**
	 * レイアウト情報をすべて削除する。
	 */
	public void deleteLayout(Long templateId) {
		templateCellLayoutService.deleteLayout(templateId);
	}

	/**
	 * 行を挿入する。
	 */
	public TemplateRow insertRow(Long templateId, Integer rowNumber) {
		return templateRowLayoutService.insertRow(templateId, rowNumber);
	}

	/**
	 * 列を挿入する。
	 */
	public TemplateColumn insertColumn(Long templateId, Integer columnNumber) {
		return templateColumnLayoutService.insertColumn(templateId, columnNumber);
	}
}