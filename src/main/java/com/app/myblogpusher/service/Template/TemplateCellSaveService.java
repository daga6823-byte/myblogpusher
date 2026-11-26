
/**
 * テンプレートのセル保存を担当するサービス
 *
 * セルの配置位置、結合範囲、既存セルとの重複を検証して保存する。
 */
package com.app.myblogpusher.service.Template;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.myblogpusher.entity.Template.TemplateCellEntity;
import com.app.myblogpusher.entity.Template.TemplateColumnEntity;
import com.app.myblogpusher.entity.Template.TemplateRowEntity;
import com.app.myblogpusher.repository.Template.TemplateCellRepository;
import com.app.myblogpusher.repository.Template.TemplateColumnRepository;
import com.app.myblogpusher.repository.Template.TemplateRowRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TemplateCellSaveService {

	private final TemplateCellRepository templateCellRepository;
	private final TemplateRowRepository templateRowRepository;
	private final TemplateColumnRepository templateColumnRepository;
	private final TemplateCellQueryService templateCellQueryService;

	/**
	 * セル情報を保存する。
	 *
	 * 配置位置、結合範囲、既存セルとの重複を検証してから保存する。
	 */
	public TemplateCellEntity saveCell(TemplateCellEntity cell) {

		if (cell.getTemplateId() == null
				|| cell.getRowNumber() == null
				|| cell.getColumnNumber() == null
				|| cell.getRowSpan() == null
				|| cell.getColumnSpan() == null
				|| cell.getRowNumber() < 1
				|| cell.getColumnNumber() < 1
				|| cell.getRowSpan() < 1
				|| cell.getColumnSpan() < 1) {
			throw new IllegalArgumentException("セルの配置情報が不正です。");
		}

		List<TemplateRowEntity> rows = templateRowRepository
				.findByTemplateIdOrderByRowNumberAsc(cell.getTemplateId());

		List<TemplateColumnEntity> columns = templateColumnRepository
				.findByTemplateIdOrderByColumnNumberAsc(cell.getTemplateId());

		int endRow = cell.getRowNumber() + cell.getRowSpan() - 1;
		int endColumn = cell.getColumnNumber() + cell.getColumnSpan() - 1;

		if (endRow > rows.size() || endColumn > columns.size()) {
			throw new IllegalArgumentException("セルの結合範囲がレイアウトの範囲外です。");
		}

		// 結合範囲内の行番号が実際に存在することを確認する。
		for (int rowNumber = cell.getRowNumber(); rowNumber <= endRow; rowNumber++) {

			final int targetRow = rowNumber;

			if (rows.stream().noneMatch(row -> row.getRowNumber() == targetRow)) {
				throw new IllegalArgumentException("セルの配置先の行が存在しません。");
			}
		}

		// 結合範囲内の列番号が実際に存在することを確認する。
		for (int columnNumber = cell.getColumnNumber(); columnNumber <= endColumn; columnNumber++) {

			final int targetColumn = columnNumber;

			if (columns.stream().noneMatch(
					column -> column.getColumnNumber() == targetColumn)) {
				throw new IllegalArgumentException("セルの配置先の列が存在しません。");
			}
		}

		// 既存セルと結合範囲が重複していないことを確認する。
		List<TemplateCellEntity> existingCells = templateCellQueryService.getCells(cell.getTemplateId());

		for (TemplateCellEntity existing : existingCells) {

			// 更新対象自身は重複判定から除外する。
			if (cell.getCellId() != null
					&& cell.getCellId().equals(existing.getCellId())) {
				continue;
			}

			int existingEndRow = existing.getRowNumber() + existing.getRowSpan() - 1;
			int existingEndColumn = existing.getColumnNumber()
					+ existing.getColumnSpan() - 1;

			boolean rowOverlap = cell.getRowNumber() <= existingEndRow
					&& endRow >= existing.getRowNumber();

			boolean columnOverlap = cell.getColumnNumber() <= existingEndColumn
					&& endColumn >= existing.getColumnNumber();

			if (rowOverlap && columnOverlap) {
				throw new IllegalArgumentException("既存のセルと配置範囲が重複しています。");
			}
		}

		return templateCellRepository.save(cell);
	}
}
