/**
 * GitHubリポジトリ上のHugoインデックスファイル(_index.md)を読み書きするサービス
 *
 * 記事投稿とは独立して、カテゴリーの_index.mdなどを取得・更新する。
 * _index.md以外やcontentディレクトリ外へのアクセスは拒否する。
 */

package com.app.myblogpusher.service.Github;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.UserRepositoryEntity;

@Service
public class GitHubMarkdownFileService {

	private final GitWorkspaceService gitWorkspaceService;

	public GitHubMarkdownFileService(
			GitWorkspaceService gitWorkspaceService) {

		this.gitWorkspaceService = gitWorkspaceService;
	}

	/**
	 * GitHubリポジトリから既存のHugoインデックスファイルを読み込む。
	 *
	 * リポジトリを初期化して最新状態へ更新し、
	 * ファイルが存在しない場合はnullを返す。
	 *
	 * @param repoEntity 接続先リポジトリ情報
	 * @param cipherKey アクセストークンの復号キー
	 * @param relativePath contentディレクトリからの相対パス
	 * @return ファイル内容。存在しない場合はnull
	 */
	public String readMarkdownFile(
			UserRepositoryEntity repoEntity,
			String cipherKey,
			String relativePath)
			throws IOException, GitAPIException {

		Path filePath = resolveIndexPath(
				relativePath,
				"リポジトリ外のファイルは読み込めません。");

		try (GitWorkspace workspace = gitWorkspaceService.open(
				repoEntity,
				cipherKey)) {

			Path targetPath = Paths.get(workspace.getRepoPath()).resolve(filePath);

			if (!Files.exists(targetPath)) {
				return null;
			}

			return Files.readString(
					targetPath,
					StandardCharsets.UTF_8);
		}
	}

	/**
	 * HugoのMarkdownファイルを保存し、GitHubへプッシュする。
	 *
	 * 記事投稿とは独立して、カテゴリーの_index.mdなどを更新する。
	 * リポジトリの初期化・認証にはGitWorkspaceServiceを利用する。
	 *
	 * @param repoEntity 接続先リポジトリ情報
	 * @param cipherKey アクセストークンの復号キー
	 * @param relativePath contentディレクトリからの相対パス
	 * @param content 保存するMarkdown本文
	 * @param commitMessage コミットメッセージ
	 */
	public void pushMarkdownFile(
			UserRepositoryEntity repoEntity,
			String cipherKey,
			String relativePath,
			String content,
			String commitMessage)
			throws IOException, GitAPIException {

		// インデックスファイル以外への書き込みを防ぐ。
		Path filePath = resolveIndexPath(
				relativePath,
				"リポジトリ外への書き込みは許可されていません。");

		try (GitWorkspace workspace = gitWorkspaceService.open(
				repoEntity,
				cipherKey)) {

			Path targetPath = Paths
					.get(workspace.getRepoPath())
					.resolve(filePath);

			// 内容が変わっていなければ不要なコミット・プッシュを行わない。
			if (Files.exists(targetPath)
					&& Files.readString(
							targetPath,
							StandardCharsets.UTF_8)
							.equals(content)) {
				return;
			}

			Files.createDirectories(targetPath.getParent());
			Files.writeString(
					targetPath,
					content,
					StandardCharsets.UTF_8);

			workspace.getGit()
					.add()
					.addFilepattern(filePath.toString().replace('\\', '/'))
					.call();

			workspace.getGit()
					.commit()
					.setMessage(commitMessage)
					.setAuthor("Myblogpusher", "noreply@myblogpusher.local")
					.call();

			workspace.push();
		}
	}

	/**
	 * _index.mdの相対パスを検証し、contentディレクトリ配下のパスへ解決する。
	 *
	 * @param relativePath contentディレクトリからの相対パス
	 * @param outsideRepoMessage content配下から外れた場合のエラーメッセージ
	 */
	private Path resolveIndexPath(
			String relativePath,
			String outsideRepoMessage) {

		Path contentDir = Paths.get("content");
		Path relativeFile = Paths.get(relativePath).normalize();

		// _index.md以外やリポジトリ外へのアクセスを拒否する。
		if (relativeFile.isAbsolute()
				|| relativeFile.startsWith("..")
				|| relativeFile.getNameCount() < 2
				|| !"_index.md".equals(relativeFile.getFileName().toString())) {
			throw new IllegalArgumentException(
					"インデックスファイルのパスが不正です: " + relativePath);
		}

		Path filePath = contentDir.resolve(relativeFile).normalize();

		if (!filePath.startsWith(contentDir)) {
			throw new IllegalArgumentException(outsideRepoMessage);
		}

		return filePath;
	}

	/**
	 * 開いているGit作業領域から既存のHugoインデックスファイルを読み込む。
	 *
	 * リポジトリの初期化やpullは行わず、既存のパス検証を利用する。
	 */
	public String readMarkdownFile(
			GitWorkspace workspace,
			String relativePath) throws IOException {

		Path filePath = resolveIndexPath(
				relativePath,
				"リポジトリ外のファイルは読み込めません。");

		Path targetPath = Paths.get(workspace.getRepoPath())
				.resolve(filePath);

		if (!Files.exists(targetPath)) {
			return null;
		}

		return Files.readString(targetPath, StandardCharsets.UTF_8);
	}
}