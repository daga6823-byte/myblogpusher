
/**
 * link_list_manager.js
 *
 * 記事リンク挿入機能の記事一覧管理を担当するクラス。
 *
 * 投稿済み記事一覧の表示、カテゴリーによる絞り込み、
 * 選択中の記事の管理を担当する。
 * 記事データの取得はLinkDataServiceに委譲する。
 */
export class LinkListManager {

	constructor(linkDataService) {
		this.linkDataService = linkDataService;
		this.selectedArticle = null;
		this.filteredArticles = [];
	}

	/**
	 * 投稿済み記事一覧を表示する。
	 *
	 * @param {Array} articles 表示対象の記事一覧
	 */
	loadArticleList(articles) {

		this.filteredArticles = [...(articles || [])];

		const list =
			document.getElementById('articleLinkList');

		if (!list) {
			return;
		}

		list.innerHTML = '';

		if (this.filteredArticles.length === 0) {
			list.textContent = '投稿済み記事がありません';
			return;
		}

		this.filteredArticles
			.sort((a, b) =>
				a.title.localeCompare(b.title, 'ja')
			)
			.forEach(article => {

				const button =
					document.createElement('button');

				button.type = 'button';
				button.className = 'article-link-item';
				button.textContent = article.title;

				button.addEventListener('click', () => {
					this.selectArticle(article);
				});

				list.appendChild(button);
			});
	}

	/**
	 * 記事を選択し、リンク入力欄へ反映する。
	 *
	 * @param {Object} article 選択した記事
	 */
	selectArticle(article) {

		this.selectedArticle = article;

		const textInput =
			document.getElementById('articleLinkText');

		const urlInput =
			document.getElementById('articleLinkUrl');

		if (textInput) {
			textInput.value = article.title;
		}

		if (urlInput) {
			urlInput.value = article.hugoPath;
		}
	}

	/**
	 * カテゴリーを指定して記事一覧を絞り込む。
	 *
	 * @param {string} categoryGroupId カテゴリーのgroupId
	 */
	async filterByCategory(categoryGroupId) {

		const articles =
			await this.linkDataService.fetchArticles();

		const filteredArticles = categoryGroupId
			? articles.filter(article =>
				String(article.groupId) === String(categoryGroupId)
			)
			: [...articles];

		this.loadArticleList(filteredArticles);
	}

	/**
	 * 選択中の記事を取得する。
	 *
	 * @returns {Object|null} 選択中の記事
	 */
	getSelectedArticle() {
		return this.selectedArticle;
	}

	/**
	 * 選択中の記事を解除する。
	 */
	clearSelection() {
		this.selectedArticle = null;
	}
}
