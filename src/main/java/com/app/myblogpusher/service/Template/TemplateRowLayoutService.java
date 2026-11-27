
/**
 * テンプレートの行レイアウト管理を担当するサービス
 *
 * 行の取得・保存・追加・削除を行う。
 * 行の追加・削除に伴うセルの配置と結合範囲の調整も担当する。
 */
package com.app.myblogpusher.service.Template;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.myblogpusher.entity.Template.TemplateCellEntity;
import com.app.myblogpusher.entity.Template.TemplateRowEntity;
import com.app.myblogpusher.repository.Template.TemplateCellRepository;
import com.app.myblogpusher.repository.Template.TemplateRowRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TemplateRowLayoutService {

	private final TemplateRowRepository templateRowRepository;
	private final TemplateCellRepository templateCellRepository;

	/**
	 * テンプレートの行情報を取得する。
	 */
	@Transactional(readOnly = true)
	public List<TemplateRowEntity> getRows(Long templateId) {
		return templateRowRepository.findByTemplateIdOrderByRowNumberAsc(templateId);
	}

	/**
	 * 行情報を保存する。
	 */
	public TemplateRowEntity saveRow(TemplateRowEntity row) {

		if (row.getTemplateId() == null || row.getRowNumber() == null
				|| row.getRowNumber() < 1) {
			throw new IllegalArgumentException("行情報が不正です。");
		}

		return templateRowRepository.save(row);
	}

	/**
	 * 指定した行の高さを更新する。
	 *
	 * 高さがNULLの場合はautoに戻す。
	 */
	public TemplateRowEntity updateRowSize(
			Long templateId,
			Integer rowNumber,
			BigDecimal height,
			String heightUnit) {

		if (templateId == null || rowNumber == null || rowNumber < 1) {
			throw new IllegalArgumentException("行の指定が不正です。");
		}

		if (height == null) {
			heightUnit = "auto";
		} else {
			if (height.compareTo(BigDecimal.ZERO) <= 0) {
				throw new IllegalArgumentException("行の高さは0より大きい値を指定してください。");
			}

			if (heightUnit == null
					|| !List.of("px", "%", "rem", "em", "vw", "vh", "fr")
							.contains(heightUnit)) {
				throw new IllegalArgumentException("行の高さの単位が不正です。");
			}
		}

		TemplateRowEntity row = getRows(templateId).stream()
				.filter(item -> item.getRowNumber().equals(rowNumber))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("対象の行が見つかりません。"));

		row.setHeight(height);
		row.setHeightUnit(heightUnit);

		return templateRowRepository.save(row);
	}

	/**
	 * 指定位置の行を削除する。
	 *
	 * 削除位置にあるセルを削除または縦方向に縮小し、
	 * 後続セルと行の番号を前方へ詰める。
	 */
	public void deleteRow(Long templateId, Integer rowNumber) {

		if (templateId == null || rowNumber == null || rowNumber < 1) {
			throw new IllegalArgumentException("行の削除位置が不正です。");
		}

		List<TemplateRowEntity> rows = getRows(templateId);

		if (rowNumber > rows.size()) {
			throw new IllegalArgumentException("行の削除位置が不正です。");
		}

		if (rows.size() <= 1) {
			throw new IllegalArgumentException("最後の1行は削除できません。");
		}

		List<TemplateCellEntity> cells = templateCellRepository
				.findByTemplateIdOrderByRowNumberAscColumnNumberAsc(templateId);

		// 移動先との一意制約の衝突を防ぐため、後方のセルから更新する
		cells.sort(
				Comparator.comparingInt(TemplateCellEntity::getColumnNumber)
						.thenComparingInt(TemplateCellEntity::getRowNumber)
						.reversed());

		for (TemplateCellEntity cell : cells) {

			int startRow = cell.getRowNumber();
			int endRow = startRow + cell.getRowSpan() - 1;

			if (startRow == rowNumber) {

				if (cell.getRowSpan() == 1) {
					templateCellRepository.delete(cell);
				} else {
					cell.setRowSpan(cell.getRowSpan() - 1);
					templateCellRepository.saveAndFlush(cell);
				}

			} else if (startRow < rowNumber && endRow >= rowNumber) {

				cell.setRowSpan(cell.getRowSpan() - 1);
				templateCellRepository.saveAndFlush(cell);

			} else if (startRow > rowNumber) {

				cell.setRowNumber(startRow - 1);
				templateCellRepository.saveAndFlush(cell);
			}
		}

		templateRowRepository.deleteByTemplateIdAndRowNumber(
				templateId, rowNumber);

		List<TemplateRowEntity> targetRows = getRows(templateId);

		for (TemplateRowEntity row : targetRows) {

			if (row.getRowNumber() > rowNumber) {
				row.setRowNumber(row.getRowNumber() - 1);
				templateRowRepository.saveAndFlush(row);
			}
		}
	}

	/**
	 * 指定位置に行を挿入する。
	 *
	 * 挿入位置以降の行番号を後方へ移動し、
	 * 挿入位置をまたぐセルは結合範囲を拡張する。
	 */
	public TemplateRowEntity insertRow(Long templateId, Integer rowNumber) {

		if (templateId == null || rowNumber == null || rowNumber < 1) {
			throw new IllegalArgumentException("行の挿入位置が不正です。");
		}

		List<TemplateRowEntity> rows = getRows(templateId);

		if (rowNumber > rows.size() + 1) {
			throw new IllegalArgumentException("行の挿入位置が不正です。");
		}

		// 後方の行から番号を移動し、番号の重複を防ぐ。
		List<TemplateRowEntity> targetRows = templateRowRepository
				.findByTemplateIdAndRowNumberGreaterThanEqualOrderByRowNumberDesc(
						templateId, rowNumber);

		for (TemplateRowEntity row : targetRows) {
			row.setRowNumber(row.getRowNumber() + 1);
			templateRowRepository.saveAndFlush(row);
		}

		// セルの配置と結合範囲を調整する。
		List<TemplateCellEntity> cells = templateCellRepository
				.findByTemplateIdOrderByRowNumberAscColumnNumberAsc(templateId);

		// 移動先との一意制約の衝突を防ぐため、後方のセルから更新する
		cells.sort(
				Comparator.comparingInt(TemplateCellEntity::getRowNumber)
						.thenComparingInt(TemplateCellEntity::getColumnNumber)
						.reversed());

		for (TemplateCellEntity cell : cells) {

			int startRow = cell.getRowNumber();
			int endRow = startRow + cell.getRowSpan() - 1;

			if (startRow >= rowNumber) {

				cell.setRowNumber(startRow + 1);
				templateCellRepository.saveAndFlush(cell);

			} else if (endRow >= rowNumber) {

				cell.setRowSpan(cell.getRowSpan() + 1);
				templateCellRepository.saveAndFlush(cell);
			}
		}

		// 新しい行を登録する。
		TemplateRowEntity newRow = new TemplateRowEntity();
		newRow.setTemplateId(templateId);
		newRow.setRowNumber(rowNumber);
		newRow.setHeightUnit("auto");

		return templateRowRepository.save(newRow);
	}
}
