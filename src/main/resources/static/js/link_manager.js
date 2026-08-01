// =====================================================
// link_manager.js
//
// 記事リンク挿入管理
//
// ・投稿済み記事一覧表示
// ・Markdownリンク生成
// ・本文へのリンク挿入
// =====================================================


// -----------------------------------------------------
// 記事リンク挿入位置
// -----------------------------------------------------
let articleLinkInsertPosition = null;


// -----------------------------------------------------
// 記事リンクモーダル表示
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
// 投稿済み記事一覧を表示
//
// Thymeleafで渡された記事一覧をJSへ展開
// -----------------------------------------------------
function loadArticleLinkList() {

	const list =
		document.getElementById('articleLinkList');

	if (!list) {
		return;
	}


	list.innerHTML = '';


	const articles =
		window.publishedArticles || [];


	articles.forEach(article => {

		const button =
			document.createElement('button');


		button.type = 'button';

		button.textContent =
			article.title;


		button.dataset.slug =
			article.slug;

		button.dataset.title =
			article.title;


		button.addEventListener('click', function() {

			document
				.querySelectorAll('#articleLinkList button')
				.forEach(btn => btn.classList.remove('selected'));


			this.classList.add('selected');


			document.getElementById('articleLinkText')
				.value =
				this.dataset.title;


			document.getElementById('articleLinkText')
				.dataset.slug =
				this.dataset.slug;

		});


		list.appendChild(button);

	});

}


// -----------------------------------------------------
// キャンセル
// -----------------------------------------------------
const cancelButton =
	document.getElementById('articleLinkCancelButton');


if (cancelButton) {

	cancelButton.addEventListener('click', function() {

		document.getElementById('articleLinkModal')
			.style.display = 'none';


		articleLinkInsertPosition = null;

	});

}


// -----------------------------------------------------
// リンク挿入
// -----------------------------------------------------
const insertButton =
	document.getElementById('articleLinkInsertButton');


if (insertButton) {

	insertButton.addEventListener('click', function() {

		const textInput =
			document.getElementById('articleLinkText');


		const slug =
			textInput.dataset.slug;


		if (!slug) {

			alert('記事を選択してください');

			return;

		}


		const text =
			textInput.value.trim();


		const markdown =
			'[' +
			text +
			'](/' +
			slug +
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

	});

}