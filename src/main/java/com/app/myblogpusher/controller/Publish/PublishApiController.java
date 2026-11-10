/**
 * 投稿画面から呼び出されるJSON APIを担当するコントローラー
 *
 * 投稿確定時のスラッグ生成、非同期投稿の状態確認、
 * 確認画面で編集されたサムネイルとスラッグの保存を担当する。
 * スラッグ決定はPublishSlugService、投稿状態の判定はPublishStatusServiceへ委譲する。
 */
package com.app.myblogpusher.controller.Publish;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.entity.Article.ArticleWork;
import com.app.myblogpusher.service.Article.ArticleWorkService;
import com.app.myblogpusher.service.Publish.PublishSlugService;
import com.app.myblogpusher.service.Publish.PublishStatusService;
import com.app.myblogpusher.util.LoginUserUtil;

import jakarta.servlet.http.HttpSession;

@RestController
public class PublishApiController {

	private final ArticleWorkService articleWorkService;
	private final PublishSlugService publishSlugService;
	private final PublishStatusService publishStatusService;

	/**
	 * 投稿用APIに必要な依存関係を設定する。
	 */
	public PublishApiController(
			ArticleWorkService articleWorkService,
			PublishSlugService publishSlugService,
			PublishStatusService publishStatusService) {
		this.articleWorkService = articleWorkService;
		this.publishSlugService = publishSlugService;
		this.publishStatusService = publishStatusService;
	}

	/**
	 * 投稿確定時にスラッグを決定する。
	 *
	 * 新規記事は形態素解析と英語辞書による変換結果から生成し、
	 * 既存記事は保存済みのスラッグを維持する。
	 */
	@PostMapping("/publish/generate-slug")
	public ResponseEntity<Map<String, String>> generatePublishSlug(
			@RequestParam Long workId,
			@RequestParam String title,
			HttpSession session) {

		UserMaster loginUser = LoginUserUtil.getLoginUser(session);

		if (loginUser == null) {
			return ResponseEntity.status(401)
					.body(Map.of("error", "UNAUTHORIZED"));
		}

		ArticleWork work = articleWorkService.findById(
				workId,
				loginUser.getUserId());

		if (work == null) {
			return ResponseEntity.notFound().build();
		}

		String slug = publishSlugService.decideSlug(work, title);

		if (slug == null || slug.isBlank()) {
			return ResponseEntity.badRequest()
					.body(Map.of("error", "SLUG_GENERATION_FAILED"));
		}

		return ResponseEntity.ok(Map.of("slug", slug));
	}

	/**
	 * 非同期投稿の状態を確認し、成功時に公開記事のURLを返す。
	 *
	 * 判定ロジックはPublishStatusServiceへ委譲する。
	 */
	@GetMapping("/publish/status")
	public ResponseEntity<Map<String, String>> getPublishStatus(
			@RequestParam Long workId,
			@RequestParam String slug,
			HttpSession session) {

		UserMaster loginUser = LoginUserUtil.getLoginUser(session);

		if (loginUser == null) {
			return ResponseEntity.status(401)
					.body(Map.of("status", "UNAUTHORIZED"));
		}

		PublishStatusService.PublishStatusResult result = publishStatusService.getStatus(
				loginUser.getUserId(),
				workId,
				slug);

		if (result.url() == null) {
			return ResponseEntity.ok(Map.of("status", result.status()));
		}

		return ResponseEntity.ok(
				Map.of(
						"status", result.status(),
						"url", result.url()));
	}

	/**
	 * 投稿確認画面で選択されたサムネイルを保存する。
	 */
	@PostMapping("/publish/preview/thumbnail")
	public ResponseEntity<Void> savePreviewThumbnail(
			@RequestParam Long workId,
			@RequestParam(required = false) String thumbnailUrl,
			HttpSession session) {

		UserMaster loginUser = LoginUserUtil.getLoginUser(session);

		if (loginUser == null) {
			return ResponseEntity.status(401).build();
		}

		articleWorkService.updateThumbnailUrl(
				workId,
				loginUser.getUserId(),
				thumbnailUrl);

		return ResponseEntity.noContent().build();
	}

	/**
	 * 投稿確認画面で編集されたスラッグを保存する。
	 */
	@PostMapping("/publish/preview/slug")
	public ResponseEntity<Void> savePreviewSlug(
			@RequestParam Long workId,
			@RequestParam String slug,
			HttpSession session) {

		UserMaster loginUser = LoginUserUtil.getLoginUser(session);

		if (loginUser == null) {
			return ResponseEntity.status(401).build();
		}

		articleWorkService.updatePreviewSlug(
				workId,
				loginUser.getUserId(),
				slug);

		return ResponseEntity.noContent().build();
	}
}