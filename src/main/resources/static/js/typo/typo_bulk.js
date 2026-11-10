// ============================================================
// 一括選択・一括操作
//
// 全選択チェックボックス、選択IDの取得、
// 一括カテゴリー変更、一括削除を担当する。
// 一括操作は、選択IDをhidden inputとして非表示フォームに詰めて送信する。
// ============================================================


// ============================================================
// 共通関数
// ============================================================

// 現在チェックされている誤字パターンのIDを配列で返す
function getSelectedTypoIds() {

	// チェックされているチェックボックスだけを取得する
	return Array.from(
		document.querySelectorAll('.bulk-typo-checkbox:checked')
	)

		// 各チェックボックスのvalue属性から誤字パターンIDを取得する
		.map(cb => cb.value);
}


// フォームにhidden inputを1つ追加する
function appendHiddenInput(form, name, value) {

	// hidden形式のinput要素を新しく作成する
	const input = document.createElement('input');

	// inputの種類をhiddenにする
	// 画面には表示されない
	input.type = 'hidden';

	// サーバーが受け取るパラメータ名を設定する
	input.name = name;

	// 送信する値を設定する
	input.value = value;

	// 作成したhidden inputをフォームに追加する
	form.appendChild(input);
}


// 選択された誤字パターンIDを、name="typoIds"のhidden inputとしてフォームに詰める
function fillTypoIds(form, selectedIds) {

	// フォーム内に以前の送信データが残っている可能性があるため、
	// 一度すべての子要素を削除する
	form.innerHTML = '';

	// サーバー側で複数のIDを受け取れるよう、
	// すべてのinputに同じname="typoIds"を設定する
	selectedIds.forEach(id => appendHiddenInput(form, 'typoIds', id));
}


// ============================================================
// 一括選択機能
// ============================================================

function initSelectAll() {

	// 一覧の全項目を選択・解除するチェックボックスを取得する
	const selectAllTypoCheckbox =
		document.getElementById('selectAllTypoCheckbox');


	// 全選択チェックボックスの状態が変更されたときに実行する
	selectAllTypoCheckbox?.addEventListener('change', () => {

		// 各誤字パターンのチェックボックスをすべて取得する
		document.querySelectorAll('.bulk-typo-checkbox').forEach(cb => {

			// 全選択チェックボックスと同じ状態にする
			// 全選択がON → すべてON
			// 全選択がOFF → すべてOFF
			cb.checked = selectAllTypoCheckbox.checked;
		});
	});
}


// ============================================================
// 一括カテゴリー変更機能
// ============================================================

function initBulkCategory() {

	// 「カテゴリーを変更」ボタンがクリックされたときの処理
	document.getElementById('bulkCategoryButton')?.addEventListener('click', () => {

		// 現在選択されている誤字パターンのIDを取得する
		const selectedIds = getSelectedTypoIds();


		// 1件も選択されていない場合
		if (selectedIds.length === 0) {

			// エラーメッセージを表示する
			alert('変更する項目が選択されていません。');

			// 処理を中止する
			return;
		}


		// カテゴリー選択コンボボックスからカテゴリーIDを取得する
		const categoryId =
			document.getElementById('bulkCategorySelect').value;


		// 選択されているカテゴリーの表示名を取得する
		// 確認ダイアログに表示するために使用する
		const categoryLabel =
			document.getElementById('bulkCategorySelect')
				.selectedOptions[0].textContent;


		// 変更対象の件数と変更先カテゴリーを表示して確認する
		const ok = confirm(
			`選択した${selectedIds.length}件のカテゴリーを「${categoryLabel}」に変更してもよろしいですか？`
		);


		// キャンセルされた場合は処理を中止する
		if (!ok) return;


		// 一括カテゴリー変更用の非表示フォームを取得する
		const form = document.getElementById('bulkCategoryForm');


		// 選択された誤字パターンIDをフォームに詰める
		fillTypoIds(form, selectedIds);


		// カテゴリーIDが指定されている場合
		// 空欄の場合は「汎用（全カテゴリー共通）」として扱う
		if (categoryId) {

			// サーバー側でカテゴリーIDとして受け取るhidden inputを追加する
			appendHiddenInput(form, 'categoryId', categoryId);
		}


		// hidden inputを追加したフォームをサーバーに送信する
		form.submit();
	});
}


// ============================================================
// 一括削除機能
// ============================================================

function initBulkDelete() {

	// 「選択項目を削除」ボタンがクリックされたときの処理
	document.getElementById('bulkDeleteButton')?.addEventListener('click', () => {

		// 現在選択されている誤字パターンのIDを取得する
		const selectedIds = getSelectedTypoIds();


		// 1件も選択されていない場合
		if (selectedIds.length === 0) {

			// エラーメッセージを表示する
			alert('削除する項目が選択されていません。');

			// 処理を中止する
			return;
		}


		// 削除対象の件数を表示して確認する
		const ok = confirm(
			`選択した${selectedIds.length}件を削除してもよろしいですか？`
		);


		// キャンセルされた場合は処理を中止する
		if (!ok) return;


		// 一括削除用の非表示フォームを取得する
		const form = document.getElementById('bulkDeleteForm');


		// 選択された誤字パターンIDをフォームに詰める
		fillTypoIds(form, selectedIds);


		// hidden inputを追加したフォームをサーバーに送信する
		form.submit();
	});
}


// 一括選択・一括カテゴリー変更・一括削除をまとめて初期化する
export function initTypoBulk() {
	initSelectAll();
	initBulkCategory();
	initBulkDelete();
}