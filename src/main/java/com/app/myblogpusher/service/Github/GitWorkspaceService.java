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

		if (!repoDir.exists()) {
			repoDir.mkdirs();
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

			Repository repository = new FileRepositoryBuilder()
					.setGitDir(gitDir)
					.build();

			Git git = new Git(repository);

			git.pull()
					.setCredentialsProvider(
							new UsernamePasswordCredentialsProvider(
									"git",
									accessToken))
					.call();

			return git;

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