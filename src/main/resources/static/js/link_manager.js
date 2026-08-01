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


		document.getElementById('articleLinkModal')
			.style.display = 'block';

	});

}


// -----------------------------------------------------
// 記事リンクキャンセル
// -----------------------------------------------------
const cancelArticleLinkButton =
	document.getElementById('cancelArticleLinkButton');


if (cancelArticleLinkButton) {

	cancelArticleLinkButton.addEventListener('click', function() {

		document.getElementById('articleLinkModal')
			.style.display = 'none';

		articleLinkInsertPosition = null;

	});

}


// -----------------------------------------------------
// 記事選択時
//
// デフォルト表示文字列は記事タイトル
// 変更欄で任意文字列へ変更可能
// -----------------------------------------------------
const articleLinkSelect =
	document.getElementById('articleLinkSelect');


if (articleLinkSelect) {

	articleLinkSelect.addEventListener('change', function() {

		const selected =
			this.options[this.selectedIndex];


		const title =
			selected.dataset.title;


		document.getElementById('articleLinkText')
			.value = title || '';

	});

}


// -----------------------------------------------------
// リンク挿入
// -----------------------------------------------------
const insertArticleLinkButton =
	document.getElementById('insertArticleLinkButton');


if (insertArticleLinkButton) {

	insertArticleLinkButton.addEventListener('click', function() {

		const select =
			document.getElementById('articleLinkSelect');


		if (!select.value) {

			alert('記事を選択してください');

			return;

		}


		const selected =
			select.options[select.selectedIndex];


		const text =
			document.getElementById('articleLinkText')
				.value.trim();


		const slug =
			select.value;


		const markdown =
			'[' +
			(text || selected.dataset.title) +
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