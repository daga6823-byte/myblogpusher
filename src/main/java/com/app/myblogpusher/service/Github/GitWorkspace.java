/**
 * clone/pull済みのGitリポジトリ作業領域を表すクラス
 *
 * Gitオブジェクト、ローカルパス、アクセストークンをまとめて保持し、
 * 認証付きpushとクローズ処理を提供する。
 */

package com.app.myblogpusher.service.Github;

import java.util.concurrent.locks.Lock;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
public final class GitWorkspace implements AutoCloseable {

	private final Git git;
	private final String repoPath;
	private final String accessToken;
	private final Lock lock;
	private boolean closed;

	public GitWorkspace(
	        Git git,
	        String repoPath,
	        String accessToken,
	        Lock lock) {

	    this.git = git;
	    this.repoPath = repoPath;
	    this.accessToken = accessToken;
	    this.lock = lock;
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
	            .setRemote("origin")
	            .add("refs/heads/main:refs/heads/main")
	            .call();
	}

	/**
	 * Gitリポジトリを閉じ、作業領域の排他ロックを解放する。
	 */
	@Override
	public void close() {

	    if (closed) {
	        return;
	    }

	    closed = true;

	    try {
	        git.close();
	    } finally {
	        lock.unlock();
	    }
	}
}