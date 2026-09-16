/**
 * GitHubから投稿済み記事を取得するサービス
 *
 * GitHub APIを使用してMarkdown記事を取得し、
 * 投稿済み記事DTOへ変換する処理を担当する。
 *
 * DBへの記事同期処理はPublishedArticleSyncServiceが担当する。
 */
package com.app.myblogpusher.service.Github;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.dto.Publish.PublishedArticleDto;
import com.app.myblogpusher.dto.Publish.PublishedArticleSummaryDto;
import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.service.TokenCipherService;
import com.app.myblogpusher.util.FrontMatterUtil;

import jakarta.servlet.http.HttpSession;

@Service
public class GitHubArticleService {

	@Autowired
	private TokenCipherService tokenCipherService;

	@Autowired
	private FrontMatterUtil frontMatterUtil;

	@Autowired
	private GitHubApiService gitHubApiService;

	/**
	 * GitHubから投稿済み記事一覧を取得する。
	 */
	public List<PublishedArticleSummaryDto> getPublishedArticles(
			UserRepositoryEntity repo,
			String cipherKey,
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

		String defaultBranch = gitHubApiService.fetchDefaultBranch(
				owner,
				repoName,
				accessToken);

		List<String> mdPaths = gitHubApiService.fetchAllMarkdownPaths(
				owner,
				repoName,
				defaultBranch,
				accessToken);

		List<PublishedArticleSummaryDto> result = mdPaths.stream()
				.map(path -> {
					try {
						String contentApiUrl = "https://api.github.com/repos/"
								+ owner + "/"
								+ repoName
								+ "/contents/"
								+ path;

						String mdContent = gitHubApiService.fetchContentViaApi(
								contentApiUrl,
								accessToken);

						String hugoPath = path
								.replaceFirst(
										"^content/",
										"")
								.replaceFirst(
										"\\.md$",
										"");

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

					return b.getUpdateDate()
							.compareTo(a.getUpdateDate());
				})
				.toList();

		if (session != null) {
			session.setAttribute(
					"publishedArticlesCache",
					result);
		}

		return result;
	}

	/**
	 * GitHub上の指定記事を取得する。
	 */
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

		String mdContent = gitHubApiService.fetchContentViaApi(
				apiUrl,
				accessToken);

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
}