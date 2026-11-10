/**
 * 投稿確定時のスラッグ決定を担当するサービス
 *
 * 新規記事は形態素解析と英語辞書による変換結果から生成し、
 * 既存記事は保存済みのスラッグを維持する。
 */
package com.app.myblogpusher.service.Publish;

import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.Article.ArticleWork;
import com.app.myblogpusher.util.SlugUtil;

@Service
public class PublishSlugService {

	private final SlugUtil slugUtil;

	/**
	 * スラッグ決定に必要な依存関係を設定する。
	 */
	public PublishSlugService(SlugUtil slugUtil) {
		this.slugUtil = slugUtil;
	}

	/**
	 * 投稿するスラッグを決定する。
	 *
	 * @return 決定したスラッグ。生成できなかった場合はnullまたは空文字
	 */
	public String decideSlug(ArticleWork work, String title) {

		// 既存記事は保存済みのスラッグを維持する。
		if (work.getArticleId() != null) {
			return work.getSlug();
		}

		var analysis = slugUtil.analyzeSlug(title);
		return slugUtil.generateSlugFromAnalysis(analysis);
	}
}