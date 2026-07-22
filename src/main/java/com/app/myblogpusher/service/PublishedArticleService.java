/**
 * 投稿済み記事の取得を担当するサービス
 * GitHub APIを使用してリポジトリから記事一覧を取得する
 * tmpディレクトリではなくGitHub API経由で取得するためRender環境に対応
 *
 * カテゴリールート(movie/note/drama/tech)配下は任意階層のフォルダ構成を許容するため、
 * 一覧取得はGit Trees API(recursive=1)で全ファイルパスを取得する
 */

package com.app.myblogpusher.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.dto.PublishedArticleDto;
import com.app.myblogpusher.dto.PublishedArticleSummaryDto;
import com.app.myblogpusher.entity.Article;
import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.repository.ArticleRepository;
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
	private ArticleService articleService;

	@Autowired
	private ArticleCategoryService articleCategoryService;

	@Autowired
	private ArticleRepository articleRepository;

	public List<PublishedArticleSummaryDto> getPublishedArticles(UserRepositoryEntity repo, String cipherKey,
			HttpSession session)
			throws IOException {

		// キャッシュを確認
		if (session != null) {
			@SuppressWarnings("unchecked")
			List<PublishedArticleSummaryDto> cached = (List<PublishedArticleSummaryDto>) session
					.getAttribute("publishedArticlesCache");

			if (cached != null) {
				return cached;
			}
		}

		String accessToken = tokenCipherService.decrypt(
				repo.getAccessToken(),
				repo.getTokenIv(),
				cipherKey);

		String owner = repo.getRepoOwner();
		String repoName = repo.getRepoName();

		String defaultBranch = fetchDefaultBranch(owner, repoName, accessToken);
		List<String> mdPaths = fetchAllMarkdownPaths(owner, repoName, defaultBranch, accessToken);

		List<PublishedArticleSummaryDto> result = mdPaths.stream()
				.map(path -> {
					try {
						String contentApiUrl = "https://api.github.com/repos/" + owner + "/" + repoName + "/contents/"
								+ path;
						String mdContent = fetchContentViaApi(contentApiUrl, accessToken);
						String hugoPath = path
								.replaceFirst("^content/", "")
								.replaceFirst("\\.md$", "");

						String slug = hugoPath.substring(
								hugoPath.lastIndexOf("/") + 1);

						String title = frontMatterUtil.extractTitle(mdContent);
						LocalDateTime updateDate = frontMatterUtil.extractDate(mdContent);
						List<String> categories = frontMatterUtil.extractCategories(mdContent);

						return new PublishedArticleSummaryDto(
								slug,
								hugoPath,
								title,
								updateDate,
								mdContent,
								categories);
					} catch (IOException e) {
						return null;
					}
				})
				.filter(a -> a != null)
				.sorted((a, b) -> {
					if (a.getUpdateDate() == null)
						return 1;
					if (b.getUpdateDate() == null)
						return -1;
					return b.getUpdateDate().compareTo(a.getUpdateDate());
				})
				.toList();

		System.out.println("Articles count: " + result.size());

		if (session != null) {
			session.setAttribute(
					"publishedArticlesCache",
					result);
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

	/**
	 * リポジトリのデフォルトブランチ名を取得する
	 */
	private String fetchDefaultBranch(String owner, String repoName, String token) throws IOException {
		String apiUrl = "https://api.github.com/repos/" + owner + "/" + repoName;

		HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
		conn.setRequestProperty("Authorization", "token " + token);
		conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

		String response = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		ObjectMapper mapper = new ObjectMapper();
		JsonNode json = mapper.readTree(response);
		return json.get("default_branch").asText();
	}

	/**
	 * Git Trees APIでリポジトリ全体のファイルパスを再帰的に取得し、
	 * カテゴリールート(movie/note/drama/tech)配下の.mdファイルのみに絞り込む
	 */
	private List<String> fetchAllMarkdownPaths(String owner, String repoName, String branch, String token)
			throws IOException {
		String apiUrl = "https://api.github.com/repos/" + owner + "/" + repoName
				+ "/git/trees/" + branch + "?recursive=1";

		HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
		conn.setRequestProperty("Authorization", "token " + token);
		conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

		if (conn.getResponseCode() != 200) {
			return new ArrayList<>();
		}

		String response = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		ObjectMapper mapper = new ObjectMapper();
		JsonNode json = mapper.readTree(response);
		JsonNode tree = json.get("tree");

		return StreamSupport.stream(tree.spliterator(), false)
				.filter(node -> "blob".equals(node.get("type").asText()))
				.map(node -> node.get("path").asText())
				.filter(this::isUnderCategoryRoot)
				.toList();
	}

	/**
	 * パスが content/{カテゴリールート}/ 配下の.mdファイルかどうかを判定する
	 */
	private boolean isUnderCategoryRoot(String path) {

		if (!path.startsWith("content/") || !path.endsWith(".md")) {
			return false;
		}

		if (path.endsWith("/_index.md")) {
			return false;
		}

		String rest = path.substring("content/".length());

		return rest.contains("/");
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

	/**
	 * GitHub上の記事をArticleテーブルへ同期する
	 */
	@Async
	public void syncArticles(
			UserRepositoryEntity repo,
			String cipherKey,
			Long userId) {

		try {

			List<PublishedArticleSummaryDto> articles = getPublishedArticles(repo, cipherKey, null);

			System.out.println("同期対象件数：" + articles.size());

			for (PublishedArticleSummaryDto summary : articles) {

				Optional<Article> existing = articleRepository.findByUserIdAndHugoPath(
						userId,
						summary.getHugoPath());

				Long articleId = existing
						.map(Article::getArticleId)
						.orElse(null);

				Long categoryId = null;

				String[] pathParts = summary.getHugoPath().split("/");

				if (pathParts.length > 1) {

					String categoryName = pathParts[0];

					categoryId = articleCategoryService
							.findByUserIdAndName(userId, categoryName)
							.map(c -> c.getCategoryId())
							.orElseGet(() -> articleCategoryService.insertCategory(
									userId,
									categoryName,
									null,
									categoryName));
				}

				if (existing.isEmpty()) {

					articleService.saveFromGitHub(
							articleId,
							userId,
							categoryId,
							summary.getSlug(),
							summary.getHugoPath(),
							summary.getTitle(),
							summary.getContent(),
							summary.getUpdateDate());

				} else {

					Article dbArticle = existing.get();

					if (summary.getUpdateDate() != null
							&& (dbArticle.getUpdateDate() == null
									|| summary.getUpdateDate().isAfter(
											dbArticle.getUpdateDate()))) {

						articleService.saveFromGitHub(
								articleId,
								userId,
								categoryId,
								summary.getSlug(),
								summary.getHugoPath(),
								summary.getTitle(),
								summary.getContent(),
								summary.getUpdateDate());
					}
				}

				List<Article> dbArticles = articleRepository.findByUserId(userId);

				for (Article dbArticle : dbArticles) {

					boolean exists = articles.stream()
							.anyMatch(github -> github.getHugoPath()
									.equals(dbArticle.getHugoPath()));

					if (!exists) {

						articleService.deleteByUserIdAndSlug(
								userId,
								dbArticle.getSlug());

						System.out.println(
								"削除：" + dbArticle.getHugoPath());
					}
				}
			}

			List<Article> dbArticles = articleRepository.findByUserId(userId);

			for (Article dbArticle : dbArticles) {

				boolean exists = articles.stream()
						.anyMatch(github -> github.getHugoPath()
								.equals(dbArticle.getHugoPath()));

				if (!exists) {

					articleService.deleteByUserIdAndSlug(
							userId,
							dbArticle.getSlug());

					System.out.println(
							"削除：" + dbArticle.getHugoPath());
				}
			}

		} catch (IOException e) {

			System.err.println(
					"投稿済み記事同期に失敗しました: "
							+ e.getMessage());
		}
	}

}