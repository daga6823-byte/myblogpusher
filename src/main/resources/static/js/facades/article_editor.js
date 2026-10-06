// =====================================================
// article_editor.js
//
// 記事編集フォームの共通操作を担当するFacade
// ・記事編集フォームの取得
// ・タイトル、カテゴリー、本文など共通入力値の取得
// ・指定したURLへのフォーム送信
// =====================================================

const ArticleEditor = {

	/**
	 * 記事編集フォームを取得する。
	 *
	 * title、content、categorySelectなどの記事編集項目を
	 * 含むフォームを対象とする。
	 */
	getForm() {
		return document.querySelector('#title')?.closest('form') || null;
	},

	/**
	 * 現在のタイトルを取得する。
	 */
	getTitle() {
		return document.getElementById('title')?.value || '';
	},

	/**
	 * 現在の本文を取得する。
	 */
	getContent() {
		return document.getElementById('content')?.value || '';
	},

	/**
	 * 現在のカテゴリー選択値を取得する。
	 */
	getCategorySelect() {
		return document.getElementById('categorySelect')?.value || '';
	},

	/**
	 * 新規カテゴリー名を取得する。
	 */
	getNewCategoryName() {
		return document.getElementById('newCategoryName')?.value || '';
	},

	/**
	 * 現在のworkIdを取得する。
	 */
	getWorkId() {
		return document.querySelector('input[name="workId"]')?.value || '';
	},

	/**
	 * 指定したURLへ記事編集フォームを送信する。
	 */
	submitTo(action) {
		const form = this.getForm();

		if (!form) {
			console.error('記事編集フォームが見つかりません。');
			return;
		}

		form.action = action;
		form.submit();
	}
};
