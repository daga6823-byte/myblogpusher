
/**
 * link_modal_manager.js
 *
 * 記事リンク挿入機能のモーダル管理を担当するクラス。
 *
 * モーダルの表示・キャンセル、カテゴリー選択、
 * リンク挿入ボタンのイベント登録を担当する。
 * データ取得、記事一覧の描画、本文への挿入は各担当クラスに委譲する。
 */
export class LinkModalManager {

	constructor(
		linkDataService,
		linkListManager,
		linkInsertService
	) {
		this.linkDataService = linkDataService;
		this.linkListManager = linkListManager;
		this.linkInsertService = linkInsertService;

		this.insertPosition = null;
	}

	/**
	 * モーダル関連のイベントを登録する。
	 */
	initialize() {

		const linkButton =
			document.getElementById('linkButton');

		if (linkButton) {
			linkButton.addEventListener('click', () => {
				this.openModal();
			});
		}

		const cancelButton =
			document.getElementById(
				'articleLinkCancelButton'
			);

		if (cancelButton) {
			cancelButton.addEventListener('click', () => {
				this.closeModal();
			});
		}

		const insertButton =
			document.getElementById(
				'articleLinkInsertButton'
			);

		if (insertButton) {
			insertButton.addEventListener('click', () => {
				this.insertLink();
			});
		}

		const categorySelect =
			document.getElementById(
				'articleLinkCategorySelect'
			);

		if (categorySelect) {
			categorySelect.addEventListener('change', () => {
				this.linkListManager
					.filterByCategory(categorySelect.value)
					.catch(error => {
						console.error(
							'記事リンク一覧の表示に失敗しました:',
							error
						);
					});
			});
		}
	}

	/**
	 * モーダルを開き、記事一覧とカテゴリーを表示する。
	 */
	async openModal() {

		const textarea =
			document.querySelector(
				'textarea[name="content"]'
			);

		if (!textarea) {
			return;
		}

		textarea.focus();

		this.insertPosition =
			textarea.selectionStart;

		// カテゴリー取得に失敗しても記事一覧の表示を続行する。
		this.loadCategories().catch(error => {
			console.error(
				'リンク検索カテゴリーの取得に失敗しました:',
				error
			);
		});

		try {

			const articles =
				await this.linkDataService.fetchArticles();

			this.linkListManager.loadArticleList(articles);

			const modal =
				document.getElementById('articleLinkModal');

			if (modal) {
				modal.style.display = 'block';
			}

		} catch (error) {

			console.error(
				'記事リンク一覧の取得に失敗しました:',
				error
			);

			alert('記事リンク一覧を取得できませんでした。');
		}
	}

	/**
	 * カテゴリー選択肢を生成し、初期選択を設定する。
	 */
	async loadCategories() {

		const select =
			document.getElementById(
				'articleLinkCategorySelect'
			);

		if (!select) {
			return;
		}

		const currentCategoryGroupId =
			window.categoryGroupId;

		const categories =
			await this.linkDataService.fetchCategories();

		if (!select.dataset.categoriesLoaded) {

			select.innerHTML = '';

			const allOption =
				document.createElement('option');

			allOption.value = '';
			allOption.textContent = 'すべて';

			select.appendChild(allOption);

			categories
				.sort((a, b) =>
					a.categoryPath.localeCompare(b.categoryPath, 'ja')
				)
				.forEach(category => {

					const option =
						document.createElement('option');

					option.value = category.groupId;
					option.textContent = category.categoryPath;

					select.appendChild(option);
				});

			select.dataset.categoriesLoaded = 'true';
		}

		select.value = '';

		if (
			currentCategoryGroupId != null &&
			[...select.options].some(
				option =>
					option.value === String(currentCategoryGroupId)
			)
		) {
			select.value = String(currentCategoryGroupId);
		}
	}

	/**
	 * モーダルを閉じ、選択状態を解除する。
	 */
	closeModal() {

		const modal =
			document.getElementById('articleLinkModal');

		if (modal) {
			modal.style.display = 'none';
		}

		this.insertPosition = null;

		this.linkListManager.clearSelection();
	}

	/**
	 * 選択した記事のMarkdownリンクを本文へ挿入する。
	 */
	insertLink() {

		const article =
			this.linkListManager.getSelectedArticle();

		if (!article) {
			alert('記事を選択してください');
			return;
		}

		const textInput =
			document.getElementById('articleLinkText');

		const text =
			textInput ? textInput.value.trim() : '';

		const markdown =
			this.linkInsertService.createMarkdown(
				article,
				text
			);

		this.linkInsertService.insertIntoContent(
			markdown,
			this.insertPosition
		);

		this.closeModal();
	}
}
