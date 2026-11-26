
/**
 * テンプレート管理機能を担当するコントローラー
 *
 * テンプレート一覧・新規作成・編集画面の表示、
 * テンプレートの保存・削除を管理する。
 */
package com.app.myblogpusher.controller.Template;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.entity.Template.TemplateEntity;
import com.app.myblogpusher.service.Template.TemplateDuplicateService;
import com.app.myblogpusher.service.Template.TemplateEditService;
import com.app.myblogpusher.service.Template.TemplateSaveService;
import com.app.myblogpusher.service.Template.TemplateService;
import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class TemplateController {

	private final TemplateService templateService;
	private final TemplateEditService templateEditService;
	private final TemplateSaveService templateSaveService;
	private final TemplateDuplicateService templateDuplicateService;

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
	 * 保存処理はTemplateSaveServiceに委譲する。
	 */
	@PostMapping("/template/save")
	public String saveTemplate(
			@ModelAttribute TemplateEntity template,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		Long templateId = templateSaveService.saveTemplate(
				loginUser.getUserId(),
				template);

		return "redirect:/template/edit?templateId=" + templateId;
	}

	/**
	 * JavaScriptからテンプレートを保存するAPI。
	 *
	 * 画面遷移せず、保存結果をJSONで返す。
	 */
	@PostMapping("/template/api/save")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> saveTemplateApi(
			@ModelAttribute TemplateEntity template,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			return ResponseEntity.status(401).build();
		}

		Long templateId = templateSaveService.saveTemplate(
				loginUser.getUserId(),
				template);

		return ResponseEntity.ok(Map.of(
				"templateId", templateId,
				"saved", true));
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

	/**
	 * テンプレート複製API
	 */
	@PostMapping("/api/{templateId}/duplicate")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> duplicateTemplate(
	        @PathVariable Long templateId,
	        @RequestParam("templateName") String templateName,
	        HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		Long userId = loginUser.getUserId();

		try {
			TemplateEntity duplicated = templateDuplicateService
			        .duplicateTemplate(userId, templateId, templateName.trim());

			Map<String, Object> response = new HashMap<>();
			response.put("templateId", duplicated.getTemplateId());
			response.put("duplicated", true);

			return ResponseEntity.ok(response);

		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		}
	}
}
