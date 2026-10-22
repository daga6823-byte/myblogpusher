// =====================================================
// link_manager.js
//
// 記事リンク挿入管理
//
// ・投稿済み記事一覧表示
// ・カテゴリー変更による記事再取得
// ・選択記事保持
// ・Markdownリンク生成
// ・本文へのリンク挿入
//
// 記事一覧はArticleテーブルから取得する。
// =====================================================


// -----------------------------------------------------
// 記事リンク挿入位置
// -----------------------------------------------------

let articleLinkInsertPosition = null;


// -----------------------------------------------------
// 選択中記事
// -----------------------------------------------------

let selectedArticleLink = null;


// -----------------------------------------------------
// 表示中リンク一覧
// -----------------------------------------------------

let filteredArticleLinks = [];

// -----------------------------------------------------
// 記事一覧キャッシュ
// -----------------------------------------------------

let articleLinkCache = null;

// -----------------------------------------------------
// カテゴリー一覧の取得状態
// -----------------------------------------------------

let articleLinkCategoriesLoaded = false;


// -----------------------------------------------------
// 記事一覧の取得とキャッシュ
//
// 初回だけArticleテーブル由来の記事一覧を取得する。
// 2回目以降はキャッシュを使用する。
// カテゴリーによる絞り込みは呼び出し元で行う。
// -----------------------------------------------------

async function fetchArticleLinkList() {

	if (articleLinkCache !== null) {
		return articleLinkCache;
	}

	try {

		const response =
			await fetch('/article/link/articles');

		if (!response.ok) {
			throw new Error(
				'記事リンク一覧の取得に失敗しました'
			);
		}

		const articles =
			await response.json();

		articleLinkCache = articles;

		return articleLinkCache;

	} catch (error) {

		console.error(
			'記事リンク一覧の取得に失敗しました:',
			error
		);

		throw error;
	}
}


// -----------------------------------------------------
// リンクメニュー表示
// -----------------------------------------------------

const linkButton =
	document.getElementById('linkButton');


if (linkButton) {

	linkButton.addEventListener('click', async function() {

		const textarea =
			document.querySelector(
				'textarea[name="content"]'
			);

		if (!textarea) {

			return;

		}

		textarea.focus();

		articleLinkInsertPosition =
			textarea.selectionStart;

		// リンク検索カテゴリーを初回だけ取得する。
		try {
			await loadArticleLinkCategory();
		} catch (error) {
			console.error(
				'リンク検索カテゴリーの取得に失敗しました:',
				error
			);
			alert('リンク検索カテゴリーを取得できませんでした。');
			return;
		}

		const categorySelect =
			document.getElementById(
				'articleLinkCategorySelect'
			);

		const categoryGroupId =
			categorySelect
				? categorySelect.value
				: '';

		// 記事一覧は初回だけ取得し、以降はキャッシュを使う。
		try {
			const articles = await fetchArticleLinkList();

			const filteredArticles = categoryGroupId
				? articles.filter(
					article =>
						String(article.categoryGroupId)
						=== String(categoryGroupId)
				)
				: [...articles];

			loadArticleLinkList(filteredArticles);

		} catch (error) {
			alert('記事リンク一覧を取得できませんでした。');
			return;
		}

		document.getElementById(
			'articleLinkModal'
		).style.display = 'block';

	});

}


// -----------------------------------------------------
// 投稿済み記事一覧表示
// -----------------------------------------------------

function loadArticleLinkList(articles) {

	filteredArticleLinks =
		articles || [];

	const list =
		document.getElementById(
			'articleLinkList'
		);

	if (!list) {

		return;

	}

	list.innerHTML = '';

	if (
		!filteredArticleLinks ||
		filteredArticleLinks.length === 0
	) {

		list.textContent =
			'投稿済み記事がありません';

		return;

	}

	filteredArticleLinks
		.sort((a, b) =>
			a.title.localeCompare(
				b.title,
				'ja'
			)
		)
		.forEach(article => {

			const button =
				document.createElement('button');

			button.type = 'button';

			button.className =
				'article-link-item';

			button.textContent =
				article.title;

			button.addEventListener('click', function() {

				selectedArticleLink =
					article;

				document.getElementById(
					'articleLinkText'
				).value =
					article.title;

				document.getElementById(
					'articleLinkUrl'
				).value =
					article.hugoPath;

			});

			list.appendChild(button);

		});

}

// -----------------------------------------------------
// リンク検索カテゴリー生成
// -----------------------------------------------------
async function loadArticleLinkCategory() {
	const select =
		document.getElementById(
			'articleLinkCategorySelect'
		);

	if (!select) {
		return;
	}

	// 編集中の記事が保持しているgroupIdをそのまま使用する。
	const currentCategoryGroupId =
		window.categoryGroupId;

	// カテゴリー一覧はページ内でキャッシュして再利用する。
	if (!articleLinkCategoriesLoaded) {

		const response =
			await fetch('/article/link/categories');

		if (!response.ok) {
			throw new Error(
				'リンク用カテゴリー一覧の取得に失敗しました。'
			);
		}

		const categories = await response.json();

		select.innerHTML = '';

		// 全カテゴリーの記事を検索するための選択肢。
		const allOption =
			document.createElement('option');

		allOption.value = '';
		allOption.textContent = 'すべて';

		select.appendChild(allOption);

		categories
			.sort((a, b) =>
				a.categoryPath.localeCompare(
					b.categoryPath,
					'ja'
				)
			)
			.forEach(category => {

				const option =
					document.createElement('option');

				// リンク検索APIにはgroupIdを渡す。
				option.value =
					category.groupId;

				option.textContent =
					category.categoryPath;

				select.appendChild(option);
			});

		articleLinkCategoriesLoaded = true;
	}

	// 「すべて」をデフォルト選択する。
	select.value = '';

	// ワークのgroupIdが選択肢に存在する場合だけ選択する。
	if (
		currentCategoryGroupId != null &&
		[...select.options].some(
			option => option.value === String(currentCategoryGroupId)
		)
	) {
		select.value = String(currentCategoryGroupId);
	}
}


// -----------------------------------------------------
// カテゴリー変更時
// -----------------------------------------------------

const articleLinkCategorySelect =
	document.getElementById(
		'articleLinkCategorySelect'
	);


if (articleLinkCategorySelect) {

	articleLinkCategorySelect.addEventListener(
		'change',
		async function() {

			try {
				const articles = await fetchArticleLinkList();

				const categoryGroupId = this.value;

				const filteredArticles = categoryGroupId
					? articles.filter(
						article =>
							String(article.categoryGroupId)
							=== String(categoryGroupId)
					)
					: [...articles];

				loadArticleLinkList(filteredArticles);

			} catch (error) {
				console.error(
					'記事リンク一覧の表示に失敗しました:',
					error
				);
			}
		}
	);

}


// -----------------------------------------------------
// 記事リンクキャンセル
// -----------------------------------------------------

const cancelArticleLinkButton =
	document.getElementById(
		'articleLinkCancelButton'
	);


if (cancelArticleLinkButton) {

	cancelArticleLinkButton.addEventListener(
		'click',
		function() {

			document.getElementById(
				'articleLinkModal'
			).style.display = 'none';

			articleLinkInsertPosition =
				null;

			selectedArticleLink =
				null;

		}
	);

}


// -----------------------------------------------------
// リンク挿入
// -----------------------------------------------------

const insertArticleLinkButton =
	document.getElementById(
		'articleLinkInsertButton'
	);


if (insertArticleLinkButton) {

	insertArticleLinkButton.addEventListener(
		'click',
		function() {

			if (!selectedArticleLink) {

				alert('記事を選択してください');

				return;

			}

			const text =
				document.getElementById(
					'articleLinkText'
				).value.trim();

			const url =
				selectedArticleLink.hugoPath;

			const markdown =
				'[' +
				(text || selectedArticleLink.title) +
				'](' +
				url +
				')';

			const textarea =
				document.querySelector(
					'textarea[name="content"]'
				);

			if (
				articleLinkInsertPosition !== null
			) {

				textarea.value =
					textarea.value.substring(
						0,
						articleLinkInsertPosition
					)
					+
					markdown
					+
					textarea.value.substring(
						articleLinkInsertPosition
					);

			} else {

				textarea.value +=
					markdown;

			}

			textarea.dispatchEvent(
				new Event('input', { bubbles: true })
			);

			textarea.focus();

			// 挿入したリンクの直後へカーソルを戻す
			const cursorPosition =
				articleLinkInsertPosition !== null
					? articleLinkInsertPosition
					+ markdown.length
					: textarea.value.length;

			textarea.setSelectionRange(
				cursorPosition,
				cursorPosition
			);

			document.getElementById(
				'articleLinkModal'
			).style.display = 'none';

			articleLinkInsertPosition =
				null;

			selectedArticleLink =
				null;

		}
	);

}