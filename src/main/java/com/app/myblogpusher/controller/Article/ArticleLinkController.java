/**
 * 記事リンク挿入機能を担当するコントローラー
 *
 * 編集画面から別記事へのリンクを作成するため、
 * GitHub上の投稿済み記事一覧をJSONで返却する。
 *
 * 投稿処理や記事編集処理とは責務を分離する。
 */

package com.app.myblogpusher.controller.Article;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.myblogpusher.dto.Publish.PublishedArticleSummaryDto;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.repository.UserRepositoryRepository;
import com.app.myblogpusher.service.PublishedArticleService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/article/link")
public class ArticleLinkController {

	@Autowired
	private PublishedArticleService publishedArticleService;

	@Autowired
	private UserRepositoryRepository userRepositoryRepository;

	/**
	 * リンク挿入用の記事一覧取得
	 *
	 * title  : 表示初期値
	 * slug   : URL生成用
	 * hugoPath : 記事パス
	 */
	@GetMapping("/articles")
	public List<PublishedArticleSummaryDto> getLinkArticles(
			HttpSession session) throws IOException {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			return List.of();
		}

		Long userId = loginUser.getUserId();

		Optional<UserRepositoryEntity> repoOpt = userRepositoryRepository.findByUserId(userId);

		if (repoOpt.isEmpty()) {
			return List.of();
		}

		UserRepositoryEntity repo = repoOpt.get();

		return publishedArticleService.getPublishedArticles(
				repo,
				loginUser.getCipherKey(),
				session);

	}

}