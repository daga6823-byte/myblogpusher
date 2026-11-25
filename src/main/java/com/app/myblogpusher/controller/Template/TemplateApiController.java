
/**
 * テンプレート管理画面から呼び出されるJSON APIを担当するコントローラー
 *
 * テンプレートの登録、更新、削除を担当する。
 */
package com.app.myblogpusher.controller.Template;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.entity.Template.TemplateCell;
import com.app.myblogpusher.entity.Template.TemplateColumn;
import com.app.myblogpusher.entity.Template.TemplateEntity;
import com.app.myblogpusher.entity.Template.TemplateRow;
import com.app.myblogpusher.service.Template.TemplateLayoutService;
import com.app.myblogpusher.service.Template.TemplateRenderService;
import com.app.myblogpusher.service.Template.TemplateService;
import com.app.myblogpusher.util.LoginUserUtil;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController

@RequestMapping("/template/api")
@RequiredArgsConstructor

public class TemplateApiController {

	private final TemplateService templateService;
	private final TemplateRenderService templateRenderService;
	private final TemplateLayoutService templateLayoutService;

	// テンプレートを新規登録する
	@PostMapping
	public ResponseEntity<?> create(@RequestBody TemplateEntity template, HttpSession session) {

		UserMaster loginUser = LoginUserUtil.getLoginUser(session);

		if (loginUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		template.setUserId(loginUser.getUserId());

		TemplateEntity saved = templateService.createTemplate(template);

		return ResponseEntity.ok(
				Map.of("templateId", saved.getTemplateId()));
	}

	// テンプレートを更新する
	@PutMapping("/{templateId}")
	public ResponseEntity<?> update(
			@PathVariable Long templateId,
			@RequestBody TemplateEntity template,
			HttpSession session) {

		UserMaster loginUser = LoginUserUtil.getLoginUser(session);

		if (loginUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		template.setTemplateId(templateId);

		templateService.updateTemplate(loginUser.getUserId(), template);

		return ResponseEntity.ok(
				Map.of("templateId", templateId));
	}

	// テンプレートを削除する
	@DeleteMapping("/{templateId}")
	public ResponseEntity<?> delete(
			@PathVariable Long templateId,
			HttpSession session) {

		UserMaster loginUser = LoginUserUtil.getLoginUser(session);

		if (loginUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		templateService.deleteTemplate(loginUser.getUserId(), templateId);

		return ResponseEntity.ok().build();
	}

	/**
	 * ログインユーザーが登録したテンプレート一覧を取得する。
	 */
	@GetMapping("/list")
	public ResponseEntity<?> getTemplateList(HttpSession session) {

		UserMaster loginUser = LoginUserUtil.getLoginUser(session);

		if (loginUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		List<TemplateEntity> templates = templateService.getTemplates(loginUser.getUserId());

		return ResponseEntity.ok(templates);
	}

	/**
	 * テンプレートのHTML・CSSを生成する。
	 *
	 * ログインユーザー本人が所有するテンプレートのみ生成できる。
	 */
	@GetMapping("/{templateId}/render")
	public ResponseEntity<?> generateTemplate(
			@PathVariable Long templateId,
			HttpSession session) {

		UserMaster loginUser = LoginUserUtil.getLoginUser(session);

		if (loginUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		Map<String, String> result = templateRenderService.generate(
				loginUser.getUserId(),
				templateId);

		return ResponseEntity.ok(result);
	}

	/**
	 * レイアウト操作対象のテンプレート所有権を確認する。
	 */
	private Long validateTemplateOwner(HttpSession session, Long templateId) {

		UserMaster loginUser = LoginUserUtil.getLoginUser(session);

		if (loginUser == null) {
			throw new IllegalArgumentException("ログインしてください。");
		}

		templateService.getTemplate(loginUser.getUserId(), templateId);

		return templateId;
	}

	/**
	 * 行を追加する。
	 */
	@PostMapping("/layout/row/insert")
	public ResponseEntity<?> insertRow(
			@RequestBody Map<String, Object> params,
			HttpSession session) {

		Long templateId = Long.valueOf(params.get("templateId").toString());
		Integer rowNumber = Integer.valueOf(params.get("rowNumber").toString());

		validateTemplateOwner(session, templateId);

		TemplateRow row = templateLayoutService.insertRow(templateId, rowNumber);

		return ResponseEntity.ok(row);
	}

	/**
	 * 列を追加する。
	 */
	@PostMapping("/layout/column/insert")
	public ResponseEntity<?> insertColumn(
			@RequestBody Map<String, Object> params,
			HttpSession session) {

		Long templateId = Long.valueOf(params.get("templateId").toString());
		Integer columnNumber = Integer.valueOf(params.get("columnNumber").toString());

		validateTemplateOwner(session, templateId);

		TemplateColumn column = templateLayoutService.insertColumn(
				templateId, columnNumber);

		return ResponseEntity.ok(column);
	}

	/**
	 * セルを新規登録する。
	 */
	@PostMapping("/layout/cell/create")
	public ResponseEntity<?> createCell(
			@RequestBody Map<String, Object> params,
			HttpSession session) {

		Long templateId = Long.valueOf(params.get("templateId").toString());

		validateTemplateOwner(session, templateId);

		TemplateCell cell = new TemplateCell();
		cell.setTemplateId(templateId);
		cell.setRowNumber(Integer.valueOf(params.get("rowNumber").toString()));
		cell.setColumnNumber(Integer.valueOf(params.get("columnNumber").toString()));
		cell.setRowSpan(1);
		cell.setColumnSpan(1);
		cell.setCellType(params.get("cellType").toString());

		setCellValues(cell, params);

		TemplateCell saved = templateLayoutService.saveCell(cell);

		return ResponseEntity.ok(
				Map.of("cellId", saved.getCellId()));
	}

	/**
	 * 既存セルを更新する。
	 */
	@PostMapping("/layout/cell/save")
	public ResponseEntity<?> saveCell(
			@RequestBody Map<String, Object> params,
			HttpSession session) {

		Long templateId = Long.valueOf(params.get("templateId").toString());
		Long cellId = Long.valueOf(params.get("cellId").toString());

		validateTemplateOwner(session, templateId);

		TemplateCell cell = templateLayoutService.getCells(templateId).stream()
				.filter(target -> cellId.equals(target.getCellId()))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("セルが存在しません。"));

		cell.setCellType(params.get("cellType").toString());

		setCellValues(cell, params);

		TemplateCell saved = templateLayoutService.saveCell(cell);

		return ResponseEntity.ok(
				Map.of("cellId", saved.getCellId()));
	}

	/**
	 * セル種類に応じた値を設定する。
	 */
	private void setCellValues(
			TemplateCell cell,
			Map<String, Object> params) {

		cell.setCellContent((String) params.get("cellContent"));
		cell.setVariableName((String) params.get("variableName"));

		if (params.get("imageAssetId") != null) {
			cell.setImageAssetId(
					Long.valueOf(params.get("imageAssetId").toString()));
		}

		if (params.get("imageWidth") != null) {
			cell.setImageWidth(parseBigDecimal(params.get("imageWidth")));
		}

		if (params.get("imageHeight") != null) {
			cell.setImageHeight(parseBigDecimal(params.get("imageHeight")));
		}
	}

	/**
	 * 画像サイズをBigDecimalに変換する
	 */
	private BigDecimal parseBigDecimal(Object value) {
		if (value == null || value.toString().isBlank()) {
			return null;
		}

		return new BigDecimal(value.toString());
	}
}
