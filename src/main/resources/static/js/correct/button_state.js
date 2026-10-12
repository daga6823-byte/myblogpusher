
/**
 * button_status.js
 * 保存ボタン活性制御
 *
 * カテゴリー階層の選択状態、新規カテゴリー名、本文の入力状態を監視し、
 * 保存可能な状態になった時のみ保存ボタンを有効化する。
 */

/**
 * 保存ボタンの活性状態を更新する。
 */
function updateButtonState() {

	const content =
		document.getElementById('content').value.trim();

	const categoryPath =
		document.getElementById('categorySelect')?.value.trim() || '';

	const newCategoryName =
		document.getElementById('newCategoryNameInput')?.value.trim() || '';

	const selectedCategory =
		Array.from(
			document.querySelectorAll('.category-level')
		).some(select => select.value !== '' && select.value !== '__new__');

	// 既存カテゴリーが選択済み、または親カテゴリーを選択して
	// 新規カテゴリー名が入力されていれば保存可能。
	const hasCategory =
		(selectedCategory && categoryPath !== '') ||
		(categoryPath !== '' && newCategoryName !== '');

	const hasContent = content !== '';

	const saveButton =
		document.querySelector('[formaction="/article/save"]');

	if (saveButton) {
		saveButton.disabled = !(hasCategory && hasContent);
	}
}

/**
 * 入力内容が変わるたびに保存ボタン状態を更新する。
 */
document.addEventListener('input', updateButtonState);
document.addEventListener('change', updateButtonState);

/**
 * 初期表示時に保存ボタン状態を反映する。
 */
window.addEventListener('DOMContentLoaded', updateButtonState);
