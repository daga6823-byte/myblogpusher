package com.app.myblogpusher.service.Article;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.Article.ArticleWork;
import com.app.myblogpusher.repository.Article.ArticleWorkRepository;

/**
 * 記事編集中データ(article_work)を管理するサービス
 *
 * ・編集中記事の取得
 * ・新規下書き保存
 * ・下書き更新
 * ・投稿完了後の削除
 * を担当する。
 *
 * 投稿済み記事(article)への変換処理はArticleService側で行う。
 */
@Service
public class ArticleWorkService {

	@Autowired
	private ArticleWorkRepository articleWorkRepository;

	/**
	 * ユーザーの編集中記事一覧を取得する
	 */
	public List<ArticleWork> findByUserId(Long userId) {

		return articleWorkRepository.findByUserIdOrderByUpdateDateDesc(userId);
	}

	/**
	 * 新規下書きを登録する
	 *
	 * 投稿前の編集内容とslugをarticle_workへ保存する。
	 */
	public Long insertArticleWork(
			Long userId,
			Long articleId,
			Long categoryGroupId,
			String title,
			String content,
			String slug) {

		ArticleWork work = new ArticleWork();

		work.setUserId(userId);
		work.setArticleId(articleId);
		work.setCategoryGroupId(categoryGroupId);
		work.setTitle(title);
		work.setContent(content);

		// 投稿時に使用するslugを保持
		work.setSlug(slug);

		work.setCreateUser(userId);
		work.setUpdateUser(userId);

		LocalDateTime now = LocalDateTime.now();

		// 投稿前
		work.setStatus(0);
		work.setCreateDate(now);
		work.setUpdateDate(now);

		articleWorkRepository.save(work);

		return work.getWorkId();
	}

	/**
	 * 既存下書きを更新する
	 *
	 * 画面で編集されたタイトル・本文・slugを保存する。
	 */
	public void updateArticleWork(
			Long workId,
			Long categoryGroupId,
			String title,
			String content,
			Long userId,
			String slug) {

		ArticleWork work = articleWorkRepository
				.findByWorkIdAndUserId(workId, userId)
				.orElseThrow();

		work.setCategoryGroupId(categoryGroupId);
		work.setTitle(title);
		work.setContent(content);
		work.setSlug(slug);
		work.setUpdateUser(userId);
		work.setUpdateDate(LocalDateTime.now());

		articleWorkRepository.save(work);
	}

	/**
	 * ログインユーザーが所有する編集中記事を取得する
	 */
	public ArticleWork findById(Long workId, Long userId) {

		return articleWorkRepository
				.findByWorkIdAndUserId(workId, userId)
				.orElseThrow();
	}

	/**
	 * 投稿状態を更新する
	 *
	 * 0 = 投稿前
	 * 1 = 投稿中
	 * 2 = エラー
	 */
	public void updateStatus(
			Long workId,
			Integer status,
			Long userId) {

		ArticleWork work = articleWorkRepository
				.findByWorkIdAndUserId(workId, userId)
				.orElseThrow();

		work.setStatus(status);
		work.setUpdateUser(userId);
		work.setUpdateDate(LocalDateTime.now());

		articleWorkRepository.save(work);
	}

	public void updateStatus(
			Long workId,
			Integer status,
			String errorCode,
			Long userId) {

		ArticleWork work = articleWorkRepository
				.findByWorkIdAndUserId(workId, userId)
				.orElseThrow();

		work.setStatus(status);
		work.setErrorCode(errorCode);
		work.setUpdateUser(userId);
		work.setUpdateDate(LocalDateTime.now());

		articleWorkRepository.save(work);
	}

	/**
	 * 同一内容の下書きが存在するか確認する
	 */
	public Optional<ArticleWork> findDuplicate(
			Long userId,
			Long categoryGroupId,
			String title,
			String content) {

		return articleWorkRepository
				.findByUserIdAndCategoryGroupIdAndTitleAndContent(
						userId,
						categoryGroupId,
						title,
						content);
	}

	/**
	 * ユーザーが所有する編集中データを削除する
	 */
	public void delete(
			Long workId,
			Long userId) {

		ArticleWork work = articleWorkRepository
				.findByWorkIdAndUserId(workId, userId)
				.orElseThrow();

		articleWorkRepository.delete(work);
	}

	/**
	 * 投稿済み記事のarticleIdから編集中記事を検索する
	 */
	public Optional<ArticleWork> findByArticleId(Long articleId) {

		return articleWorkRepository.findByArticleId(articleId);
	}

	/**
	 * 投稿中の記事を取得する
	 */
	public List<ArticleWork> findPublishing(Long userId) {

		return articleWorkRepository.findByUserIdAndStatusOrderByUpdateDateDesc(
				userId,
				1);
	}

	/**
	 * エラー記事を取得する
	 */
	public List<ArticleWork> findError(Long userId) {
		return articleWorkRepository.findByUserIdAndStatusOrderByUpdateDateDesc(userId, 2);
	}

	/**
	 * 投稿前の下書きを取得する
	 */
	public List<ArticleWork> findDrafts(Long userId) {

		return articleWorkRepository.findByUserIdAndStatusOrderByUpdateDateDesc(
				userId,
				0);
	}

	/**
	 * ユーザーが所有する編集中記事のカテゴリーグループIDを更新する
	 */
	public void updateCategoryGroupId(
			Long workId,
			Long categoryGroupId,
			Long userId) {

		ArticleWork work = articleWorkRepository
				.findByWorkIdAndUserId(workId, userId)
				.orElseThrow();

		work.setCategoryGroupId(categoryGroupId);
		work.setUpdateUser(userId);
		work.setUpdateDate(LocalDateTime.now());

		articleWorkRepository.save(work);
	}

	/**
	 * ユーザーが所有する編集中記事のslugを更新する
	 */
	public void updateSlug(
			Long workId,
			String slug,
			Long userId) {

		ArticleWork work = articleWorkRepository
				.findByWorkIdAndUserId(workId, userId)
				.orElseThrow();

		work.setSlug(slug);
		work.setUpdateUser(userId);
		work.setUpdateDate(LocalDateTime.now());

		articleWorkRepository.save(work);
	}

	/**
	 * ユーザーが所有する記事のサムネイルURLを更新する
	 */
	public void updateThumbnailUrl(
			Long workId,
			Long userId,
			String thumbnailUrl) {

		ArticleWork work = articleWorkRepository
				.findByWorkIdAndUserId(workId, userId)
				.orElseThrow();

		work.setThumbnailUrl(thumbnailUrl);
		work.setUpdateUser(userId);
		work.setUpdateDate(LocalDateTime.now());

		articleWorkRepository.save(work);
	}

	/**
	 * ユーザーが所有する記事の投稿確認画面で編集したslugを保存する
	 */
	public void updatePreviewSlug(
			Long workId,
			Long userId,
			String slug) {

		ArticleWork work = articleWorkRepository
				.findByWorkIdAndUserId(workId, userId)
				.orElseThrow();

		work.setSlug(slug);
		work.setUpdateUser(userId);
		work.setUpdateDate(LocalDateTime.now());

		articleWorkRepository.save(work);
	}
}
