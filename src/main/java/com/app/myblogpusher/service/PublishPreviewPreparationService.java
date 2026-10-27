
/**
 * 投稿確認画面の表示準備を担当するサービス
 *
 * 保存前のワークと投稿済み記事の取得、保存後のワークと投稿先リポジトリの取得、
 * および投稿確認画面に必要なデータの準備を担当する。
 * フォームの組み立てとスラッグ生成はPublishPreviewServiceへ委譲する。
 * 記事の保存とGitHubへの投稿処理は担当しない。
 */
package com.app.myblogpusher.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.entity.Article.Article;
import com.app.myblogpusher.entity.Article.ArticleWork;
import com.app.myblogpusher.repository.UserRepositoryRepository;
import com.app.myblogpusher.service.Article.ArticleService;
import com.app.myblogpusher.service.Article.ArticleWorkService;

@Service
public class PublishPreviewPreparationService {

	private final ArticleWorkService articleWorkService;
	private final ArticleService articleService;
	private final UserRepositoryRepository userRepositoryRepository;
	private final PublishPreviewService publishPreviewService;

	/**
	 * 投稿確認画面の準備に必要な依存関係を設定する。
	 */
	public PublishPreviewPreparationService(
			ArticleWorkService articleWorkService,
			ArticleService articleService,
			UserRepositoryRepository userRepositoryRepository,
			PublishPreviewService publishPreviewService) {
		this.articleWorkService = articleWorkService;
		this.articleService = articleService;
		this.userRepositoryRepository = userRepositoryRepository;
		this.publishPreviewService = publishPreviewService;
	}

	/**
	 * 保存前のワークと編集元の記事を取得する。
	 *
	 * 投稿確認へ進む前に呼び出し、既存スラッグなどの引き継ぎ元を保持する。
	 *
	 * @param workId 編集対象のワークID
	 * @return 保存前のワークと投稿済み記事
	 */
	public ExistingArticleData findExistingArticleData(
	        Long workId,
	        Long userId) {

		// 新規記事の場合は、引き継ぎ元のデータがない状態を返す。
		if (workId == null) {
			return new ExistingArticleData(null, null);
		}

		// 保存前のワークをログインユーザー本人のデータに限定して取得する。
		ArticleWork existingWork = articleWorkService.findById(workId, userId);

		// ワークに投稿済み記事との関連があれば、その記事も取得する。
		Article existingArticle = null;

		if (existingWork != null && existingWork.getArticleId() != null) {
			existingArticle = articleService.findById(
					existingWork.getArticleId());
		}

		return new ExistingArticleData(existingWork, existingArticle);
	}

	/**
	 * 投稿済み記事のスラッグを優先して、投稿確認画面の表示情報を準備する。
	 *
	 * 通常の投稿確認画面表示で使用する。
	 *
	 * @param savedWorkId 保存後のワークID
	 * @param title 記事タイトル
	 * @param content 記事本文
	 * @param userId ログインユーザーID
	 * @param existingData 保存前に取得した編集元データ
	 * @return 投稿確認画面の準備結果
	 */
	public PreparationResult preparePreview(
			Long savedWorkId,
			String title,
			String content,
			Long userId,
			ExistingArticleData existingData) {

		return preparePreview(
				savedWorkId,
				title,
				content,
				userId,
				existingData,
				false);
	}

	/**
	 * 指定された優先順位でスラッグを引き継ぎ、投稿確認画面の表示情報を準備する。
	 *
	 * @param savedWorkId 保存後のワークID
	 * @param title 記事タイトル
	 * @param content 記事本文
	 * @param userId ログインユーザーID
	 * @param existingData 保存前に取得した編集元データ
	 * @param preferWorkSlug trueの場合はワークのスラッグを優先する
	 * @return 投稿確認画面の準備結果
	 */
	public PreparationResult preparePreview(
			Long savedWorkId,
			String title,
			String content,
			Long userId,
			ExistingArticleData existingData,
			boolean preferWorkSlug) {

		// 保存後のワークを取得する。
		ArticleWork work = articleWorkService.findById(savedWorkId, userId);

		if (work == null) {
			return PreparationResult.failure(
					"保存した記事を取得できませんでした");
		}

		// 投稿先リポジトリの設定を確認する。
		Optional<UserRepositoryEntity> repoOpt = userRepositoryRepository.findByUserId(userId);

		if (repoOpt.isEmpty()) {
			return PreparationResult.failure(
					"リポジトリが設定されていません");
		}

		UserRepositoryEntity repo = repoOpt.get();

		ArticleWork existingWork = existingData.getWork();
		Article existingArticle = existingData.getArticle();

		// 呼び出し元の用途に応じて、優先するスラッグと代替スラッグを決定する。
		String primarySlug;
		String fallbackSlug;

		if (preferWorkSlug) {
			primarySlug = existingWork != null
					? existingWork.getSlug()
					: null;

			fallbackSlug = existingArticle != null
					? existingArticle.getSlug()
					: null;
		} else {
			primarySlug = existingArticle != null
					? existingArticle.getSlug()
					: null;

			fallbackSlug = existingWork != null
					? existingWork.getSlug()
					: null;
		}

		// 保存後のカテゴリーグループIDと既存情報から確認画面のデータを生成する。
		PublishPreviewService.PublishPreviewData previewData = publishPreviewService.createPreviewData(
				savedWorkId,
				title,
				content,
				work.getCategoryGroupId(),
				work,
				existingArticle,
				repo,
				primarySlug,
				fallbackSlug);

		return PreparationResult.success(previewData);
	}

	/**
	 * 保存前に取得したワークと投稿済み記事を保持する。
	 */
	public static class ExistingArticleData {

		private final ArticleWork work;
		private final Article article;

		public ExistingArticleData(
				ArticleWork work,
				Article article) {
			this.work = work;
			this.article = article;
		}

		public ArticleWork getWork() {
			return work;
		}

		public Article getArticle() {
			return article;
		}
	}

	/**
	 * 投稿確認画面の準備結果を保持する。
	 *
	 * 成功時はプレビュー表示データを、失敗時はエラーメッセージを保持する。
	 */
	public static class PreparationResult {

		private final PublishPreviewService.PublishPreviewData previewData;
		private final String errorMessage;

		private PreparationResult(
				PublishPreviewService.PublishPreviewData previewData,
				String errorMessage) {
			this.previewData = previewData;
			this.errorMessage = errorMessage;
		}

		/**
		 * 成功結果を生成する。
		 */
		public static PreparationResult success(
				PublishPreviewService.PublishPreviewData previewData) {
			return new PreparationResult(previewData, null);
		}

		/**
		 * エラー結果を生成する。
		 */
		public static PreparationResult failure(String errorMessage) {
			return new PreparationResult(null, errorMessage);
		}

		/**
		 * プレビュー準備に成功したかを返す。
		 */
		public boolean isSuccess() {
			return previewData != null;
		}

		/**
		 * 投稿確認画面の表示データを返す。
		 */
		public PublishPreviewService.PublishPreviewData getPreviewData() {
			return previewData;
		}

		/**
		 * 準備に失敗した場合のエラーメッセージを返す。
		 */
		public String getErrorMessage() {
			return errorMessage;
		}
	}
}
