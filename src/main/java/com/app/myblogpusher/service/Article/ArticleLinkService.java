/**
 * 記事リンク挿入機能のデータ取得を担当するサービス
 *
 * 投稿済み記事の一覧取得と、記事が存在するカテゴリーの抽出を行う。
 * 編集画面の表示制御や投稿処理は担当しない。
 */
package com.app.myblogpusher.service.Article;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.dto.Article.ArticleLinkView;
import com.app.myblogpusher.dto.Category.CategoryOptionView;
import com.app.myblogpusher.entity.Article.Article;
import com.app.myblogpusher.repository.Article.ArticleRepository;
import com.app.myblogpusher.service.Category.CategorySelectionService;

@Service
public class ArticleLinkService {

	@Autowired
	private ArticleRepository articleRepository;

	@Autowired
	private CategorySelectionService categorySelectionService;

	/**
	 * リンク挿入用の記事一覧を取得する。
	 *
	 * カテゴリー未指定の場合はユーザーの投稿済み記事をすべて取得する。
	 * カテゴリー指定時は、そのカテゴリーの記事だけを取得する。
	 */
	public List<ArticleLinkView> findLinkArticles(
			Long userId,
			Long categoryGroupId) {

		List<Article> articles;

		if (categoryGroupId == null) {
			articles = articleRepository
					.findByUserIdOrderByUpdateDateDesc(userId);
		} else {
			articles = articleRepository
					.findByUserIdAndCategoryGroupIdOrderByUpdateDateDesc(
							userId,
							categoryGroupId);
		}

		return articles.stream()
				.map(article -> new ArticleLinkView(
						article.getSlug(),
						article.getHugoPath(),
						article.getTitle(),
						article.getHugoPath()))
				.toList();
	}

	/**
	 * リンク挿入用のカテゴリー一覧を取得する。
	 *
	 * 投稿済み記事が1件以上存在するカテゴリーだけを返す。
	 * カテゴリーの表示形式と階層情報は既存サービスの結果を維持する。
	 */
	public List<CategoryOptionView> findLinkCategories(Long userId) {

		List<Long> articleCategoryGroupIds = articleRepository
				.findByUserIdOrderByUpdateDateDesc(userId)
				.stream()
				.map(Article::getCategoryGroupId)
				.filter(groupId -> groupId != null)
				.distinct()
				.toList();

		if (articleCategoryGroupIds.isEmpty()) {
			return List.of();
		}

		return categorySelectionService.findSelectableCategories(userId)
				.stream()
				.filter(category -> articleCategoryGroupIds
						.contains(category.getGroupId()))
				.toList();
	}
}
