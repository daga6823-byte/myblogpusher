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
// カテゴリー指定で記事一覧を取得
// -----------------------------------------------------

async function fetchArticleLinkList(categoryGroupId) {

	if (!categoryGroupId) {

		loadArticleLinkList([]);

		return;

	}

	try {

		// Articleテーブルから
		// 指定カテゴリー経路の記事を取得する。
		const response =
			await fetch(
				'/article/link/articles?categoryGroupId='
				+ encodeURIComponent(categoryGroupId)
			);

		if (!response.ok) {

			throw new Error(
				'記事リンク一覧の取得に失敗しました'
			);

		}

		const articles =
			await response.json();

		// 取得した記事一覧を保持する。
		filteredArticleLinks = articles;

		loadArticleLinkList(articles);

	} catch (error) {

		console.error(
			'記事リンク一覧の取得に失敗しました:',
			error
		);

		loadArticleLinkList([]);

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

		// リンク検索カテゴリーを生成する。
		loadArticleLinkCategory();

		const categorySelect =
			document.getElementById(
				'articleLinkCategorySelect'
			);

		const categoryGroupId =
			categorySelect
				? categorySelect.value
				: null;

		// モーダルを開いた時点で
		// 現在選択されているカテゴリーの記事を取得する。
		await fetchArticleLinkList(
			categoryGroupId
		);

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

	filteredArticleLinks.forEach(article => {

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

function loadArticleLinkCategory() {

	const select =
		document.getElementById(
			'articleLinkCategorySelect'
		);

	if (
		!select ||
		!window.linkCategories
	) {

		return;

	}

	select.innerHTML = '';

	window.linkCategories.forEach(category => {

		const option =
			document.createElement('option');

		// APIにはカテゴリー経路を識別するgroupIdを送る。
		option.value =
			category.groupId;

		option.textContent =
			category.categoryPath;

		// 現在の記事に対応するカテゴリーは
		// categoryIdで判定する。
		if (
			String(category.categoryId)
			===
			String(window.linkSearchCategoryId)
		) {

			option.selected = true;

		}

		select.appendChild(option);

	});

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

			const categoryGroupId =
				this.value;

			await fetchArticleLinkList(
				categoryGroupId
			);

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