// =====================================================
// link_manager.js
//
// 記事リンク挿入管理
//
// ・投稿済み記事一覧表示
// ・選択記事保持
// ・Markdownリンク生成
// ・本文へのリンク挿入
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
// リンクメニュー表示
// -----------------------------------------------------

const linkButton =
	document.getElementById('linkButton');


if (linkButton) {

	linkButton.addEventListener('click', function() {

		const textarea =
			document.querySelector('textarea[name="content"]');

		textarea.focus();

		articleLinkInsertPosition =
			textarea.selectionStart;


		loadArticleLinkCategory();

		filteredArticleLinks =
			window.publishedArticles || [];

		loadArticleLinkList();

		document.getElementById('articleLinkModal')
			.style.display = 'block';

	});

}


// -----------------------------------------------------
// 投稿済み記事一覧表示
//
// window.publishedArticles
// から取得する
// -----------------------------------------------------

function loadArticleLinkList(articles) {

	filteredArticleLinks =
		articles || window.publishedArticles || [];

	const list =
		document.getElementById('articleLinkList');


	if (!list) {

		return;

	}


	list.innerHTML = '';


	if (!window.publishedArticles
		|| window.publishedArticles.length === 0) {

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

			selectedArticleLink = article;


			document.getElementById('articleLinkText')
				.value =
				article.title;


			document.getElementById('articleLinkUrl')
				.value =
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

	if (!select || !window.linkCategories) {

		return;

	}


	select.innerHTML = '';


	window.linkCategories.forEach(category => {

		const option =
			document.createElement('option');


		option.value =
			category.categoryId;


		option.textContent =
			category.fullPath;


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

			const categoryId =
				this.value;


			if (!categoryId) {

				loadArticleLinkList([]);

				return;

			}


			try {

				// カテゴリー変更時にArticleテーブルから
				// 対象カテゴリーの記事を再取得する
				const response =
					await fetch(
						'/article/link/articles?categoryId='
						+ encodeURIComponent(categoryId)
					);


				if (!response.ok) {

					throw new Error(
						'記事リンク一覧の取得に失敗しました'
					);

				}


				const articles =
					await response.json();


				// 再取得した記事一覧を保持する
				window.publishedArticles =
					articles;


				// 再取得した記事一覧を表示する
				loadArticleLinkList(
					articles
				);


			} catch (error) {

				console.error(
					'記事リンク一覧の取得に失敗しました:',
					error
				);

				loadArticleLinkList([]);

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


			articleLinkInsertPosition = null;

			selectedArticleLink = null;

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


			if (articleLinkInsertPosition !== null) {

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


			articleLinkInsertPosition = null;

			selectedArticleLink = null;

		}
	);

}