/**
 * 投稿済み記事の取得を担当するサービス
 *
 * 投稿済み記事一覧は article テーブルから取得し、
 * 個別記事の取得には既存の GitHub API 処理を使用する。
 * 投稿済み記事一覧の取得結果はセッションにキャッシュする。
 */
package com.app.myblogpusher.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.dto.Publish.PublishedArticleDto;
import com.app.myblogpusher.dto.Publish.PublishedArticleSummaryDto;
import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.repository.Article.ArticleRepository;
import com.app.myblogpusher.util.FrontMatterUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;

@Service
public class PublishedArticleService {

	@Autowired
	private TokenCipherService tokenCipherService;

	@Autowired
	private FrontMatterUtil frontMatterUtil;

	@Autowired
	private ArticleRepository articleRepository;

	/**
	 * DBから投稿済み記事一覧を取得する。
	 *
	 * ログインユーザーの投稿済み記事を更新日時の降順で取得する。
	 * 取得結果はセッションにキャッシュし、一覧表示時の再検索を抑制する。
	 *
	 * @param repo ユーザーのリポジトリ情報
	 * @param cipherKey 暗号化キー
	 * @param session セッション情報
	 * @return 投稿済み記事一覧
	 * @throws IOException 記事取得処理でエラーが発生した場合
	 */
	public List<PublishedArticleSummaryDto> getPublishedArticles(
			UserRepositoryEntity repo,
			String cipherKey,
			HttpSession session) throws IOException {

		// キャッシュを確認
		if (session != null) {
			@SuppressWarnings("unchecked")
			List<PublishedArticleSummaryDto> cached = (List<PublishedArticleSummaryDto>) session
					.getAttribute("publishedArticlesCache");

			if (cached != null) {
				return cached;
			}
		}

		Long userId = repo.getUserId();

		List<PublishedArticleSummaryDto> result = articleRepository
				.findByUserIdAndStatusOrderByUpdateDateDesc(
						userId,
						com.app.myblogpusher.enums.ArticleStatus.PUBLISHED)
				.stream()
				.map(article -> new PublishedArticleSummaryDto(
						article.getSlug(),
						article.getHugoPath(),
						article.getTitle(),
						article.getUpdateDate(),
						article.getContent(),
						List.of()))
				.toList();

		if (session != null) {
			session.setAttribute("publishedArticlesCache", result);
		}

		return result;
	}

	public PublishedArticleDto getPublishedArticle(
			UserRepositoryEntity repo,
			String cipherKey,
			Long articleId,
			String hugoPath)
			throws IOException {

		String accessToken = tokenCipherService.decrypt(
				repo.getAccessToken(),
				repo.getTokenIv(),
				cipherKey);

		String apiUrl = "https://api.github.com/repos/"
				+ repo.getRepoOwner() + "/"
				+ repo.getRepoName()
				+ "/contents/content/"
				+ hugoPath
				+ ".md";

		HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();

		conn.setRequestProperty(
				"Authorization",
				"token " + accessToken);

		conn.setRequestProperty(
				"Accept",
				"application/vnd.github.v3+json");

		if (conn.getResponseCode() != 200) {
			return null;
		}

		String mdContent = fetchContentViaApi(apiUrl, accessToken);

		String slug = hugoPath.substring(
				hugoPath.lastIndexOf("/") + 1);

		String title = frontMatterUtil.extractTitle(mdContent);

		List<String> categories = frontMatterUtil.extractCategories(mdContent);

		LocalDateTime updateDate = frontMatterUtil.extractDate(mdContent);

		return new PublishedArticleDto(
				articleId,
				slug,
				hugoPath,
				title,
				updateDate,
				mdContent,
				categories);
	}

	private String fetchContentViaApi(String url, String token) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setRequestProperty("Authorization", "token " + token);
		conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

		String response = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

		// Base64デコード
		ObjectMapper mapper = new ObjectMapper();
		JsonNode json = mapper.readTree(response);
		String encoded = json.get("content").asText().replaceAll("\\s", "");
		return new String(java.util.Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
	}

	/**
	 * ログイン直後に投稿済み記事一覧を非同期で先読みし、セッションにキャッシュしておく
	 * 記事一覧画面への遷移時には既にキャッシュ済みの状態を目指す
	 */
	@Async
	public void prefetchPublishedArticles(UserRepositoryEntity repo, String cipherKey, HttpSession session) {
		try {
			getPublishedArticles(repo, cipherKey, session);
		} catch (IOException e) {
			System.err.println("記事一覧の先読みに失敗しました: " + e.getMessage());
		}
	}

}