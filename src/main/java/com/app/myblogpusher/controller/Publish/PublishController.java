/**
 * 記事投稿の画面遷移を担当するコントローラー
 *
 * 投稿前確認画面の表示、確認画面への復帰、投稿確定、投稿待機画面、エラー画面の表示を担当する。
 * ワーク保存はPublishPreviewDraftService、確認データの生成はPublishPreviewPreparationService、
 * GitHubへの投稿処理はPublishExecutionServiceへ委譲する。
 * JSON応答のAPIはPublishApiControllerが担当する。
 */
package com.app.myblogpusher.controller.Publish;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.service.PublishExecutionService;
import com.app.myblogpusher.service.PublishPreviewPreparationService;
import com.app.myblogpusher.service.PublishPreviewService;
import com.app.myblogpusher.service.Publish.PublishPreviewDraftService;
import com.app.myblogpusher.util.LoginUserUtil;

import jakarta.servlet.http.HttpSession;

@Controller
public class PublishController {

	private final PublishPreviewPreparationService publishPreviewPreparationService;
	private final PublishPreviewDraftService publishPreviewDraftService;
	private final PublishExecutionService publishExecutionService;

	/**
	 * 投稿画面の遷移に必要な依存関係を設定する。
	 */
	public PublishController(
			PublishPreviewPreparationService publishPreviewPreparationService,
			PublishPreviewDraftService publishPreviewDraftService,
			PublishExecutionService publishExecutionService) {
		this.publishPreviewPreparationService = publishPreviewPreparationService;
		this.publishPreviewDraftService = publishPreviewDraftService;
		this.publishExecutionService = publishExecutionService;
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

		UserMaster loginUser = LoginUserUtil.getLoginUser(session);

		if (loginUser == null) {
			return "redirect:/login";
		}

		Long userId = loginUser.getUserId();

		// 保存前のワークと投稿済み記事を取得する。
		PublishPreviewPreparationService.ExistingArticleData existingData = publishPreviewPreparationService
				.findExistingArticleData(workId, userId);

		// 記事とカテゴリー情報をワークテーブルへ保存する。
		Long savedWorkId = publishPreviewDraftService.saveDraft(
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

		return renderPreview(result, model);
	}

	/**
	 * 投稿確認画面への復帰要求を処理する。
	 *
	 * セッションに保持された記事情報を取得し、
	 * 投稿確認画面の表示準備をPublishPreviewPreparationServiceへ委譲する。
	 */
	@GetMapping("/publish/preview/back")
	public String backToPreview(HttpSession session, Model model) {

		// ログイン状態を確認する。
		UserMaster loginUser = LoginUserUtil.getLoginUser(session);

		if (loginUser == null) {
			return "redirect:/login";
		}

		// 投稿確認画面へ戻す記事情報をセッションから取得する。
		String title = (String) session.getAttribute("previewTitle");
		String content = (String) session.getAttribute("previewContent");
		Long workId = (Long) session.getAttribute("previewWorkId");

		Long userId = loginUser.getUserId();

		// 保存前のワークと投稿済み記事を取得する。
		PublishPreviewPreparationService.ExistingArticleData existingData = publishPreviewPreparationService
				.findExistingArticleData(workId, userId);

		// 保存済みワークのスラッグを優先して投稿確認画面の表示情報を準備する。
		PublishPreviewPreparationService.PreparationResult result = publishPreviewPreparationService.preparePreview(
				workId,
				title,
				content,
				userId,
				existingData,
				true);

		return renderPreview(result, model);
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
			@RequestParam String slug,
			@RequestParam(required = false) String thumbnailUrl,
			HttpSession session,
			Model model) {

		// ログイン状態を確認する。
		UserMaster loginUser = LoginUserUtil.getLoginUser(session);

		if (loginUser == null) {
			return "redirect:/login";
		}

		// 投稿確定後の処理をサービスへ委譲する。
		boolean started = publishExecutionService.executePublish(
				workId,
				slug,
				thumbnailUrl,
				loginUser.getUserId(),
				loginUser.getCipherKey());

		// 投稿先リポジトリが未設定の場合はエラー画面を表示する。
		if (!started) {
			model.addAttribute("error", "リポジトリが設定されていません");
			return "error";
		}

		return "redirect:/home";
	}

	/**
	 * 記事の投稿完了を待機する画面を表示する。
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

	/**
	 * 記事投稿失敗時に共通エラー画面を表示する。
	 */
	@GetMapping("/error")
	public String showPublishError(Model model) {

		model.addAttribute(
				"errorMessage",
				"記事の投稿に失敗しました。投稿状況を確認し、必要に応じて再投稿してください。");

		model.addAttribute(
				"errorReason",
				"GitHubへのPushまたはGitHub Actionsによるデプロイに失敗した可能性があります。");

		return "error";
	}

	/**
	 * 確認画面の表示準備結果をModelへ設定し、遷移先のビュー名を返す。
	 *
	 * 準備に失敗した場合はエラー画面へ遷移する。
	 */
	private String renderPreview(
			PublishPreviewPreparationService.PreparationResult result,
			Model model) {

		if (!result.isSuccess()) {
			model.addAttribute("error", result.getErrorMessage());
			return "error";
		}

		PublishPreviewService.PublishPreviewData previewData = result.getPreviewData();

		model.addAttribute("analysis", previewData.getAnalysis());
		model.addAttribute("form", previewData.getForm());

		return "publish_preview";
	}
}