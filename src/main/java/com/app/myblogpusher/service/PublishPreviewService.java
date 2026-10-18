
/**
 * 記事の投稿前確認画面に必要なデータの生成を担当するサービス
 *
 * 投稿確認フォームの組み立て、スラッグの決定、
 * サムネイルおよびリポジトリ情報の設定を担当する。
 * 記事の保存やGitHubへの投稿処理は担当しない。
 */
package com.app.myblogpusher.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.app.myblogpusher.dto.SlugAnalysisDto;
import com.app.myblogpusher.dto.Publish.PublishPreviewForm;
import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.entity.Article.Article;
import com.app.myblogpusher.entity.Article.ArticleWork;
import com.app.myblogpusher.util.SlugUtil;

@Service
public class PublishPreviewService {

	private final SlugUtil slugUtil;

	/**
	 * スラッグ解析・生成に使用するサービスを受け取る。
	 */
	public PublishPreviewService(SlugUtil slugUtil) {
		this.slugUtil = slugUtil;
	}

	/**
	 * 投稿前確認画面に表示するフォームとスラッグ解析結果を生成する。
	 *
	 * スラッグは既存値を優先し、未設定の場合に限り新規生成する。
	 * サムネイルは保存済みワークを優先し、なければ投稿済み記事から引き継ぐ。
	 *
	 * @param workId 保存済みワークID
	 * @param title 記事タイトル
	 * @param content 記事本文
	 * @param categoryId 選択されたカテゴリーグループID
	 * @param work 保存済みワーク
	 * @param existingArticle 対応する投稿済み記事
	 * @param repo 投稿先リポジトリ情報
	 * @param primarySlug 最優先で使用する既存スラッグ
	 * @param fallbackSlug 最優先のスラッグがない場合に使用する既存スラッグ
	 * @return 投稿確認フォームとスラッグ解析結果
	 */
	public PublishPreviewData createPreviewData(
			Long workId,
			String title,
			String content,
			Long categoryId,
			ArticleWork work,
			Article existingArticle,
			UserRepositoryEntity repo,
			String primarySlug,
			String fallbackSlug) {

		// タイトルからスラッグ候補の解析結果を生成する。
		List<SlugAnalysisDto> analysis = slugUtil.analyzeSlug(title);

		// 投稿済み記事は既存スラッグを維持する。
		// 新規記事・下書きは現在のタイトルと辞書からスラッグを生成する。
		String slug;

		if (existingArticle != null
				&& primarySlug != null
				&& !primarySlug.isBlank()) {
			slug = primarySlug;
		} else if (existingArticle != null
				&& fallbackSlug != null
				&& !fallbackSlug.isBlank()) {
			slug = fallbackSlug;
		} else {
			slug = slugUtil.generateSlugFromAnalysis(analysis);
		}

		// 投稿確認画面に渡す基本情報を設定する。
		PublishPreviewForm form = new PublishPreviewForm();
		form.setArticleId(workId);
		form.setArticleTitle(title);
		form.setArticleContent(content);
		form.setCategoryId(categoryId);
		form.setSlug(slug);

		// 保存済みワークのサムネイルを最初に取得する。
		String thumbnailUrl = work != null
				? work.getThumbnailUrl()
				: null;

		// ワークにサムネイルがなければ、投稿済み記事の値を引き継ぐ。
		if ((thumbnailUrl == null || thumbnailUrl.isBlank())
				&& existingArticle != null) {
			thumbnailUrl = existingArticle.getThumbnailUrl();
		}

		// サムネイルと投稿先リポジトリの情報をフォームに設定する。
		form.setThumbnailUrl(thumbnailUrl);
		form.setRepoOwner(repo.getRepoOwner());
		form.setRepoName(repo.getRepoName());

		// コントローラーが画面表示に必要なデータをまとめて返す。
		return new PublishPreviewData(form, analysis);
	}

	/**
	 * 投稿確認画面のフォームとスラッグ解析結果をまとめて保持するデータクラス
	 */
	public static class PublishPreviewData {

		private final PublishPreviewForm form;
		private final List<SlugAnalysisDto> analysis;

		/**
		 * 画面表示に必要なフォームと解析結果を保持する。
		 */
		public PublishPreviewData(
				PublishPreviewForm form,
				List<SlugAnalysisDto> analysis) {
			this.form = form;
			this.analysis = analysis;
		}

		/** 投稿確認フォームを返す。 */
		public PublishPreviewForm getForm() {
			return form;
		}

		/** スラッグ解析結果を返す。 */
		public List<SlugAnalysisDto> getAnalysis() {
			return analysis;
		}
	}
}
