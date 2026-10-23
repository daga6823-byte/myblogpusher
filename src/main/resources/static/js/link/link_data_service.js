
/**
 * link_data_service.js
 *
 * 記事リンク挿入機能のデータ取得を担当するサービス。
 *
 * Articleテーブル由来の投稿済み記事一覧と、
 * リンク検索用カテゴリー一覧の取得およびキャッシュを担当する。
 * 一覧表示や本文へのリンク挿入は担当しない。
 */
export class LinkDataService {

	constructor() {
		this.articleCache = null;
		this.categoryCache = null;
	}

	/**
	 * 投稿済み記事一覧を取得する。
	 *
	 * 初回のみAPIから取得し、2回目以降はキャッシュを返す。
	 *
	 * @returns {Promise<Array>} 投稿済み記事一覧
	 */
	async fetchArticles() {

		if (this.articleCache !== null) {
			return this.articleCache;
		}

		try {

			const response =
				await fetch('/article/link/articles');

			if (!response.ok) {
				throw new Error(
					'記事リンク一覧の取得に失敗しました'
				);
			}

			this.articleCache =
				await response.json();

			return this.articleCache;

		} catch (error) {

			console.error(
				'記事リンク一覧の取得に失敗しました:',
				error
			);

			throw error;
		}
	}

	/**
	 * リンク検索用カテゴリー一覧を取得する。
	 *
	 * 初回のみAPIから取得し、2回目以降はキャッシュを返す。
	 *
	 * @returns {Promise<Array>} カテゴリー一覧
	 */
	async fetchCategories() {

		if (this.categoryCache !== null) {
			return this.categoryCache;
		}

		try {

			const response =
				await fetch('/article/link/categories');

			if (!response.ok) {
				throw new Error(
					'リンク用カテゴリー一覧の取得に失敗しました。'
				);
			}

			this.categoryCache =
				await response.json();

			return this.categoryCache;

		} catch (error) {

			console.error(
				'リンク検索カテゴリーの取得に失敗しました:',
				error
			);

			throw error;
		}
	}
}
