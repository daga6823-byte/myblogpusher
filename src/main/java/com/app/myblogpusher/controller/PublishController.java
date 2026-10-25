
/**
 * 記事投稿機能を担当するコントローラー
 *
 * 投稿前確認画面の表示、投稿確定、確認画面でのスラッグ・サムネイル保存を担当する。
 * 記事の保存はArticleSaveUtil、投稿確認データの生成はPublishPreviewService、
 * GitHubへの投稿処理はArticlePublishServiceへ委譲する。
 */
package com.app.myblogpusher.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.enums.ArticleStatus;
import com.app.myblogpusher.repository.UserRepositoryRepository;
import com.app.myblogpusher.service.PublishExecutionService;
import com.app.myblogpusher.service.PublishPreviewPreparationService;
import com.app.myblogpusher.service.PublishPreviewService;
import com.app.myblogpusher.service.Article.ArticleService;
import com.app.myblogpusher.service.Article.ArticleWorkService;
import com.app.myblogpusher.util.ArticleSaveUtil;

import jakarta.servlet.http.HttpSession;

@Controller
public class PublishController {

	private final PublishPreviewPreparationService publishPreviewPreparationService;
	private final PublishExecutionService publishExecutionService;
	private final ArticleWorkService articleWorkService;
	private final ArticleSaveUtil articleSaveUtil;
	private final ArticleService articleService;
	private final UserRepositoryRepository userRepositoryRepository;

	/**
	 * 投稿確認画面と投稿確定処理に必要な依存関係を設定する。
	 */
	public PublishController(
			PublishPreviewService publishPreviewService,
			PublishPreviewPreparationService publishPreviewPreparationService,
			PublishExecutionService publishExecutionService,
			ArticleWorkService articleWorkService,
			ArticleSaveUtil articleSaveUtil,
			ArticleService articleService,
			UserRepositoryRepository userRepositoryRepository) {
		this.publishPreviewPreparationService = publishPreviewPreparationService;
		this.publishExecutionService = publishExecutionService;
		this.articleWorkService = articleWorkService;
		this.articleSaveUtil = articleSaveUtil;
		this.articleService = articleService;
		this.userRepositoryRepository = userRepositoryRepository;
	}

	/**
	 * 投稿前確認画面を表示する。
	 *
	 * 投稿確認へ進む前に記事をワークテーブルへ保存し、
	 * 保存済みデータをもとに確認画面のフォームを生成する。
	 */
	@PostMapping("/publish/preview")
	public String showPreview(
			@RequestParam(required = false) Long workId,
			@RequestParam String title,
			@RequestParam String content,
			@RequestParam String categorySelect,
			@RequestParam(required = false) String newCategoryName,
			@RequestParam(required = false) String newCategoryDisplayName,
			HttpSession session,
			Model model) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			return "redirect:/login";
		}

		Long userId = loginUser.getUserId();

		// 保存前のワークと投稿済み記事を取得する。
		PublishPreviewPreparationService.ExistingArticleData existingData = publishPreviewPreparationService
				.findExistingArticleData(workId);

		// 記事とカテゴリー情報をワークテーブルへ保存する。
		Long savedWorkId = articleSaveUtil.doSaveDraft(
				workId,
				categorySelect,
				newCategoryName,
				newCategoryDisplayName,
				title,
				content,
				userId);

		if (savedWorkId == null) {
			model.addAttribute("error", "記事の保存に失敗しました");
			return "error";
		}

		// 辞典登録後に投稿確認画面へ戻るための記事情報を保持する。
		session.setAttribute("previewTitle", title);
		session.setAttribute("previewContent", content);
		session.setAttribute("previewWorkId", savedWorkId);

		// 保存後のデータから投稿確認画面の表示情報を準備する。
		PublishPreviewPreparationService.PreparationResult result = publishPreviewPreparationService.preparePreview(
				savedWorkId,
				title,
				content,
				userId,
				existingData);

		// 表示準備に失敗した場合はエラー画面へ遷移する。
		if (!result.isSuccess()) {
			model.addAttribute("error", result.getErrorMessage());
			return "error";
		}

		// 生成されたフォームとスラッグ解析結果を画面に渡す。
		PublishPreviewService.PublishPreviewData previewData = result.getPreviewData();

		model.addAttribute("analysis", previewData.getAnalysis());
		model.addAttribute("form", previewData.getForm());

		return "publish_preview";
	}

	/**
	 * 投稿を確定し、非同期のGitHub投稿処理を開始する。
	 *
	 * 投稿確定後の処理はPublishExecutionServiceへ委譲する。
	 */
	@PostMapping("/publish/execute")
	public String executePublish(
			@RequestParam(required = false) Long workId,
			@RequestParam String title,
			@RequestParam String content,
			@RequestParam Long categoryId,
			@RequestParam String slug,
			@RequestParam(required = false) String thumbnailUrl,
			HttpSession session,
			Model model) {

		// ログイン状態を確認する。
		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			return "redirect:/login";
		}

		// 投稿確定後の処理をサービスへ委譲する。
		boolean started = publishExecutionService.executePublish(
				workId,
				categoryId,
				slug,
				thumbnailUrl,
				loginUser.getUserId(),
				loginUser.getCipherKey());

		// 投稿先リポジトリが未設定の場合はエラー画面を表示する。
		if (!started) {
			model.addAttribute("error", "リポジトリが設定されていません");
			return "error";
		}

		return "redirect:/article/list?published";
	}

	/**
	 * 非同期投稿の状態を確認し、成功時に公開記事のURLを返す。
	 *
	 * ArticleWorkが存在する間は投稿状態を確認し、
	 * Work削除後はArticleの公開ステータスを確認する。
	 */
	@GetMapping("/publish/status")
	@ResponseBody
	public ResponseEntity<Map<String, String>> getPublishStatus(
			@RequestParam Long workId,
			@RequestParam String slug,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			return ResponseEntity.status(401)
					.body(Map.of("status", "UNAUTHORIZED"));
		}

		Long userId = loginUser.getUserId();

		// 投稿対象のWorkが残っていれば、投稿中または失敗。
		var works = articleWorkService.findByUserId(userId);

		for (var work : works) {
			if (workId.equals(work.getWorkId())) {
				if (work.getStatus() != null && work.getStatus() == 2) {
					return ResponseEntity.ok(
							Map.of("status", "FAILED"));
				}

				return ResponseEntity.ok(
						Map.of("status", "PENDING"));
			}
		}

		// Work削除後は、公開済みArticleを確認する。
		var article = articleService.findBySlug(userId, slug);

		if (article == null
				|| article.getStatus() != ArticleStatus.PUBLISHED) {
			return ResponseEntity.ok(
					Map.of("status", "PENDING"));
		}

		var repoOpt = userRepositoryRepository.findByUserId(userId);

		if (repoOpt.isEmpty()) {
			return ResponseEntity.ok(
					Map.of("status", "FAILED"));
		}

		var repo = repoOpt.get();
		String repoOwner = repo.getRepoOwner();
		String repoName = repo.getRepoName();

		String baseUrl;

		if (repoName.equalsIgnoreCase(repoOwner + ".github.io")) {
			baseUrl = "https://" + repoName + "/";
		} else {
			baseUrl = "https://" + repoOwner + ".github.io/"
					+ repoName + "/";
		}

		String hugoPath = article.getHugoPath();

		if (hugoPath == null || hugoPath.isBlank()) {
			return ResponseEntity.ok(
					Map.of("status", "FAILED"));
		}

		hugoPath = hugoPath.replaceAll("^/+", "").replaceAll("/+$", "");

		return ResponseEntity.ok(
				Map.of(
						"status", "PUBLISHED",
						"url", baseUrl + hugoPath + "/"));
	}

	/**
	 * 投稿確認画面への復帰要求を処理する。
	 *
	 * セッションに保持された記事情報を取得し、
	 * 投稿確認画面の表示準備をPublishPreviewPreparationServiceへ委譲する。
	 */
	@GetMapping("/publish/preview/back")
	public String backToPreview(HttpSession session, Model model) {

		// 投稿確認画面へ戻す記事情報をセッションから取得する。
		String title = (String) session.getAttribute("previewTitle");
		String content = (String) session.getAttribute("previewContent");
		Long workId = (Long) session.getAttribute("previewWorkId");

		// ログイン状態を確認する。
		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			return "redirect:/login";
		}

		Long userId = loginUser.getUserId();

		// 保存前のワークと投稿済み記事を取得する。
		PublishPreviewPreparationService.ExistingArticleData existingData = publishPreviewPreparationService
				.findExistingArticleData(workId);

		// 保存済みワークのスラッグを優先して投稿確認画面の表示情報を準備する。
		PublishPreviewPreparationService.PreparationResult result = publishPreviewPreparationService.preparePreview(
				workId,
				title,
				content,
				userId,
				existingData,
				true);

		// 表示準備に失敗した場合はエラー画面へ遷移する。
		if (!result.isSuccess()) {
			model.addAttribute("error", result.getErrorMessage());
			return "error";
		}

		// 生成されたフォームとスラッグ解析結果を画面に渡す。
		PublishPreviewService.PublishPreviewData previewData = result.getPreviewData();

		model.addAttribute("analysis", previewData.getAnalysis());
		model.addAttribute("form", previewData.getForm());

		return "publish_preview";
	}

	/**
	 * 投稿確認画面で選択されたサムネイルを保存する。
	 */
	@PostMapping("/publish/preview/thumbnail")
	@ResponseBody
	public ResponseEntity<Void> savePreviewThumbnail(
			@RequestParam Long workId,
			@RequestParam(required = false) String thumbnailUrl,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

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
	@ResponseBody
	public ResponseEntity<Void> savePreviewSlug(
			@RequestParam Long workId,
			@RequestParam String slug,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			return ResponseEntity.status(401).build();
		}

		articleWorkService.updatePreviewSlug(
				workId,
				loginUser.getUserId(),
				slug);

		return ResponseEntity.noContent().build();
	}

	/**
	 * 記事の投稿完了を待機する画面を表示する
	 *
	 * 投稿状況の確認と、投稿成功後の公開記事への遷移は
	 * publish_wait.html 側で行う。
	 *
	 * @return 投稿待機画面
	 */
	@GetMapping("/publish/wait")
	public String showPublishWait() {
		return "publish_wait";
	}
}
