/**
 * カテゴリーインデックス編集画面の参考文献・脚注機能
 *
 * 参考文献の取得・絞り込み・登録と、
 * 共通の脚注処理を利用した本文への挿入を担当する。
 */

/**
 * 脚注ボタンから参考文献一覧を開く。
 */
const footnoteButton = document.getElementById('footnoteButton');

if (footnoteButton) {
	footnoteButton.addEventListener('click', async () => {
		const textarea = document.getElementById('content');
		const selector = document.getElementById('referenceSelector');

		if (!textarea || !selector) {
			return;
		}

		await window.ReferenceManager.loadCategories();

		if (!await window.ReferenceManager.loadReferences(
			window.categoryGroupId,
			true
		)) {
			return;
		}

		selector.style.display = 'block';
	});
}

/**
 * カテゴリー選択時は、取得済みデータを絞り込む。
 */
const referenceCategorySelect =
	document.getElementById('referenceCategorySelect');

if (referenceCategorySelect) {
	referenceCategorySelect.addEventListener('change', () => {

		window.ReferenceManager.filterReferences(
			referenceCategorySelect.value,
			true
		);

	});
}

/**
 * 参考文献を登録する。
 */
const saveReferenceButton =
	document.getElementById('saveReferenceButton');

if (saveReferenceButton) {
	saveReferenceButton.addEventListener('click', async () => {

		await window.ReferenceManager.saveReference({
			parameterName: 'categoryPath',
			parameterValue: window.categoryPath,
			groupId: window.categoryGroupId
		});

	});
}

/**
 * 選択した参考文献を脚注として本文に挿入する。
 */
const insertReferenceButton =
	document.getElementById('insertReferenceButton');

if (insertReferenceButton) {
	insertReferenceButton.addEventListener('click', () => {
		const checked = document.querySelector(
			'input[name="referenceCheck"]:checked'
		);

		const referenceName =
			document.getElementById('referenceName').value.trim();

		if (!checked && !referenceName) {
			return;
		}

		const textarea = document.getElementById('content');

		const reference = referenceName
			? { referenceName: referenceName }
			: JSON.parse(checked.value);

		if (!window.FootnoteManager) {
			console.error('FootnoteManager が読み込まれていません');
			alert('脚注機能の読み込みに失敗しました');
			return;
		}

		window.FootnoteManager.insert(textarea, reference);

		document.getElementById('referenceSelector').style.display = 'none';
	});
}

/**
 * 参考文献一覧を閉じる。
 */
const cancelReferenceButton =
	document.getElementById('cancelReferenceButton');

if (cancelReferenceButton) {
	cancelReferenceButton.addEventListener('click', () => {
		document.getElementById('referenceSelector').style.display = 'none';
	});
}
