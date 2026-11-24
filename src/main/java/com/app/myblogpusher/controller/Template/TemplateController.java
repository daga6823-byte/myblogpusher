
/**
 * テンプレート管理機能を担当するコントローラー
 *
 * テンプレート一覧・新規作成・編集画面の表示、
 * テンプレートの保存・削除を管理する。
 */
package com.app.myblogpusher.controller.Template;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.entity.Template.Template;
import com.app.myblogpusher.entity.Template.TemplateCell;
import com.app.myblogpusher.entity.Template.TemplateColumn;
import com.app.myblogpusher.entity.Template.TemplateRow;
import com.app.myblogpusher.service.Image.ImageAssetQueryService;
import com.app.myblogpusher.service.Template.TemplateColorService;
import com.app.myblogpusher.service.Template.TemplateLayoutService;
import com.app.myblogpusher.service.Template.TemplateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpSession;

@Controller
public class TemplateController {

	@Autowired
	private TemplateService templateService;

	@Autowired
	private TemplateLayoutService templateLayoutService;

	@Autowired
	private TemplateColorService templateColorService;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ImageAssetQueryService imageAssetQueryService;

	/**
	 * テンプレート管理トップ画面を表示する。
	 */
	@GetMapping("/template")
	public String templateHome() {
		return "template/template";
	}

	/**
	 * テンプレート一覧画面を表示する。
	 *
	 * ログインユーザー本人が所有するテンプレートのみ取得する。
	 */
	@GetMapping("/template/list")
	public String templateList(HttpSession session, Model model) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		Long userId = loginUser.getUserId();

		model.addAttribute(
				"templates",
				templateService.getTemplates(userId));

		return "template/template/template_list";
	}

	/**
	 * テンプレート新規作成画面を表示する。
	 */
	@GetMapping("/template/new")
	public String newTemplate(Model model) {

		model.addAttribute("template", new Template());

		return "template/template/template_edit";
	}

	/**
	 * テンプレート編集画面を表示する。
	 *
	 * テンプレート本体と行・列・セル情報、
	 * ユーザーの色履歴を画面に渡す。
	 */
	@GetMapping("/template/edit")
	public String editTemplate(
			@RequestParam Long templateId,
			HttpSession session,
			Model model) throws JsonProcessingException {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		Long userId = loginUser.getUserId();

		Template template = templateService.getTemplate(userId, templateId);

		model.addAttribute("template", template);

		model.addAttribute(
				"rows",
				templateLayoutService.getRows(templateId));

		model.addAttribute(
				"columns",
				templateLayoutService.getColumns(templateId));

		List<TemplateCell> cells = templateLayoutService.getCells(templateId);

		model.addAttribute("cells", cells);

		// JavaScriptでレイアウト情報を扱えるようJSON形式に変換する。
		model.addAttribute(
				"rowsJson",
				objectMapper.writeValueAsString(
						templateLayoutService.getRows(templateId)));

		model.addAttribute(
				"columnsJson",
				objectMapper.writeValueAsString(
						templateLayoutService.getColumns(templateId)));

		// セル情報をJSON配列に変換する。
		JsonNode cellsTree = objectMapper.valueToTree(cells);
		ArrayNode cellsJson = (ArrayNode) cellsTree;

		// IMAGEセルには、所有者確認済みの画像URLを追加する。
		for (JsonNode node : cellsJson) {

			if (!(node instanceof ObjectNode cellJson)) {
				continue;
			}

			if (!"IMAGE".equals(cellJson.path("cellType").asText())) {
				continue;
			}

			if (cellJson.path("imageAssetId").isNull()) {
				continue;
			}

			Long imageAssetId = cellJson.path("imageAssetId").asLong();

			try {
				String imageUrl = imageAssetQueryService.getImageUrl(
						userId,
						imageAssetId);

				cellJson.put("imageUrl", imageUrl);

			} catch (IllegalArgumentException e) {
				// 画像が削除済み、またはユーザーの所有物でない場合はURLを設定しない。
				cellJson.putNull("imageUrl");
			}
		}

		model.addAttribute(
				"cellsJson",
				objectMapper.writeValueAsString(cellsJson));

		model.addAttribute(
				"colors",
				templateColorService.getColors(userId));

		return "template/template/template_edit";
	}

	/**
	 * テンプレートを新規登録または更新する。
	 *
	 * ユーザーIDは画面から受け取らず、
	 * ログインユーザーのIDを設定する。
	 */
	@PostMapping("/template/save")
	public String saveTemplate(
			@ModelAttribute Template template,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		Long userId = loginUser.getUserId();

		if (template.getTemplateId() == null) {

			template.setUserId(userId);

			// テンプレート本体を新規登録する。
			Template saved = templateService.createTemplate(template);

			Long templateId = saved.getTemplateId();

			// 初期行を登録する。
			TemplateRow row = new TemplateRow();
			row.setTemplateId(templateId);
			row.setRowNumber(1);
			row.setHeightUnit("auto");
			templateLayoutService.saveRow(row);

			// 初期列を登録する。
			TemplateColumn column = new TemplateColumn();
			column.setTemplateId(templateId);
			column.setColumnNumber(1);
			column.setWidthUnit("fr");
			templateLayoutService.saveColumn(column);

			// 初期セルを登録する。
			TemplateCell cell = new TemplateCell();
			cell.setTemplateId(templateId);
			cell.setRowNumber(1);
			cell.setColumnNumber(1);
			cell.setRowSpan(1);
			cell.setColumnSpan(1);
			cell.setCellType("EMPTY");
			templateLayoutService.saveCell(cell);

			return "redirect:/template/edit?templateId="
					+ templateId;
		}

		templateService.updateTemplate(userId, template);

		return "redirect:/template/edit?templateId="
				+ template.getTemplateId();
	}

	/**
	 * テンプレートを削除する。
	 *
	 * ログインユーザー本人のテンプレートであることを確認して削除する。
	 */
	@PostMapping("/template/delete")
	public String deleteTemplate(
			@RequestParam Long templateId,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		Long userId = loginUser.getUserId();

		templateService.deleteTemplate(userId, templateId);

		return "redirect:/template/list";
	}
}
