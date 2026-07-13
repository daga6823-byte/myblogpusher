// =====================================================
// navigation.js
//
// 画面遷移・カテゴリー入力・保存ボタン制御を担当
// ・カテゴリー新規入力切替
// ・編集有無の管理
// ・一覧・ホームへの遷移
// ・保存ボタン活性制御
// =====================================================

// -----------------------------------------------------
// 新規カテゴリー入力欄の表示切替
// -----------------------------------------------------
function toggleNewCategory() {
	const select = document.getElementById('categorySelect');
	const newInput = document.getElementById('newCategoryName');

	if (select.value === '__new__') {
		newInput.style.display = 'block';
		newInput.focus();
	} else {
		newInput.style.display = 'none';
		newInput.value = '';
	}

	updateButtonState();
}

// -----------------------------------------------------
// 編集有無フラグ
// 保存確認ダイアログ表示に使用
// -----------------------------------------------------
let contentChanged = false;

// 本文編集を監視
const content = document.getElementById('content');
if (content) {
	content.addEventListener('input', () => {
		contentChanged = true;
	});
}

// -----------------------------------------------------
// 保存ボタン活性制御
// カテゴリー・本文が入力されている場合のみ有効
// -----------------------------------------------------
function updateButtonState() {
	const content = document.getElementById('content').value.trim();
	const categorySelect = document.getElementById('categorySelect').value;
	const newCategoryName = document.getElementById('newCategoryName').value.trim();

	const hasCategory =
		categorySelect !== '' &&
		(categorySelect !== '__new__' || newCategoryName !== '');

	const hasContent = content !== '';

	const enabled = hasCategory && hasContent;

	document.querySelector('[formaction="/article/save"]').disabled = !enabled;
	document.querySelector('[formaction="/article/correct"]').disabled = !enabled;
}

// -----------------------------------------------------
// 入力内容変更時に保存ボタン状態を更新
// -----------------------------------------------------
['categorySelect', 'newCategoryName', 'content'].forEach(id => {
	const el = document.getElementById(id);

	if (el) {
		el.addEventListener('input', updateButtonState);
		el.addEventListener('change', updateButtonState);
	}
});

// -----------------------------------------------------
// 一覧画面へ戻る
// 未保存の場合は保存確認を表示
// -----------------------------------------------------
document.getElementById('listButton').addEventListener('click', () => {

	if (contentChanged) {

		const wantSave = confirm('保存していない変更があります。保存しますか？');

		if (wantSave) {
			document.getElementById('redirectTo').value = 'list';

			const form = document.querySelector('form');
			form.action = '/article/save';
			form.submit();

		} else {
			window.location.href = '/article/list';
		}

	} else {
		window.location.href = '/article/list';
	}
});

// -----------------------------------------------------
// ホームへ戻る
// 未保存の場合は保存確認を表示
// -----------------------------------------------------
document.getElementById('backButton').addEventListener('click', () => {

	if (contentChanged) {

		const wantSave = confirm('保存していない変更があります。保存しますか？');

		if (wantSave) {
			document.getElementById('redirectTo').value = 'home';

			const form = document.querySelector('form');
			form.action = '/article/save';
			form.submit();

		} else {
			window.location.href = '/home';
		}

	} else {
		window.location.href = '/home';
	}
});