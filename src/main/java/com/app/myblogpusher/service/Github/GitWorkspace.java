/**
 * clone/pull済みのGitリポジトリ作業領域を表すクラス
 *
 * Gitオブジェクト、ローカルパス、アクセストークンをまとめて保持し、
 * 認証付きpushとクローズ処理を提供する。
 */

package com.app.myblogpusher.service.Github;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public final class GitWorkspace implements AutoCloseable {

	private final Git git;
	private final String repoPath;
	private final String accessToken;

	public GitWorkspace(
			Git git,
			String repoPath,
			String accessToken) {

		this.git = git;
		this.repoPath = repoPath;
		this.accessToken = accessToken;
	}

	public Git getGit() {
		return git;
	}

	public String getRepoPath() {
		return repoPath;
	}

	public CredentialsProvider credentials() {
		return new UsernamePasswordCredentialsProvider(
				"git",
				accessToken);
	}

	/**
	 * 認証情報付きでリモートへpushする
	 */
	public void push() throws GitAPIException {
		git.push()
				.setCredentialsProvider(credentials())
				.call();
	}

	@Override
	public void close() {
		git.close();
	}
}