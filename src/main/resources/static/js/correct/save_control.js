/**
 * 編集状態管理
 *
 * ・本文・タイトル・カテゴリー変更を監視する
 * ・未保存変更の有無を管理する
 */

/**
 * 未保存変更フラグ
 */
let contentChanged = false;

/**
 * 編集対象入力項目
 *
 * 入力または変更があれば未保存状態にする。
 */
[
	'categorySelect',
	'newCategoryName',
	'title',
	'content'
].forEach(id => {

	const el = document.getElementById(id);

	if (!el) {
		return;
	}

	el.addEventListener('input', () => {
		contentChanged = true;
	});

	el.addEventListener('change', () => {
		contentChanged = true;
	});

});
