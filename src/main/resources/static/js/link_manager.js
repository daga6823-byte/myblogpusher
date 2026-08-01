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
function loadArticleLinkList() {

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


	window.publishedArticles.forEach(article => {

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


		});


		list.appendChild(button);

	});

}


// -----------------------------------------------------
// 記事リンクキャンセル
// -----------------------------------------------------
const cancelArticleLinkButton =
	document.getElementById('articleLinkCancelButton');


if (cancelArticleLinkButton) {

	cancelArticleLinkButton.addEventListener('click', function() {

		document.getElementById('articleLinkModal')
			.style.display = 'none';


		articleLinkInsertPosition = null;

		selectedArticleLink = null;

	});

}


// -----------------------------------------------------
// リンク挿入
// -----------------------------------------------------
const insertArticleLinkButton =
	document.getElementById('articleLinkInsertButton');


if (insertArticleLinkButton) {

	insertArticleLinkButton.addEventListener('click', function() {


		if (!selectedArticleLink) {

			alert('記事を選択してください');

			return;

		}


		const text =
			document.getElementById('articleLinkText')
				.value.trim();


		const markdown =
			'[' +
			(text || selectedArticleLink.title) +
			'](' +
			window.siteUrl +
			'/' +
			selectedArticleLink.hugoPath +
			'/)';


		const textarea =
			document.querySelector('textarea[name="content"]');


		if (articleLinkInsertPosition !== null) {

			textarea.value =
				textarea.value.substring(
					0,
					articleLinkInsertPosition)
				+
				markdown
				+
				textarea.value.substring(
					articleLinkInsertPosition);

		} else {

			textarea.value += markdown;

		}


		textarea.focus();


		document.getElementById('articleLinkModal')
			.style.display = 'none';


		articleLinkInsertPosition = null;

		selectedArticleLink = null;

	});

}