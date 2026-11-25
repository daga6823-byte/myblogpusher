/**
 * テンプレートのレイアウト管理を担当するサービス
 *
 * 行・列・セルの登録・取得・更新・削除を行う。
 * セルの配置や結合範囲の整合性検証も担当する。
 */
package com.app.myblogpusher.service.Template;

import java.util.Comparator;
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
public class TemplateLayoutService {

	private final TemplateRowRepository templateRowRepository;
	private final TemplateColumnRepository templateColumnRepository;
	private final TemplateCellRepository templateCellRepository;

	/**
	 * テンプレートの行情報を取得する。
	 */
	@Transactional(readOnly = true)
	public List<TemplateRow> getRows(Long templateId) {
		return templateRowRepository.findByTemplateIdOrderByRowNumberAsc(templateId);
	}

	/**
	 * テンプレートの列情報を取得する。
	 */
	@Transactional(readOnly = true)
	public List<TemplateColumn> getColumns(Long templateId) {
		return templateColumnRepository.findByTemplateIdOrderByColumnNumberAsc(templateId);
	}

	/**
	 * テンプレートのセル情報を取得する。
	 */
	@Transactional(readOnly = true)
	public List<TemplateCellEntity> getCells(Long templateId) {
		return templateCellRepository
				.findByTemplateIdOrderByRowNumberAscColumnNumberAsc(templateId);
	}

	/**
	 * 行情報を保存する。
	 *
	 * テンプレートIDと行番号の入力を検証してから保存する。
	 */
	public TemplateRow saveRow(TemplateRow row) {

		if (row.getTemplateId() == null || row.getRowNumber() == null
				|| row.getRowNumber() < 1) {
			throw new IllegalArgumentException("行情報が不正です。");
		}

		return templateRowRepository.save(row);
	}

	/**
	 * 列情報を保存する。
	 *
	 * テンプレートIDと列番号の入力を検証してから保存する。
	 */
	public TemplateColumn saveColumn(TemplateColumn column) {

		if (column.getTemplateId() == null || column.getColumnNumber() == null
				|| column.getColumnNumber() < 1) {
			throw new IllegalArgumentException("列情報が不正です。");
		}

		return templateColumnRepository.save(column);
	}

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

		List<TemplateRow> rows = getRows(cell.getTemplateId());
		List<TemplateColumn> columns = getColumns(cell.getTemplateId());

		int endRow = cell.getRowNumber() + cell.getRowSpan() - 1;
		int endColumn = cell.getColumnNumber() + cell.getColumnSpan() - 1;

		if (endRow > rows.size() || endColumn > columns.size()) {
			throw new IllegalArgumentException("セルの結合範囲がレイアウトの範囲外です。");
		}

		// 結合範囲内の行番号・列番号が実際に存在することを確認する。
		for (int rowNumber = cell.getRowNumber(); rowNumber <= endRow; rowNumber++) {
			final int targetRow = rowNumber;

			if (rows.stream().noneMatch(row -> row.getRowNumber() == targetRow)) {
				throw new IllegalArgumentException("セルの配置先の行が存在しません。");
			}
		}

		for (int columnNumber = cell.getColumnNumber(); columnNumber <= endColumn; columnNumber++) {

			final int targetColumn = columnNumber;

			if (columns.stream().noneMatch(column -> column.getColumnNumber() == targetColumn)) {
				throw new IllegalArgumentException("セルの配置先の列が存在しません。");
			}
		}

		// 既存セルと結合範囲が重複していないことを確認する。
		List<TemplateCellEntity> existingCells = getCells(cell.getTemplateId());

		for (TemplateCellEntity existing : existingCells) {

			// 更新対象自身は重複判定から除外する。
			if (cell.getCellId() != null
					&& cell.getCellId().equals(existing.getCellId())) {
				continue;
			}

			int existingEndRow = existing.getRowNumber() + existing.getRowSpan() - 1;

			int existingEndColumn = existing.getColumnNumber() + existing.getColumnSpan() - 1;

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

	/**
	 * 指定位置の行を削除する。
	 *
	 * 削除位置にあるセルを削除または縦方向に縮小し、
	 * 後続セルの行番号を前方へ詰める。
	 *
	 * @param templateId テンプレートID
	 * @param rowNumber 削除する行番号
	 */
	public void deleteRow(Long templateId, Integer rowNumber) {

		List<TemplateRow> rows = getRows(templateId);

		if (templateId == null || rowNumber == null
				|| rowNumber < 1 || rowNumber > rows.size()) {
			throw new IllegalArgumentException("行の削除位置が不正です。");
		}

		if (rows.size() <= 1) {
			throw new IllegalArgumentException("最後の1行は削除できません。");
		}

		List<TemplateCellEntity> cells = getCells(templateId);

		for (TemplateCellEntity cell : cells) {

			int startRow = cell.getRowNumber();
			int endRow = startRow + cell.getRowSpan() - 1;

			if (startRow == rowNumber) {

				if (cell.getRowSpan() == 1) {

					// 削除対象行にある通常セルを削除する。
					templateCellRepository.delete(cell);

				} else {

					// 削除対象行から始まる結合セルを縮小する。
					cell.setRowSpan(cell.getRowSpan() - 1);
					templateCellRepository.saveAndFlush(cell);
				}

			} else if (startRow < rowNumber && endRow >= rowNumber) {

				// 削除位置をまたぐ結合セルを縮小する。
				cell.setRowSpan(cell.getRowSpan() - 1);
				templateCellRepository.saveAndFlush(cell);

			} else if (startRow > rowNumber) {

				// 後続セルの配置位置を前方へ詰める。
				cell.setRowNumber(startRow - 1);
				templateCellRepository.saveAndFlush(cell);
			}
		}

		// セルの調整後に対象行を削除する。
		templateRowRepository.deleteByTemplateIdAndRowNumber(
				templateId, rowNumber);

		// 後続の行番号を前方へ詰める。
		List<TemplateRow> targetRows = getRows(templateId);

		for (TemplateRow row : targetRows) {

			if (row.getRowNumber() > rowNumber) {
				row.setRowNumber(row.getRowNumber() - 1);
				templateRowRepository.saveAndFlush(row);
			}
		}
	}

	/**
	 * 指定位置の列を削除する。
	 *
	 * 削除位置にあるセルを削除または横方向に縮小し、
	 * 後続セルの列番号を前方へ詰める。
	 *
	 * @param templateId テンプレートID
	 * @param columnNumber 削除する列番号
	 */
	public void deleteColumn(Long templateId, Integer columnNumber) {

		List<TemplateColumn> columns = getColumns(templateId);

		if (templateId == null || columnNumber == null
				|| columnNumber < 1 || columnNumber > columns.size()) {
			throw new IllegalArgumentException("列の削除位置が不正です。");
		}

		if (columns.size() <= 1) {
			throw new IllegalArgumentException("最後の1列は削除できません。");
		}

		List<TemplateCellEntity> cells = getCells(templateId);

		for (TemplateCellEntity cell : cells) {

			int startColumn = cell.getColumnNumber();
			int endColumn = startColumn + cell.getColumnSpan() - 1;

			if (startColumn == columnNumber) {

				if (cell.getColumnSpan() == 1) {

					// 削除対象列にある通常セルを削除する。
					templateCellRepository.delete(cell);

				} else {

					// 削除対象列から始まる結合セルを縮小する。
					cell.setColumnSpan(cell.getColumnSpan() - 1);
					templateCellRepository.saveAndFlush(cell);
				}

			} else if (startColumn < columnNumber
					&& endColumn >= columnNumber) {

				// 削除位置をまたぐ結合セルを縮小する。
				cell.setColumnSpan(cell.getColumnSpan() - 1);
				templateCellRepository.saveAndFlush(cell);

			} else if (startColumn > columnNumber) {

				// 後続セルの配置位置を前方へ詰める。
				cell.setColumnNumber(startColumn - 1);
				templateCellRepository.saveAndFlush(cell);
			}
		}

		// セルの調整後に対象列を削除する。
		templateColumnRepository.deleteByTemplateIdAndColumnNumber(
				templateId, columnNumber);

		// 後続の列番号を前方へ詰める。
		List<TemplateColumn> targetColumns = getColumns(templateId);

		for (TemplateColumn column : targetColumns) {

			if (column.getColumnNumber() > columnNumber) {
				column.setColumnNumber(column.getColumnNumber() - 1);
				templateColumnRepository.saveAndFlush(column);
			}
		}
	}

	/**
	 * 指定範囲のセルを結合する。
	 *
	 * 左上のセルを残し、それ以外のセルを削除する。
	 * 結合後の内容は左上セルの内容を引き継ぐ。
	 *
	 * @param templateId テンプレートID
	 * @param startRow 開始行
	 * @param startColumn 開始列
	 * @param rowSpan 結合する行数
	 * @param columnSpan 結合する列数
	 * @return 結合後のセル
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

		List<TemplateRow> rows = getRows(templateId);
		List<TemplateColumn> columns = getColumns(templateId);

		int endRow = startRow + rowSpan - 1;
		int endColumn = startColumn + columnSpan - 1;

		if (endRow > rows.size() || endColumn > columns.size()) {
			throw new IllegalArgumentException("結合範囲がレイアウトの範囲外です。");
		}

		List<TemplateCellEntity> cells = getCells(templateId);

		TemplateCellEntity baseCell = null;

		// 結合範囲内のセルを確認する。
		for (TemplateCellEntity cell : cells) {

			int cellEndRow = cell.getRowNumber() + cell.getRowSpan() - 1;
			int cellEndColumn = cell.getColumnNumber() + cell.getColumnSpan() - 1;

			boolean rowOverlap = startRow <= cellEndRow && endRow >= cell.getRowNumber();

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

			boolean rowOverlap = startRow <= cellEndRow && endRow >= cell.getRowNumber();

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
	 *
	 * @param templateId テンプレートID
	 * @param cellId 分割対象セルID
	 */
	public void splitCell(Long templateId, Long cellId) {

		if (templateId == null || cellId == null) {
			throw new IllegalArgumentException("セルの指定が不正です。");
		}

		TemplateCellEntity targetCell = getCells(templateId).stream()
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

	/**
	 * テンプレートのレイアウト情報をすべて削除する。
	 */
	public void deleteLayout(Long templateId) {
		templateCellRepository.deleteByTemplateId(templateId);
		templateRowRepository.deleteByTemplateId(templateId);
		templateColumnRepository.deleteByTemplateId(templateId);
	}

	/**
	 * 指定位置に行を挿入する。
	 *
	 * 挿入位置以降の行番号を後方へ移動し、
	 * 挿入位置をまたぐセルは結合範囲を拡張する。
	 *
	 * @param templateId テンプレートID
	 * @param rowNumber 挿入する行番号
	 */
	public TemplateRow insertRow(Long templateId, Integer rowNumber) {

		List<TemplateRow> rows = getRows(templateId);

		if (templateId == null || rowNumber == null
				|| rowNumber < 1 || rowNumber > rows.size() + 1) {
			throw new IllegalArgumentException("行の挿入位置が不正です。");
		}

		// 後方の行から番号を移動する。
		List<TemplateRow> targetRows = templateRowRepository
				.findByTemplateIdAndRowNumberGreaterThanEqualOrderByRowNumberDesc(
						templateId, rowNumber);

		for (TemplateRow row : targetRows) {
			row.setRowNumber(row.getRowNumber() + 1);
			templateRowRepository.saveAndFlush(row);
		}

		// セルの配置と結合範囲を調整する。
		List<TemplateCellEntity> cells = getCells(templateId);

		cells.sort(
				Comparator.comparing(TemplateCellEntity::getRowNumber)
						.reversed()
						.thenComparing(
								TemplateCellEntity::getColumnNumber,
								Comparator.reverseOrder()));

		for (TemplateCellEntity cell : cells) {

			int startRow = cell.getRowNumber();
			int endRow = startRow + cell.getRowSpan() - 1;

			if (startRow >= rowNumber) {

				// 挿入位置以降にあるセルは配置位置を移動する。
				cell.setRowNumber(startRow + 1);

			} else if (endRow >= rowNumber) {

				// 挿入位置をまたぐ結合セルは縦方向に拡張する。
				cell.setRowSpan(cell.getRowSpan() + 1);
			}

			if (startRow >= rowNumber || endRow >= rowNumber) {
				templateCellRepository.saveAndFlush(cell);
			}
		}

		// 新しい行を登録する。
		TemplateRow newRow = new TemplateRow();
		newRow.setTemplateId(templateId);
		newRow.setRowNumber(rowNumber);
		newRow.setHeightUnit("auto");

		return templateRowRepository.save(newRow);
	}

	/**
	 * 指定位置に列を挿入する。
	 *
	 * 挿入位置以降の列番号を後方へ移動し、
	 * 挿入位置をまたぐセルは結合範囲を拡張する。
	 *
	 * @param templateId テンプレートID
	 * @param columnNumber 挿入する列番号
	 */
	public TemplateColumn insertColumn(Long templateId, Integer columnNumber) {

		List<TemplateColumn> columns = getColumns(templateId);

		if (templateId == null || columnNumber == null
				|| columnNumber < 1 || columnNumber > columns.size() + 1) {
			throw new IllegalArgumentException("列の挿入位置が不正です。");
		}

		// 後方の列から番号を移動する。
		List<TemplateColumn> targetColumns = templateColumnRepository
				.findByTemplateIdAndColumnNumberGreaterThanEqualOrderByColumnNumberDesc(
						templateId, columnNumber);

		for (TemplateColumn column : targetColumns) {
			column.setColumnNumber(column.getColumnNumber() + 1);
			templateColumnRepository.saveAndFlush(column);
		}

		// セルの配置と結合範囲を調整する。
		List<TemplateCellEntity> cells = getCells(templateId);

		cells.sort(
				Comparator.comparing(TemplateCellEntity::getColumnNumber)
						.reversed()
						.thenComparing(
								TemplateCellEntity::getRowNumber,
								Comparator.reverseOrder()));

		for (TemplateCellEntity cell : cells) {

			int startColumn = cell.getColumnNumber();
			int endColumn = startColumn + cell.getColumnSpan() - 1;

			if (startColumn >= columnNumber) {

				// 挿入位置以降にあるセルは配置位置を移動する。
				cell.setColumnNumber(startColumn + 1);

			} else if (endColumn >= columnNumber) {

				// 挿入位置をまたぐ結合セルは横方向に拡張する。
				cell.setColumnSpan(cell.getColumnSpan() + 1);
			}

			if (startColumn >= columnNumber || endColumn >= columnNumber) {
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