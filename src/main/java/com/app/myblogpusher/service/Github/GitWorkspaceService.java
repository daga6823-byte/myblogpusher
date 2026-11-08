/**
 * Gitリポジトリの作業領域を準備するサービス
 *
 * アクセストークンの復号、一時ディレクトリの決定、
 * clone/pullによるリポジトリの最新化を担当する。
 */

package com.app.myblogpusher.service.Github;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.service.TokenCipherService;

@Service
public class GitWorkspaceService {

	private final TokenCipherService tokenCipherService;

	public GitWorkspaceService(
			TokenCipherService tokenCipherService) {

		this.tokenCipherService = tokenCipherService;
	}

	/**
	 * リポジトリを最新状態にした作業領域を開く。
	 * 呼び出し側はtry-with-resourcesで閉じること。
	 */
	public GitWorkspace open(
			UserRepositoryEntity repoEntity,
			String cipherKey)
			throws IOException, GitAPIException {

		String accessToken = tokenCipherService.decrypt(
				repoEntity.getAccessToken(),
				repoEntity.getTokenIv(),
				cipherKey);

		String repoPath = System.getProperty("java.io.tmpdir")
				+ "/myblogpusher_"
				+ repoEntity.getRepoId();

		File repoDir = new File(repoPath);

		if (!repoDir.exists() && !repoDir.mkdirs()) {
			throw new IOException(
					"Git作業ディレクトリを作成できません: " + repoPath);
		}

		if (!repoDir.isDirectory() || !repoDir.canWrite()) {
			throw new IOException(
					"Git作業ディレクトリに書き込めません: " + repoPath);
		}

		Git git = initializeRepository(
				repoDir,
				repoEntity,
				accessToken);

		return new GitWorkspace(git, repoPath, accessToken);
	}

	private Git initializeRepository(
			File repoDir,
			UserRepositoryEntity repoEntity,
			String accessToken)
			throws IOException, GitAPIException {

		File gitDir = new File(repoDir, ".git");

		if (gitDir.exists()) {
			if (!gitDir.isDirectory()) {
				throw new IOException(
						"Git管理ディレクトリがディレクトリではありません: "
								+ gitDir.getAbsolutePath());
			}

			try {
				Repository repository = new FileRepositoryBuilder()
						.setGitDir(gitDir)
						.build();

				Git git = new Git(repository);

				UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(
						"git",
						accessToken);

				// リモートのmainブランチを取得する。
				git.fetch()
						.setCredentialsProvider(credentialsProvider)
						.call();

				// ローカルにmainブランチがなければ作成する。
				if (repository.findRef("refs/heads/main") == null) {
					git.checkout()
							.setName("main")
							.setCreateBranch(true)
							.setStartPoint("origin/main")
							.setUpstreamMode(
									org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode.TRACK)
							.call();
				} else {
					git.checkout()
							.setName("main")
							.call();
				}

				// mainブランチを最新化する。
				git.pull()
						.setRemoteBranchName("main")
						.setCredentialsProvider(credentialsProvider)
						.call();

				return git;

			} catch (IOException | GitAPIException e) {

				Throwable rootCause = e;

				while (rootCause.getCause() != null
						&& rootCause.getCause() != rootCause) {
					rootCause = rootCause.getCause();
				}

				String rootCauseMessage = rootCause.getClass().getSimpleName()
						+ ": "
						+ rootCause.getMessage();

				throw new IOException(
						"既存のGit作業領域を開くか最新化できませんでした: "
								+ gitDir.getAbsolutePath()
								+ " / 発生例外: "
								+ e.getClass().getSimpleName()
								+ ": "
								+ e.getMessage()
								+ " / 根本原因: "
								+ rootCauseMessage,
						e);
			}
		}

		String remoteUrl = String.format(
				"https://github.com/%s/%s.git",
				repoEntity.getRepoOwner(),
				repoEntity.getRepoName());

		return Git.cloneRepository()
				.setURI(remoteUrl)
				.setDirectory(repoDir)
				.setCredentialsProvider(
						new UsernamePasswordCredentialsProvider(
								"git",
								accessToken))
				.call();
	}
}