
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
import com.app.myblogpusher.entity.Template.TemplateEntity;
import com.app.myblogpusher.entity.Template.TemplateCell;
import com.app.myblogpusher.entity.Template.TemplateColumn;
import com.app.myblogpusher.entity.Template.TemplateRow;
import com.app.myblogpusher.service.Template.TemplateLayoutService;
import com.app.myblogpusher.service.Template.TemplateService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/template/api/layout")
public class TemplateLayoutApiController {

	@Autowired
	private TemplateLayoutService templateLayoutService;

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
	public ResponseEntity<TemplateRow> insertRow(
			@RequestParam Long templateId,
			@RequestParam Integer rowNumber,
			HttpSession session) {

		getOwnedTemplate(templateId, session);

		TemplateRow row = templateLayoutService.insertRow(templateId, rowNumber);

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

		templateLayoutService.deleteRow(templateId, rowNumber);

		return ResponseEntity.ok().build();
	}

	/**
	 * 指定位置に列を追加する。
	 */
	@PostMapping("/column/insert")
	public ResponseEntity<TemplateColumn> insertColumn(
			@RequestParam Long templateId,
			@RequestParam Integer columnNumber,
			HttpSession session) {

		getOwnedTemplate(templateId, session);

		TemplateColumn column = templateLayoutService.insertColumn(templateId, columnNumber);

		return ResponseEntity.ok(column);
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

		templateLayoutService.deleteColumn(templateId, columnNumber);

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
				templateLayoutService.mergeCells(
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

		templateLayoutService.splitCell(templateId, cellId);

		return ResponseEntity.ok().build();
	}

	/**
	 * 新しいセルを作成する。
	 *
	 * 指定位置とセル種類を受け取り、テンプレートに登録する。
	 */
	@PostMapping("/cell/create")
	public ResponseEntity<TemplateCell> createCell(
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

		TemplateCell cell = new TemplateCell();
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

		templateLayoutService.saveCell(cell);

		return ResponseEntity.ok(cell);
	}

	/**
	 * セルの種類・内容・画像情報を保存する。
	 *
	 * 操作対象のセルが指定テンプレートに属していることを確認する。
	 */
	@PostMapping("/cell/save")
	public ResponseEntity<TemplateCell> saveCell(
			@RequestParam Long templateId,
			@RequestParam Long cellId,
			@RequestParam String cellType,
			@RequestParam(required = false) String cellContent,
			@RequestParam(required = false) String variableName,
			@RequestParam(required = false) Long imageAssetId,
			@RequestParam(required = false) BigDecimal imageWidth,
			@RequestParam(required = false) BigDecimal imageHeight,
			HttpSession session) {

		getOwnedTemplate(templateId, session);

		// 指定テンプレート内から対象セルを取得する。
		TemplateCell cell = templateLayoutService.getCells(templateId)
				.stream()
				.filter(item -> item.getCellId().equals(cellId))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("対象のセルが見つかりません。"));

		// セルの種類を更新する。
		cell.setCellType(cellType);

		// セル種類に応じて保存項目を切り替える。
		cell.setCellContent(null);
		cell.setVariableName(null);
		cell.setImageAssetId(null);
		cell.setImageWidth(null);
		cell.setImageHeight(null);

		switch (cellType) {
		case "TEXT":
			cell.setCellContent(cellContent);
			break;

		case "VARIABLE":
			cell.setVariableName(variableName);
			break;

		case "IMAGE":
			cell.setImageAssetId(imageAssetId);
			cell.setImageWidth(imageWidth);
			cell.setImageHeight(imageHeight);
			break;

		case "EMPTY":
			break;

		default:
			throw new IllegalArgumentException("不正なセル種類です。");
		}

		// 既存のレイアウト保存処理を利用する。
		templateLayoutService.saveCell(cell);

		return ResponseEntity.ok(cell);
	}
}
