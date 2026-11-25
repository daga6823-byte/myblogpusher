
/**
 * テンプレートのセル結合・分割を担当するサービス
 *
 * 指定範囲のセル結合と、結合セルの分割を行う。
 */
package com.app.myblogpusher.service.Template;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.myblogpusher.entity.Template.TemplateCellEntity;
import com.app.myblogpusher.entity.Template.TemplateColumn;
import com.app.myblogpusher.entity.Template.TemplateRow;
import com.app.myblogpusher.repository.Template.TemplateCellRepository;
import com.app.myblogpusher.repository.Template.TemplateColumnRepository;
import com.app.myblogpusher.repository.Template.TemplateRowRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TemplateCellMergeService {

	private final TemplateCellRepository templateCellRepository;
	private final TemplateRowRepository templateRowRepository;
	private final TemplateColumnRepository templateColumnRepository;
	private final TemplateCellQueryService templateCellQueryService;

	/**
	 * 指定範囲のセルを結合する。
	 *
	 * 左上のセルを残し、それ以外のセルを削除する。
	 * 結合後の内容は左上セルの内容を引き継ぐ。
	 */
	public TemplateCellEntity mergeCells(
			Long templateId,
			Integer startRow,
			Integer startColumn,
			Integer rowSpan,
			Integer columnSpan) {

		if (templateId == null || startRow == null || startColumn == null
				|| rowSpan == null || columnSpan == null
				|| startRow < 1 || startColumn < 1
				|| rowSpan < 1 || columnSpan < 1) {
			throw new IllegalArgumentException("セルの結合範囲が不正です。");
		}

		List<TemplateRow> rows = templateRowRepository
				.findByTemplateIdOrderByRowNumberAsc(templateId);

		List<TemplateColumn> columns = templateColumnRepository
				.findByTemplateIdOrderByColumnNumberAsc(templateId);

		int endRow = startRow + rowSpan - 1;
		int endColumn = startColumn + columnSpan - 1;

		if (endRow > rows.size() || endColumn > columns.size()) {
			throw new IllegalArgumentException("結合範囲がレイアウトの範囲外です。");
		}

		// 結合範囲内の行番号が連続して存在することを確認する。
		for (int rowNumber = startRow; rowNumber <= endRow; rowNumber++) {
			final int targetRow = rowNumber;

			if (rows.stream().noneMatch(row -> row.getRowNumber() == targetRow)) {
				throw new IllegalArgumentException("結合範囲内の行が存在しません。");
			}
		}

		// 結合範囲内の列番号が連続して存在することを確認する。
		for (int columnNumber = startColumn; columnNumber <= endColumn; columnNumber++) {
			final int targetColumn = columnNumber;

			if (columns.stream().noneMatch(column -> column.getColumnNumber() == targetColumn)) {
				throw new IllegalArgumentException("結合範囲内の列が存在しません。");
			}
		}

		List<TemplateCellEntity> cells = templateCellQueryService.getCells(templateId);

		TemplateCellEntity baseCell = null;

		// 結合範囲内のセルを確認する。
		for (TemplateCellEntity cell : cells) {

			int cellEndRow = cell.getRowNumber() + cell.getRowSpan() - 1;
			int cellEndColumn = cell.getColumnNumber() + cell.getColumnSpan() - 1;

			boolean rowOverlap = startRow <= cellEndRow
					&& endRow >= cell.getRowNumber();

			boolean columnOverlap = startColumn <= cellEndColumn
					&& endColumn >= cell.getColumnNumber();

			if (!rowOverlap || !columnOverlap) {
				continue;
			}

			// 既に結合されているセルは対象外とする。
			if (cell.getRowSpan() != 1 || cell.getColumnSpan() != 1) {
				throw new IllegalArgumentException(
						"既に結合されているセルを含む範囲は結合できません。");
			}

			if (cell.getRowNumber().equals(startRow)
					&& cell.getColumnNumber().equals(startColumn)) {
				baseCell = cell;
			}
		}

		// 左上セルが存在しない場合は空白セルを作成する。
		if (baseCell == null) {
			baseCell = new TemplateCellEntity();
			baseCell.setTemplateId(templateId);
			baseCell.setRowNumber(startRow);
			baseCell.setColumnNumber(startColumn);
			baseCell.setRowSpan(1);
			baseCell.setColumnSpan(1);
			baseCell.setCellType("EMPTY");
		}

		// 左上以外のセルを削除する。
		for (TemplateCellEntity cell : cells) {

			if (cell.getCellId() == null
					|| cell.getCellId().equals(baseCell.getCellId())) {
				continue;
			}

			int cellEndRow = cell.getRowNumber() + cell.getRowSpan() - 1;
			int cellEndColumn = cell.getColumnNumber() + cell.getColumnSpan() - 1;

			boolean rowOverlap = startRow <= cellEndRow
					&& endRow >= cell.getRowNumber();

			boolean columnOverlap = startColumn <= cellEndColumn
					&& endColumn >= cell.getColumnNumber();

			if (rowOverlap && columnOverlap) {
				templateCellRepository.delete(cell);
			}
		}

		baseCell.setRowSpan(rowSpan);
		baseCell.setColumnSpan(columnSpan);

		return templateCellRepository.save(baseCell);
	}

	/**
	 * 結合セルを分割する。
	 *
	 * 元のセルは左上に残し、その他の位置には空白セルを作成する。
	 */
	public void splitCell(Long templateId, Long cellId) {

		if (templateId == null || cellId == null) {
			throw new IllegalArgumentException("セルの指定が不正です。");
		}

		TemplateCellEntity targetCell = templateCellQueryService
				.getCells(templateId).stream()
				.filter(cell -> cellId.equals(cell.getCellId()))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("対象セルが存在しません。"));

		int startRow = targetCell.getRowNumber();
		int startColumn = targetCell.getColumnNumber();
		int rowSpan = targetCell.getRowSpan();
		int columnSpan = targetCell.getColumnSpan();

		if (rowSpan == 1 && columnSpan == 1) {
			throw new IllegalArgumentException("このセルは結合されていません。");
		}

		// 元のセルを通常サイズに戻す。
		targetCell.setRowSpan(1);
		targetCell.setColumnSpan(1);
		templateCellRepository.saveAndFlush(targetCell);

		// 分割によって空いた位置に空白セルを作成する。
		for (int row = startRow; row < startRow + rowSpan; row++) {
			for (int column = startColumn; column < startColumn + columnSpan; column++) {

				if (row == startRow && column == startColumn) {
					continue;
				}

				TemplateCellEntity emptyCell = new TemplateCellEntity();
				emptyCell.setTemplateId(templateId);
				emptyCell.setRowNumber(row);
				emptyCell.setColumnNumber(column);
				emptyCell.setRowSpan(1);
				emptyCell.setColumnSpan(1);
				emptyCell.setCellType("EMPTY");

				templateCellRepository.save(emptyCell);
			}
		}
	}
}
