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
	 * カテゴリー指定時は、groupIdからカテゴリー経路を取得し、
	 * HugoPathがそのカテゴリー配下にある記事だけを取得する。
	 */
	public List<ArticleLinkView> findLinkArticles(
	        Long userId,
	        Long categoryGroupId) {

	    List<CategoryOptionView> categories =
	            categorySelectionService.findSelectableCategories(userId);

	    List<Article> articles;

	    if (categoryGroupId == null) {

	        articles = articleRepository
	                .findByUserIdOrderByUpdateDateDesc(userId);

	    } else {

	        String categoryPath = categories.stream()
	                .filter(category -> categoryGroupId.equals(
	                        category.getGroupId()))
	                .map(CategoryOptionView::getCategoryPath)
	                .findFirst()
	                .orElse(null);

	        if (categoryPath == null) {
	            return List.of();
	        }

	        articles = articleRepository.findLinkArticles(
	                userId,
	                categoryPath + "/");
	    }

	    return articles.stream()
	            .map(article -> {

	                String articleCategoryPath =
	                        extractCategoryPath(article.getHugoPath());

	                Long groupId = categories.stream()
	                        .filter(category -> category.getCategoryPath()
	                                .equals(articleCategoryPath))
	                        .map(CategoryOptionView::getGroupId)
	                        .findFirst()
	                        .orElse(null);

	                return new ArticleLinkView(
	                        article.getSlug(),
	                        article.getHugoPath(),
	                        article.getTitle(),
	                        article.getHugoPath(),
	                        groupId);
	            })
	            .toList();
	}

	/**
	 * 投稿済み記事が存在するカテゴリー一覧を取得する。
	 *
	 * ArticleのHugoPathからカテゴリー経路を抽出し、
	 * 既存のカテゴリー選択肢と照合する。
	 */
	public List<CategoryOptionView> findLinkCategories(Long userId) {

	    List<Article> articles = articleRepository.findByUserId(userId);

	    if (articles.isEmpty()) {
	        return List.of();
	    }

	    List<String> articleCategoryPaths = articles.stream()
	            .map(article -> extractCategoryPath(article.getHugoPath()))
	            .distinct()
	            .toList();

	    return categorySelectionService.findSelectableCategories(userId)
	            .stream()
	            .filter(category -> articleCategoryPaths.contains(
	                    category.getCategoryPath()))
	            .toList();
	}

	/**
	 * HugoPathから末尾のslugを除いたカテゴリー経路を取得する。
	 *
	 * 例:
	 * movie/batman/dark-knight
	 * → movie/batman
	 */
	private String extractCategoryPath(String hugoPath) {

	    if (hugoPath == null || hugoPath.isBlank()) {
	        return "";
	    }

	    int lastSlash = hugoPath.lastIndexOf("/");

	    if (lastSlash < 0) {
	        return "";
	    }

	    return hugoPath.substring(0, lastSlash);
	}
}
