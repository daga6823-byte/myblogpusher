/**
 * 投稿記事のステータス
 */

package com.app.myblogpusher.enums;

public enum ArticleStatus {

	DRAFT, // 下書き
	PUBLISHING, // 投稿中
	PUBLISHED, // 投稿済み
	FAILED // 投稿失敗

}