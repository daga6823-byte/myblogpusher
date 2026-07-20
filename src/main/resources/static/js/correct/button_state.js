/**
 * 保存ボタン活性制御
 *
 * ・カテゴリーと本文の入力状態を監視する
 * ・保存可能な状態になった時のみ保存ボタンを有効化する
 */

/**
 * 保存ボタンの活性状態を更新する
 */
function updateButtonState() {

	const content =
		document.getElementById('content').value.trim();

	const categorySelect =
		document.getElementById('categorySelect').value;

	const newCategoryName =
		document.getElementById('newCategoryName').value.trim();

	// カテゴリーが選択済みか
	const hasCategory =
		categorySelect !== '' &&
		(
			categorySelect !== '__new__' ||
			newCategoryName !== ''
		);

	// 本文が入力済みか
	const hasContent =
		content !== '';

	const enabled =
		hasCategory &&
		hasContent;

	document.querySelector('[formaction="/article/save"]').disabled =
		!enabled;

}

/**
 * 入力内容が変わるたびに保存ボタン状態を更新する
 */
[
	'categorySelect',
	'newCategoryName',
	'content'
].forEach(id => {

	const el =
		document.getElementById(id);

	if (!el) {
		return;
	}

	el.addEventListener(
		'input',
		updateButtonState
	);

	el.addEventListener(
		'change',
		updateButtonState
	);

});

/**
 * 初期表示時に保存ボタン状態を反映する
 */
window.addEventListener(
	'DOMContentLoaded',
	updateButtonState
);
