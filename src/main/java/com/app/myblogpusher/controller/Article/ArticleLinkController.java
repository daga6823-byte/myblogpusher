/**
 * 記事リンク挿入機能を担当するコントローラー
 *
 * 編集画面から別記事へのリンクを作成するため、
 * Articleテーブルから指定カテゴリーの記事一覧をJSONで返却する。
 *
 * 投稿処理や記事編集処理とは責務を分離する。
 */

package com.app.myblogpusher.controller.Article;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.myblogpusher.dto.Article.ArticleLinkView;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.entity.Article.Article;
import com.app.myblogpusher.repository.Article.ArticleRepository;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/article/link")
public class ArticleLinkController {

	@Autowired
	private ArticleRepository articleRepository;

	/**
	 * リンク挿入用の記事一覧取得
	 *
	 * Articleテーブルから指定カテゴリーの記事を取得する。
	 */
	@GetMapping("/articles")
	public List<ArticleLinkView> getLinkArticles(
			@RequestParam Long categoryId,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			return List.of();
		}

		List<Article> articles = articleRepository
				.findByUserIdAndCategoryIdOrderByUpdateDateDesc(
						loginUser.getUserId(),
						categoryId);

		return articles.stream()
				.map(article -> new ArticleLinkView(
						article.getSlug(),
						article.getHugoPath(),
						article.getTitle(),
						article.getHugoPath()))
				.toList();
	}
}