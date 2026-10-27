/**
 * 1記事分のGitHub投稿処理を担当するサービス
 *
 * 投稿ステータスとGitHub APIの投稿可否を確認し、
 * 記事の生成、GitHubへの投稿、投稿成功後のDB更新を実行する。
 * 投稿失敗時は記事をエラー状態に更新する。
 */
package com.app.myblogpusher.service.Article;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.entity.Article.Article;
import com.app.myblogpusher.entity.Article.ArticleWork;
import com.app.myblogpusher.service.HugoArticleService;
import com.app.myblogpusher.service.Facade.GitHubFacadeService;

@Service
public class ArticlePublishWorkerService {

	@Autowired
	private ArticleWorkService articleWorkService;

	@Autowired
	private ArticleService articleService;

	@Autowired
	private GitHubFacadeService gitHubFacadeService;

	@Autowired
	private HugoArticleService hugoArticleService;

	/**
	 * 1記事分の投稿処理を実行する。
	 *
	 * 投稿に失敗した場合は、記事をエラー状態に更新する。
	 */
	public void publish(
			UserRepositoryEntity repo,
			String cipherKey,
			ArticleWork work,
			String thumbnailUrl) {

		try {
			// 投稿処理中(status=1)のWorkだけを投稿する。
			// 非同期処理開始後にstatusが変更された場合の誤投稿を防ぐ。
			if (work.getStatus() == null || work.getStatus() != 1) {
				return;
			}

			// GitHub APIで投稿可能か確認する。
			if (!gitHubFacadeService.canPublish(repo, cipherKey)) {
				articleWorkService.updateStatus(
				        work.getWorkId(),
				        2,
				        "GitHub APIで投稿権限を確認できませんでした。",
				        work.getUserId());
				return;
			}

			String hugoPath = hugoArticleService.buildArticlePath(
					work.getCategoryGroupId(),
					work.getSlug());

			// ArticleWork.articleIdを基準に元の記事を取得する。
			// カテゴリー変更後のhugoPathではなく、元の記事そのものを特定する。
			Article existingArticle = null;

			if (work.getArticleId() != null) {
				existingArticle = articleService.findById(
						work.getArticleId());
			}

			// GitHub投稿用のArticleをメモリ上だけで作成する。
			Article article = new Article();
			article.setUserId(work.getUserId());
			article.setCategoryGroupId(work.getCategoryGroupId());
			article.setTitle(work.getTitle());
			article.setSlug(work.getSlug());
			article.setHugoPath(hugoPath);
			article.setContent(
					applyThumbnailToFrontMatter(
							work.getContent(),
							thumbnailUrl));
			article.setThumbnailUrl(thumbnailUrl);

			// GitHub投稿を実行する。
			// 投稿成功前にArticleテーブルへ保存しない。
			gitHubFacadeService.pushArticle(
					repo,
					cipherKey,
					article,
					existingArticle,
					existingArticle == null,
					work.getSlug());

			// GitHub投稿成功後にArticleを作成・更新する。
			// Articleの重複整理、PUBLISHEDへの変更、Work削除まで
			// DBトランザクション内でまとめて実行する。
			Article publishedArticle = articleService.completePublish(
			        work,
			        work.getSlug(),
			        thumbnailUrl);

		} catch (Exception e) {
			System.err.println(
					"投稿処理に失敗しました: " + e.getMessage());
			e.printStackTrace();

			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);

			// 投稿失敗した記事はエラー状態で残す。
			// スタックトレースを含む詳細をDBへ保存する。
			articleWorkService.updateStatus(
			        work.getWorkId(),
			        2,
			        sw.toString(),
			        work.getUserId());
		}
	}

	/**
	 * 記事のFront MatterへサムネイルURLを設定する。
	 *
	 * 既存のimageがあれば置き換え、
	 * 存在しなければFront Matterへ追加する。
	 */
	private String applyThumbnailToFrontMatter(
			String content,
			String thumbnailUrl) {

		if (content == null
				|| thumbnailUrl == null
				|| thumbnailUrl.isBlank()) {
			return content;
		}

		final String escapedThumbnailUrl = thumbnailUrl
				.replace("\\", "\\\\")
				.replace("\"", "\\\"");

		final String imageLine = "image = \"" + escapedThumbnailUrl + "\"";

		final String frontMatterEnd = "\n+++\n";
		final int endIndex = content.indexOf(frontMatterEnd);

		if (endIndex == -1) {
			return content;
		}

		String frontMatter = content.substring(0, endIndex);
		String[] lines = frontMatter.split("\n", -1);
		StringBuilder updatedFrontMatter = new StringBuilder();
		boolean replaced = false;

		for (String line : lines) {
			if (line.trim().startsWith("image =")) {
				updatedFrontMatter.append(imageLine);
				replaced = true;
			} else {
				updatedFrontMatter.append(line);
			}

			updatedFrontMatter.append('\n');
		}

		if (!replaced) {
			updatedFrontMatter
					.append(imageLine)
					.append('\n');
		}

		return updatedFrontMatter.toString()
				+ content.substring(endIndex);
	}
}
