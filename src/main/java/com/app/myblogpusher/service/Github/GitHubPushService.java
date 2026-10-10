/**
 * 記事をGitHubリポジトリにMarkdownファイルとしてプッシュするサービス
 *
 * 記事のHugoパス移動、Markdown生成の呼び出し、commit、pushを担当する。
 * リポジトリの準備はGitWorkspaceServiceへ、
 * Hugo用Markdown生成はHugoArticleServiceへ委譲する。
 */

package com.app.myblogpusher.service.Github;

import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.entity.Article.Article;
import com.app.myblogpusher.service.HugoArticleService;

@Service
public class GitHubPushService {

	private final GitWorkspaceService gitWorkspaceService;
	private final HugoArticleService hugoArticleService;

	public GitHubPushService(
			GitWorkspaceService gitWorkspaceService,
			HugoArticleService hugoArticleService) {

		this.gitWorkspaceService = gitWorkspaceService;
		this.hugoArticleService = hugoArticleService;
	}

	/**
	 * 記事をMarkdownファイルとしてGitHubへプッシュ
	 */
	public void pushArticle(
			UserRepositoryEntity repoEntity,
			String cipherKey,
			Article article,
			Article existingArticle,
			boolean newArticle,
			String slug)
			throws IOException, GitAPIException {

		try (GitWorkspace workspace = gitWorkspaceService.open(
				repoEntity,
				cipherKey)) {

			Git git = workspace.getGit();
			String repoPath = workspace.getRepoPath();

			System.out.println("=== GitHubPushService.pushArticle ===");
			System.out.println("newArticle       = " + newArticle);
			System.out.println("article.articleId = " + article.getArticleId());
			System.out.println("article.slug      = " + article.getSlug());
			System.out.println("article.hugoPath  = " + article.getHugoPath());

			if (existingArticle != null) {
				System.out.println("existingArticle.articleId = "
						+ existingArticle.getArticleId());
				System.out.println("existingArticle.hugoPath  = "
						+ existingArticle.getHugoPath());
			} else {
				System.out.println("existingArticle = null");
			}

			// 既存記事のカテゴリー変更などでHugoパスが変わった場合、
			// 既存ファイルを削除せず、新しいパスへ移動する。
			if (existingArticle != null
					&& existingArticle.getHugoPath() != null
					&& !existingArticle.getHugoPath().equals(article.getHugoPath())) {

				System.out.println("=== Hugoパス変更検出 ===");
				System.out.println("旧パス: " + existingArticle.getHugoPath());
				System.out.println("新パス: " + article.getHugoPath());

				hugoArticleService.moveArticle(
						git,
						repoPath,
						existingArticle.getHugoPath(),
						article.getHugoPath());
			}

			hugoArticleService.createArticle(
					git,
					repoPath,
					article,
					slug);

			String commitMessage = newArticle
					? "Add article: "
					: "Update article: ";

			git.commit()
					.setMessage(commitMessage + article.getSlug())
					.setAuthor(
							"Myblogpusher",
							"noreply@myblogpusher.local")
					.call();

			workspace.push();
		}
	}
}