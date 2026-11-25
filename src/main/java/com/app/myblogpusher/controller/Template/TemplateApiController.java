
/**
 * テンプレート管理画面から呼び出されるJSON APIを担当するコントローラー
 *
 * テンプレートの登録、更新、削除を担当する。
 */
package com.app.myblogpusher.controller.Template;

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
import com.app.myblogpusher.entity.Template.TemplateEntity;
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
}
