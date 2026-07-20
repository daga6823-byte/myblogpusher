package com.app.myblogpusher.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.ArticleWork;
import com.app.myblogpusher.repository.ArticleWorkRepository;

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
			Long categoryId,
			String title,
			String content,
			String slug) {

		ArticleWork work = new ArticleWork();

		work.setUserId(userId);
		work.setCategoryId(categoryId);
		work.setTitle(title);
		work.setContent(content);

		// 投稿時に使用するslugを保持
		work.setSlug(slug);

		work.setCreateUser(userId);
		work.setUpdateUser(userId);

		LocalDateTime now = LocalDateTime.now();

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
			Long categoryId,
			String title,
			String content,
			Long userId,
			String slug) {

		ArticleWork work =
				articleWorkRepository.findById(workId)
						.orElseThrow();

		work.setCategoryId(categoryId);
		work.setTitle(title);
		work.setContent(content);

		// 画面で変更したslugを保持
		work.setSlug(slug);

		work.setUpdateUser(userId);
		work.setUpdateDate(LocalDateTime.now());

		articleWorkRepository.save(work);
	}


	/**
	 * workIdから編集中記事を取得する
	 */
	public ArticleWork findById(Long workId) {

		return articleWorkRepository.findById(workId)
				.orElseThrow();
	}


	/**
	 * 同一内容の下書きが存在するか確認する
	 */
	public Optional<ArticleWork> findDuplicate(
			Long userId,
			Long categoryId,
			String title,
			String content) {

		return articleWorkRepository
				.findByUserIdAndCategoryIdAndTitleAndContent(
						userId,
						categoryId,
						title,
						content);
	}


	/**
	 * 投稿完了後に編集中データを削除する
	 */
	public void delete(
			Long workId,
			Long userId) {

		ArticleWork work =
				articleWorkRepository.findById(workId)
						.orElseThrow();

		if (!work.getUserId().equals(userId)) {

			throw new IllegalStateException(
					"他のユーザーの記事は削除できません");
		}

		articleWorkRepository.delete(work);
	}


	/**
	 * slugから編集中記事を検索する
	 */
	public Optional<ArticleWork> findBySlug(String slug) {

		return articleWorkRepository.findBySlug(slug);
	}

}