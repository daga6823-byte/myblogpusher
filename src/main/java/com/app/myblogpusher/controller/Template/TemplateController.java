
/**
 * テンプレート管理機能を担当するコントローラー
 *
 * テンプレート一覧・新規作成・編集画面の表示、
 * テンプレートの保存・削除を管理する。
 */
package com.app.myblogpusher.controller.Template;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.entity.Template.TemplateCellEntity;
import com.app.myblogpusher.entity.Template.TemplateColumn;
import com.app.myblogpusher.entity.Template.TemplateEntity;
import com.app.myblogpusher.entity.Template.TemplateRow;
import com.app.myblogpusher.facade.TemplateLayoutFacadeService;
import com.app.myblogpusher.service.Template.TemplateEditService;
import com.app.myblogpusher.service.Template.TemplateService;
import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.servlet.http.HttpSession;

@Controller
public class TemplateController {

	@Autowired
	private TemplateService templateService;

	@Autowired
	private TemplateLayoutFacadeService templateLayoutFacadeService;

	@Autowired
	private TemplateEditService templateEditService;

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

		return "template/template_list";
	}

	/**
	 * テンプレート新規作成画面を表示する。
	 */
	@GetMapping("/template/new")
	public String newTemplate(
			HttpSession session,
			Model model) throws JsonProcessingException {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		model.addAllAttributes(
				templateEditService.prepareNewTemplate(loginUser.getUserId()));

		return "template/template_edit";
	}

	/**
	 * テンプレート編集画面を表示する。
	 *
	 * テンプレート編集画面に必要なデータの準備は
	 * TemplateEditServiceに委譲する。
	 */
	@GetMapping("/template/edit")
	public String editTemplate(
			@RequestParam Long templateId,
			HttpSession session,
			Model model) throws JsonProcessingException {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		model.addAllAttributes(
				templateEditService.prepareEditTemplate(
						loginUser.getUserId(),
						templateId));

		return "template/template_edit";
	}

	/**
	 * テンプレートを新規登録または更新する。
	 *
	 * ユーザーIDは画面から受け取らず、
	 * ログインユーザーのIDを設定する。
	 */
	@PostMapping("/template/save")
	public String saveTemplate(
			@ModelAttribute TemplateEntity template,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		Long userId = loginUser.getUserId();

		if (template.getTemplateId() == null) {

			template.setUserId(userId);

			// テンプレート本体を新規登録する。
			TemplateEntity saved = templateService.createTemplate(template);

			Long templateId = saved.getTemplateId();

			// 初期行を登録する。
			TemplateRow row = new TemplateRow();
			row.setTemplateId(templateId);
			row.setRowNumber(1);
			row.setHeightUnit("auto");
			templateLayoutFacadeService.saveRow(row);

			// 初期列を登録する。
			TemplateColumn column = new TemplateColumn();
			column.setTemplateId(templateId);
			column.setColumnNumber(1);
			column.setWidthUnit("fr");
			templateLayoutFacadeService.saveColumn(column);

			// 初期セルを登録する。
			TemplateCellEntity cell = new TemplateCellEntity();
			cell.setTemplateId(templateId);
			cell.setRowNumber(1);
			cell.setColumnNumber(1);
			cell.setRowSpan(1);
			cell.setColumnSpan(1);
			cell.setCellType("EMPTY");
			templateLayoutFacadeService.saveCell(cell);

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
