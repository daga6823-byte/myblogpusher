
/**
 * テンプレートのレイアウト操作APIを担当するコントローラー
 *
 * 行・列の追加と削除、セルの結合と分割を受け付ける。
 * 操作対象のテンプレートがログインユーザーの所有物であることを確認する。
 */
package com.app.myblogpusher.controller.Template;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.entity.Template.TemplateCellEntity;
import com.app.myblogpusher.entity.Template.TemplateColumnEntity;
import com.app.myblogpusher.entity.Template.TemplateEntity;
import com.app.myblogpusher.entity.Template.TemplateRowEntity;
import com.app.myblogpusher.service.Facade.TemplateLayoutFacadeService;
import com.app.myblogpusher.service.Template.TemplateService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/template/api/layout")
public class TemplateLayoutApiController {

	@Autowired
	private TemplateLayoutFacadeService templateLayoutFacadeService;

	@Autowired
	private TemplateService templateService;

	/**
	 * 操作対象のテンプレートがログインユーザーの所有物であることを確認する。
	 *
	 * @param templateId テンプレートID
	 * @param session HTTPセッション
	 * @return 所有者確認済みのテンプレート
	 */
	private TemplateEntity getOwnedTemplate(Long templateId, HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			throw new IllegalStateException("ログイン情報がありません。");
		}

		return templateService.getTemplate(
				loginUser.getUserId(), templateId);
	}

	/**
	 * 指定位置に行を追加する。
	 */
	@PostMapping("/row/insert")
	public ResponseEntity<TemplateRowEntity> insertRow(
			@RequestParam Long templateId,
			@RequestParam Integer rowNumber,
			HttpSession session) {

		getOwnedTemplate(templateId, session);

		TemplateRowEntity row = templateLayoutFacadeService.insertRow(templateId, rowNumber);

		return ResponseEntity.ok(row);
	}

	/**
	 * 指定位置の行を削除する。
	 */
	@PostMapping("/row/delete")
	public ResponseEntity<Void> deleteRow(
			@RequestParam Long templateId,
			@RequestParam Integer rowNumber,
			HttpSession session) {

		getOwnedTemplate(templateId, session);

		templateLayoutFacadeService.deleteRow(templateId, rowNumber);

		return ResponseEntity.ok().build();
	}

	/**
	 * 指定した行の高さを更新する。
	 */
	@PostMapping("/row/size")
	public ResponseEntity<TemplateRowEntity> updateRowSize(
			@RequestParam Long templateId,
			@RequestParam Integer rowNumber,
			@RequestParam(required = false) BigDecimal height,
			@RequestParam(required = false, defaultValue = "auto") String heightUnit,
			HttpSession session) {

		getOwnedTemplate(templateId, session);

		TemplateRowEntity updatedRow = templateLayoutFacadeService.updateRowSize(
				templateId,
				rowNumber,
				height,
				heightUnit);

		return ResponseEntity.ok(updatedRow);
	}
	
	/**
	 * 隣接する2行の高さを一括更新する。
	 */
	@PostMapping("/row/sizes")
	public ResponseEntity<Void> updateRowSizes(
			@RequestParam Long templateId,
			@RequestParam Integer firstRowNumber,
			@RequestParam BigDecimal firstHeight,
			@RequestParam Integer secondRowNumber,
			@RequestParam BigDecimal secondHeight,
			HttpSession session) {

		getOwnedTemplate(templateId, session);

		templateLayoutFacadeService.updateRowSizes(
				templateId,
				firstRowNumber,
				firstHeight,
				secondRowNumber,
				secondHeight);

		return ResponseEntity.ok().build();
	}

	/**
	 * 指定位置に列を追加する。
	 */
	@PostMapping("/column/insert")
	public ResponseEntity<TemplateColumnEntity> insertColumn(
			@RequestParam Long templateId,
			@RequestParam Integer columnNumber,
			HttpSession session) {

		getOwnedTemplate(templateId, session);

		TemplateColumnEntity column = templateLayoutFacadeService.insertColumn(templateId, columnNumber);

		return ResponseEntity.ok(column);
	}
	
	/**
	 * 指定した列の幅を更新する。
	 */
	@PostMapping("/column/size")
	public ResponseEntity<TemplateColumnEntity> updateColumnSize(
	        @RequestParam Long templateId,
	        @RequestParam Integer columnNumber,
	        @RequestParam(required = false) BigDecimal width,
	        @RequestParam(required = false, defaultValue = "auto") String widthUnit,
	        HttpSession session) {

	    getOwnedTemplate(templateId, session);

	    TemplateColumnEntity updatedColumn =
	            templateLayoutFacadeService.updateColumnSize(
	                    templateId,
	                    columnNumber,
	                    width,
	                    widthUnit);

	    return ResponseEntity.ok(updatedColumn);
	}
	
	/**
	 * 隣接する2列の幅を一括更新する。
	 */
	@PostMapping("/column/sizes")
	public ResponseEntity<Void> updateColumnSizes(
			@RequestParam Long templateId,
			@RequestParam Integer firstColumnNumber,
			@RequestParam BigDecimal firstWidth,
			@RequestParam Integer secondColumnNumber,
			@RequestParam BigDecimal secondWidth,
			HttpSession session) {

		getOwnedTemplate(templateId, session);

		templateLayoutFacadeService.updateColumnSizes(
				templateId,
				firstColumnNumber,
				firstWidth,
				secondColumnNumber,
				secondWidth);

		return ResponseEntity.ok().build();
	}

	/**
	 * 指定位置の列を削除する。
	 */
	@PostMapping("/column/delete")
	public ResponseEntity<Void> deleteColumn(
			@RequestParam Long templateId,
			@RequestParam Integer columnNumber,
			HttpSession session) {

		getOwnedTemplate(templateId, session);

		templateLayoutFacadeService.deleteColumn(templateId, columnNumber);

		return ResponseEntity.ok().build();
	}

	/**
	 * 指定範囲のセルを結合する。
	 */
	@PostMapping("/cell/merge")
	public ResponseEntity<?> mergeCells(
			@RequestParam Long templateId,
			@RequestParam Integer startRow,
			@RequestParam Integer startColumn,
			@RequestParam Integer rowSpan,
			@RequestParam Integer columnSpan,
			HttpSession session) {

		getOwnedTemplate(templateId, session);

		return ResponseEntity.ok(
				templateLayoutFacadeService.mergeCells(
						templateId,
						startRow,
						startColumn,
						rowSpan,
						columnSpan));
	}

	/**
	 * 指定した結合セルを分割する。
	 */
	@PostMapping("/cell/split")
	public ResponseEntity<Void> splitCell(
			@RequestParam Long templateId,
			@RequestParam Long cellId,
			HttpSession session) {

		getOwnedTemplate(templateId, session);

		templateLayoutFacadeService.splitCell(templateId, cellId);

		return ResponseEntity.ok().build();
	}

	/**
	 * 新しいセルを作成する。
	 *
	 * 指定位置とセル種類を受け取り、テンプレートに登録する。
	 */
	@PostMapping("/cell/create")
	public ResponseEntity<TemplateCellEntity> createCell(
			@RequestParam Long templateId,
			@RequestParam Integer rowNumber,
			@RequestParam Integer columnNumber,
			@RequestParam(defaultValue = "EMPTY") String cellType,
			@RequestParam(required = false) String cellContent,
			@RequestParam(required = false) String variableName,
			@RequestParam(required = false) Long imageAssetId,
			@RequestParam(required = false) BigDecimal imageWidth,
			@RequestParam(required = false) BigDecimal imageHeight,
			HttpSession session) {

		getOwnedTemplate(templateId, session);

		TemplateCellEntity cell = new TemplateCellEntity();
		cell.setTemplateId(templateId);
		cell.setRowNumber(rowNumber);
		cell.setColumnNumber(columnNumber);
		cell.setRowSpan(1);
		cell.setColumnSpan(1);
		cell.setCellType(cellType);
		cell.setCellContent(cellContent);
		cell.setVariableName(variableName);
		cell.setImageAssetId(imageAssetId);
		cell.setImageWidth(imageWidth);
		cell.setImageHeight(imageHeight);

		templateLayoutFacadeService.saveCell(cell);

		return ResponseEntity.ok(cell);
	}

	/**
	 * セルの種類・内容・画像情報を保存する。
	 *
	 * 操作対象のセルが指定テンプレートに属していることを確認する。
	 */
	@PostMapping("/cell/save")
	public ResponseEntity<TemplateCellEntity> saveCell(
			@RequestParam Long templateId,
			@RequestParam Long cellId,
			@RequestParam(required = false) String cellType,
			@RequestParam(required = false) String cellContent,
			@RequestParam(required = false) String variableName,
			@RequestParam(required = false) Long imageAssetId,
			@RequestParam(required = false) BigDecimal imageWidth,
			@RequestParam(required = false) BigDecimal imageHeight,
			@RequestParam(required = false) String textColor,
			@RequestParam(required = false) String backgroundColor,
			@RequestParam(required = false) BigDecimal fontSize,
			@RequestParam(required = false, defaultValue = "false") boolean clearFontSize,
			@RequestParam(required = false) String fontWeight,
			@RequestParam(required = false) String textStrokeColor,
			@RequestParam(required = false) BigDecimal textStrokeWidth,
			@RequestParam(required = false) String textAlign,
			HttpSession session) {

		getOwnedTemplate(templateId, session);

		// 指定テンプレート内から対象セルを取得する。
		TemplateCellEntity cell = templateLayoutFacadeService.getCells(templateId)
				.stream()
				.filter(item -> item.getCellId().equals(cellId))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("対象のセルが見つかりません。"));

		// セル種類が指定された場合だけ、種類と内容を更新する。
		if (cellType != null) {

			cell.setCellType(cellType);

			switch (cellType) {
			case "TEXT":
				cell.setCellContent(cellContent);
				cell.setVariableName(null);
				cell.setImageAssetId(null);
				cell.setImageWidth(null);
				cell.setImageHeight(null);
				break;

			case "VARIABLE":
				cell.setCellContent(null);
				cell.setVariableName(variableName);
				cell.setImageAssetId(null);
				cell.setImageWidth(null);
				cell.setImageHeight(null);
				break;

			case "IMAGE":
				cell.setCellContent(null);
				cell.setVariableName(null);
				cell.setImageAssetId(imageAssetId);
				cell.setImageWidth(imageWidth);
				cell.setImageHeight(imageHeight);
				break;

			case "EMPTY":
				cell.setCellContent(null);
				cell.setVariableName(null);
				cell.setImageAssetId(null);
				cell.setImageWidth(null);
				cell.setImageHeight(null);
				break;

			default:
				throw new IllegalArgumentException("不正なセル種類です。");
			}
		}

		// 装飾項目は指定されたものだけ更新する。
		if (textColor != null) {
			cell.setTextColor(textColor);
		}

		// 背景色が空文字の場合は個別設定を解除する。
		if (backgroundColor != null) {
			cell.setBackgroundColor(
					backgroundColor.isBlank() ? null : backgroundColor);
		}

		if (clearFontSize) {
			cell.setFontSize(null);
		} else if (fontSize != null) {
			cell.setFontSize(fontSize);
		}

		if (fontWeight != null) {
			cell.setFontWeight(fontWeight);
		}

		if (textStrokeColor != null) {
			cell.setTextStrokeColor(textStrokeColor);
		}

		if (textStrokeWidth != null) {
			cell.setTextStrokeWidth(textStrokeWidth);
		}

		if (textAlign != null) {
			cell.setTextAlign(textAlign);
		}

		// 既存のレイアウト保存処理を利用する。
		templateLayoutFacadeService.saveCell(cell);

		return ResponseEntity.ok(cell);
	}
}