/**
 * 記事本文内の画像パス変換を担当するユーティリティ
 *
 * Markdown内のローカル画像パスを
 * Supabase Storageの公開URLへ変換する。
 */

package com.app.myblogpusher.util;

import org.springframework.stereotype.Component;

@Component
public class ArticleImageUtil {

	/**
	 * Markdown画像パスをStorage URLへ変換する
	 */
	public String convertImageUrl(
			String content,
			String storageBaseUrl) {

		if (content == null || content.isBlank()) {
			return content;
		}

		if (storageBaseUrl == null || storageBaseUrl.isBlank()) {
			return content;
		}

		return content.replaceAll(
				"!\\[([^]]*)\\]\\(/images/([^/]+)/([^)]+)\\)",
				"![](" + storageBaseUrl + "/$2/$3)");

	}
}