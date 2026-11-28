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
package com.app.myblogpusher.service.Article;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.Article.ArticleWorkEntity;
import com.app.myblogpusher.repository.Article.ArticleWorkRepository;

import jakarta.transaction.Transactional;

@Service
public class ArticleWorkService {

	@Autowired
	private ArticleWorkRepository articleWorkRepository;

	/**
	 * ユーザーの編集中記事一覧を取得する
	 */
	public List<ArticleWorkEntity> findByUserId(Long userId) {

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
	        String hugoPath,
	        Long typoCategoryId,
	        String title,
	        String content,
	        String slug) {

	    ArticleWorkEntity work = new ArticleWorkEntity();

	    work.setUserId(userId);
	    work.setArticleId(articleId);
	    work.setHugoPath(hugoPath);
	    work.setTypoCategoryId(typoCategoryId);
		work.setTitle(title);
		work.setContent(content);
		work.setSlug(slug);
		work.setCreateUser(userId);
		work.setUpdateUser(userId);

		LocalDateTime now = LocalDateTime.now();

		// 投稿前
		work.setStatus(0);
		work.setCreateDate(now);

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
	        String hugoPath,
	        Long typoCategoryId,
	        String title,
	        String content,
	        Long userId,
	        String slug) {

	    ArticleWorkEntity work = articleWorkRepository
	            .findByWorkIdAndUserId(workId, userId)
	            .orElseThrow();

	    work.setHugoPath(hugoPath);
	    work.setTypoCategoryId(typoCategoryId);
		work.setTitle(title);
		work.setContent(content);
		work.setSlug(slug);
		work.setUpdateUser(userId);

		articleWorkRepository.save(work);
	}

	/**
	 * ログインユーザーが所有する編集中記事を取得する
	 */
	public ArticleWorkEntity findById(Long workId, Long userId) {

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

		ArticleWorkEntity work = articleWorkRepository
				.findByWorkIdAndUserId(workId, userId)
				.orElseThrow();

		work.setStatus(status);
		work.setUpdateUser(userId);

		articleWorkRepository.save(work);
	}

	/**
	 * 投稿前の記事を投稿中に変更する
	 *
	 * 条件付き更新により、連打や同時リクエストによる二重投稿を防ぐ。
	 *
	 * @return 更新できた場合はtrue
	 */
	@Transactional
	public boolean startPublishing(Long workId, Long userId) {

		return articleWorkRepository.startPublishing(workId, userId) == 1;
	}

	public void updateStatus(
			Long workId,
			Integer status,
			String errorCode,
			Long userId) {

		ArticleWorkEntity work = articleWorkRepository
				.findByWorkIdAndUserId(workId, userId)
				.orElseThrow();

		work.setStatus(status);
		work.setErrorCode(errorCode);
		work.setUpdateUser(userId);

		articleWorkRepository.save(work);
	}

	/**
	 * 同一内容の下書きが存在するか確認する
	 */
	public Optional<ArticleWorkEntity> findDuplicate(
			Long userId,
			String hugoPath,
			String title,
			String content) {

		return articleWorkRepository
				.findByUserIdAndHugoPathAndTitleAndContent(
						userId,
						hugoPath,
						title,
						content);
	}

	/**
	 * ユーザーが所有する編集中データを削除する
	 */
	public void delete(
			Long workId,
			Long userId) {

		ArticleWorkEntity work = articleWorkRepository
				.findByWorkIdAndUserId(workId, userId)
				.orElseThrow();

		articleWorkRepository.delete(work);
	}

	/**
	 * 投稿済み記事のarticleIdから編集中記事を検索する
	 */
	public Optional<ArticleWorkEntity> findByArticleId(Long articleId) {

		return articleWorkRepository.findByArticleId(articleId);
	}

	/**
	 * 投稿中の記事を取得する
	 */
	public List<ArticleWorkEntity> findPublishing(Long userId) {

		return articleWorkRepository.findByUserIdAndStatusOrderByUpdateDateDesc(
				userId,
				1);
	}

	/**
	 * エラー記事を取得する
	 */
	public List<ArticleWorkEntity> findError(Long userId) {
		return articleWorkRepository.findByUserIdAndStatusOrderByUpdateDateDesc(userId, 2);
	}

	/**
	 * 投稿前の下書きを取得する
	 */
	public List<ArticleWorkEntity> findDrafts(Long userId) {

		return articleWorkRepository.findByUserIdAndStatusOrderByUpdateDateDesc(
				userId,
				0);
	}

	/**
	 * ユーザーが所有する編集中記事のslugを更新する
	 */
	public void updateSlug(
			Long workId,
			String slug,
			Long userId) {

		ArticleWorkEntity work = articleWorkRepository
				.findByWorkIdAndUserId(workId, userId)
				.orElseThrow();

		work.setSlug(slug);
		work.setUpdateUser(userId);

		articleWorkRepository.save(work);
	}

	/**
	 * ユーザーが所有する記事のサムネイルURLを更新する
	 */
	public void updateThumbnailUrl(
			Long workId,
			Long userId,
			String thumbnailUrl) {

		ArticleWorkEntity work = articleWorkRepository
				.findByWorkIdAndUserId(workId, userId)
				.orElseThrow();

		work.setThumbnailUrl(thumbnailUrl);
		work.setUpdateUser(userId);

		articleWorkRepository.save(work);
	}

	/**
	 * ユーザーが所有する記事の投稿確認画面で編集したslugを保存する
	 */
	public void updatePreviewSlug(
			Long workId,
			Long userId,
			String slug) {

		ArticleWorkEntity work = articleWorkRepository
				.findByWorkIdAndUserId(workId, userId)
				.orElseThrow();

		work.setSlug(slug);
		work.setUpdateUser(userId);

		articleWorkRepository.save(work);
	}

	/**
	 * ArticleWorkのHugoパスを更新する。
	 */
	@Transactional
	public void updateHugoPath(
			Long workId,
			String hugoPath,
			Long userId) {

		ArticleWorkEntity work = articleWorkRepository
				.findByWorkIdAndUserId(workId, userId)
				.orElseThrow();

		work.setHugoPath(hugoPath);
		work.setUpdateUser(userId);

		articleWorkRepository.save(work);
	}
}
