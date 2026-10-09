/**
 * 記事リンク挿入機能を担当するコントローラー
 *
 * 編集画面から別記事へのリンクを作成するため、
 * 投稿済み記事一覧とリンク対象カテゴリー一覧をJSONで返却する。
 *
 * データ取得処理はArticleLinkServiceに委譲し、
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
import com.app.myblogpusher.dto.Category.CategoryOptionView;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.service.Article.ArticleLinkService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/article/link")
public class ArticleLinkController {

	@Autowired
	private ArticleLinkService articleLinkService;

	/**
	 * リンク挿入用の記事一覧取得
	 *
	 * カテゴリー未指定の場合は投稿済み記事をすべて取得する。
	 * カテゴリー指定時は、そのカテゴリーの記事だけを取得する。
	 */
	@GetMapping("/articles")
	public List<ArticleLinkView> getLinkArticles(
			@RequestParam(required = false) Long categoryGroupId,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			return List.of();
		}

		return articleLinkService.findLinkArticles(
				loginUser.getUserId(),
				categoryGroupId);
	}

	/**
	 * リンク挿入用のカテゴリー一覧取得
	 *
	 * 投稿済み記事が存在するカテゴリーだけを返却する。
	 */
	@GetMapping("/categories")
	public List<CategoryOptionView> getLinkCategories(
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			return List.of();
		}

		return articleLinkService.findLinkCategories(
				loginUser.getUserId());
	}
}