
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.entity.Article.ArticleTemplateCellEntity;
import com.app.myblogpusher.entity.Article.ArticleTemplateEntity;
import com.app.myblogpusher.entity.Template.TemplateEntity;
import com.app.myblogpusher.service.Article.ArticleTemplateCellService;
import com.app.myblogpusher.service.Article.ArticleTemplateSaveService;
import com.app.myblogpusher.service.Article.ArticleTemplateService;
import com.app.myblogpusher.service.Template.TemplateEditService;
import com.app.myblogpusher.service.Template.TemplateRenderService;
import com.app.myblogpusher.service.Template.TemplateService;
import com.app.myblogpusher.util.LoginUserUtil;
import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController

@RequestMapping("/template/api")
@RequiredArgsConstructor

public class TemplateApiController {

	private final TemplateService templateService;
	private final TemplateRenderService templateRenderService;
	private final ArticleTemplateService articleTemplateService;
	private final ArticleTemplateCellService articleTemplateCellService;
	private final ArticleTemplateSaveService articleTemplateSaveService;
	private final TemplateEditService templateEditService;
	
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
	 * テンプレートの行・列・セル情報をJSON形式で取得する。
	 *
	 * テンプレートの所有者確認と画像URLの設定は
	 * TemplateEditServiceに委譲する。
	 */
	@GetMapping("/{templateId}/data")
	public ResponseEntity<?> getTemplateData(
			@PathVariable Long templateId,
			HttpSession session) throws JsonProcessingException {

		UserMaster loginUser = LoginUserUtil.getLoginUser(session);

		if (loginUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		Map<String, Object> templateData = templateEditService.prepareEditTemplate(
				loginUser.getUserId(),
				templateId);

		return ResponseEntity.ok(templateData);
	}

	/**
	 * 記事に保存されたテンプレート設定を取得する。
	 *
	 * workIdが指定されている場合は下書き側を優先する。
	 * workIdがない場合はarticleId側の設定を取得する。
	 */
	@GetMapping("/{templateId}/article-settings")
	public ResponseEntity<?> getArticleTemplateSettings(
			@PathVariable Long templateId,
			@RequestParam(required = false) Long workId,
			@RequestParam(required = false) Long articleId,
			HttpSession session) {

		UserMaster loginUser = LoginUserUtil.getLoginUser(session);

		if (loginUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		Long userId = loginUser.getUserId();

		// テンプレートの所有者を確認する
		templateService.getTemplate(userId, templateId);

		ArticleTemplateEntity settings = null;
		List<ArticleTemplateCellEntity> cells = List.of();

		if (workId != null) {

			settings = articleTemplateService
					.findByWorkId(userId, workId)
					.orElse(null);

			if (settings != null && settings.getTemplateId().equals(templateId)) {
				cells = articleTemplateCellService.findByWorkId(userId, workId);
			} else {
				settings = null;
			}

		} else if (articleId != null) {

			settings = articleTemplateService
					.findByArticleId(userId, articleId)
					.orElse(null);

			if (settings != null && settings.getTemplateId().equals(templateId)) {
				cells = articleTemplateCellService.findByArticleId(userId, articleId);
			} else {
				settings = null;
			}
		}

		Map<String, Object> result = new java.util.HashMap<>();

		result.put("templateId", templateId);
		result.put("backgroundImageAssetId",
				settings != null ? settings.getBackgroundImageAssetId() : null);
		result.put("backgroundColor",
				settings != null ? settings.getBackgroundColor() : null);
		result.put("cells", cells);

		return ResponseEntity.ok(result);
	}

	/**
	 * 記事ごとのテンプレート全体設定とセル情報を一括保存する。
	 */
	@PostMapping("/article-settings")
	public ResponseEntity<?> saveArticleTemplateSettings(
			@RequestBody ArticleTemplateSaveRequest request,
			HttpSession session) {

		UserMaster loginUser = LoginUserUtil.getLoginUser(session);

		if (loginUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		articleTemplateSaveService.save(
				loginUser.getUserId(),
				request.workId(),
				request.articleId(),
				request.templateId(),
				request.backgroundImageAssetId(),
				request.backgroundColor(),
				request.cells());

		return ResponseEntity.ok(Map.of("saved", true));
	}

	/**
	 * 記事テンプレート設定の保存リクエスト
	 */
	public record ArticleTemplateSaveRequest(
			Long workId,
			Long articleId,
			Long templateId,
			Long backgroundImageAssetId,
			String backgroundColor,
			List<ArticleTemplateCellEntity> cells) {
	}
}
