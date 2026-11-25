
/**
 * テンプレートの列レイアウト管理を担当するサービス
 *
 * 列の取得・保存・追加・削除を行う。
 * 列の追加・削除に伴うセルの配置と結合範囲の調整も担当する。
 */
package com.app.myblogpusher.service.Template;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.myblogpusher.entity.Template.TemplateCellEntity;
import com.app.myblogpusher.entity.Template.TemplateColumn;
import com.app.myblogpusher.repository.Template.TemplateCellRepository;
import com.app.myblogpusher.repository.Template.TemplateColumnRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TemplateColumnLayoutService {

	private final TemplateColumnRepository templateColumnRepository;
	private final TemplateCellRepository templateCellRepository;

	/**
	 * テンプレートの列情報を取得する。
	 */
	@Transactional(readOnly = true)
	public List<TemplateColumn> getColumns(Long templateId) {
		return templateColumnRepository.findByTemplateIdOrderByColumnNumberAsc(templateId);
	}

	/**
	 * 列情報を保存する。
	 */
	public TemplateColumn saveColumn(TemplateColumn column) {

		if (column.getTemplateId() == null || column.getColumnNumber() == null
				|| column.getColumnNumber() < 1) {
			throw new IllegalArgumentException("列情報が不正です。");
		}

		return templateColumnRepository.save(column);
	}

	/**
	 * 指定位置の列を削除する。
	 *
	 * 削除位置にあるセルを削除または横方向に縮小し、
	 * 後続セルと列の番号を前方へ詰める。
	 */
	public void deleteColumn(Long templateId, Integer columnNumber) {

		if (templateId == null || columnNumber == null || columnNumber < 1) {
			throw new IllegalArgumentException("列の削除位置が不正です。");
		}

		List<TemplateColumn> columns = getColumns(templateId);

		if (columnNumber > columns.size()) {
			throw new IllegalArgumentException("列の削除位置が不正です。");
		}

		if (columns.size() <= 1) {
			throw new IllegalArgumentException("最後の1列は削除できません。");
		}

		List<TemplateCellEntity> cells = templateCellRepository.findByTemplateIdOrderByRowNumberAscColumnNumberAsc(
				templateId);

		for (TemplateCellEntity cell : cells) {

			int startColumn = cell.getColumnNumber();
			int endColumn = startColumn + cell.getColumnSpan() - 1;

			if (startColumn == columnNumber) {

				if (cell.getColumnSpan() == 1) {
					templateCellRepository.delete(cell);
				} else {
					cell.setColumnSpan(cell.getColumnSpan() - 1);
					templateCellRepository.saveAndFlush(cell);
				}

			} else if (startColumn < columnNumber && endColumn >= columnNumber) {

				cell.setColumnSpan(cell.getColumnSpan() - 1);
				templateCellRepository.saveAndFlush(cell);

			} else if (startColumn > columnNumber) {

				cell.setColumnNumber(startColumn - 1);
				templateCellRepository.saveAndFlush(cell);
			}
		}

		templateColumnRepository.deleteByTemplateIdAndColumnNumber(
				templateId, columnNumber);

		List<TemplateColumn> targetColumns = getColumns(templateId);

		for (TemplateColumn column : targetColumns) {

			if (column.getColumnNumber() > columnNumber) {
				column.setColumnNumber(column.getColumnNumber() - 1);
				templateColumnRepository.saveAndFlush(column);
			}
		}
	}

	/**
	 * 指定位置に列を挿入する。
	 *
	 * 挿入位置以降の列番号を後方へ移動し、
	 * 挿入位置をまたぐセルは結合範囲を拡張する。
	 */
	public TemplateColumn insertColumn(Long templateId, Integer columnNumber) {

		if (templateId == null || columnNumber == null || columnNumber < 1) {
			throw new IllegalArgumentException("列の挿入位置が不正です。");
		}

		List<TemplateColumn> columns = getColumns(templateId);

		if (columnNumber > columns.size() + 1) {
			throw new IllegalArgumentException("列の挿入位置が不正です。");
		}

		// 後方の列から番号を移動し、番号の重複を防ぐ。
		List<TemplateColumn> targetColumns = templateColumnRepository
				.findByTemplateIdAndColumnNumberGreaterThanEqualOrderByColumnNumberDesc(
						templateId, columnNumber);

		for (TemplateColumn column : targetColumns) {
			column.setColumnNumber(column.getColumnNumber() + 1);
			templateColumnRepository.saveAndFlush(column);
		}

		// セルの配置と結合範囲を調整する。
		List<TemplateCellEntity> cells = templateCellRepository.findByTemplateIdOrderByRowNumberAscColumnNumberAsc(
				templateId);

		for (TemplateCellEntity cell : cells) {

			int startColumn = cell.getColumnNumber();
			int endColumn = startColumn + cell.getColumnSpan() - 1;

			if (startColumn >= columnNumber) {

				cell.setColumnNumber(startColumn + 1);
				templateCellRepository.saveAndFlush(cell);

			} else if (endColumn >= columnNumber) {

				cell.setColumnSpan(cell.getColumnSpan() + 1);
				templateCellRepository.saveAndFlush(cell);
			}
		}

		// 新しい列を登録する。
		TemplateColumn newColumn = new TemplateColumn();
		newColumn.setTemplateId(templateId);
		newColumn.setColumnNumber(columnNumber);
		newColumn.setWidthUnit("fr");

		return templateColumnRepository.save(newColumn);
	}
}
