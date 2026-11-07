/**
 * 参考文献・脚注挿入機能
 *
 * ・脚注入力ダイアログ表示
 * ・参考文献カテゴリー選択
 * ・登録済み参考文献の取得
 * ・参考文献の登録
 * ・カーソル位置へ [^n] を挿入
 * ・本文末尾へ [^n]: 脚注テキスト を追記
 *
 * 参考文献はカテゴリー経路単位で管理する。
 */

// =====================================================
// 参考文献カテゴリーを作成
//
// 参考文献が実際に登録されているカテゴリーだけを取得する。
// =====================================================
async function loadReferenceCategories() {

	const select =
		document.getElementById('referenceCategorySelect');

	if (!select) {
		return;
	}

	// 取得済みの場合は再通信しない。
	if (referenceCategoriesLoaded) {
		return;
	}

	select.innerHTML = '';

	const allOption =
		document.createElement('option');

	allOption.value = '';
	allOption.textContent = 'すべて';

	select.appendChild(allOption);

	const response =
		await fetch('/category/reference/categories');

	if (!response.ok) {
		alert('参考文献カテゴリーの取得に失敗しました');
		return;
	}

	const categories =
		await response.json();

	categories.forEach(category => {

		const option =
			document.createElement('option');

		option.value = category.categoryId;
		option.textContent = category.categoryName;

		select.appendChild(option);
	});

	// 選択肢の作成が完了してから取得済みにする。
	referenceCategoriesLoaded = true;
}

// =====================================================
// 参考文献一覧を表示
//
// API通信は行わず、取得済みのreferenceCacheから表示する。
// =====================================================

function displayReferences(references) {

	const list =
		document.getElementById('referenceList');

	list.innerHTML = '';

	references.forEach(reference => {

		const label =
			document.createElement('label');

		const checkbox =
			document.createElement('input');

		checkbox.type = 'checkbox';
		checkbox.name = 'referenceCheck';
		checkbox.value =
			JSON.stringify(reference);

		label.appendChild(checkbox);

		label.appendChild(
			document.createTextNode(
				reference.referenceName
			)
		);

		list.appendChild(label);

		list.appendChild(
			document.createElement('br')
		);
	});
}

// =====================================================
// 脚注ボタン
//
// ダイアログを開いたときに参考文献を一度だけ取得する。
// =====================================================

document.getElementById('footnoteButton')
	.addEventListener('click', async () => {

		const textarea =
			document.getElementById('content');

		if (!textarea) {

			return;

		}

		const selector =
			document.getElementById('referenceSelector');

		if (!selector) {
			return;
		}

		await window.ReferenceManager.loadCategories();

		const groupId =
			document.getElementById('categorySelect').value;

		if (!groupId || groupId === '__new__') {
			alert('記事のカテゴリーを選択してください');
			return;
		}

		if (!await window.ReferenceManager.loadReferences(groupId)) {
			return;
		}

		selector.style.display = 'block';
	});

// =====================================================
// カテゴリー変更
//
// API通信は行わず、取得済みデータを絞り込む。
// =====================================================

document.getElementById('referenceCategorySelect')
	.addEventListener('change', () => {

		const categoryId =
			document.getElementById('referenceCategorySelect')
				.value;

		window.ReferenceManager.filterReferences(categoryId);

	});

// =====================================================
// 参考文献登録
// =====================================================
document.getElementById('saveReferenceButton')
	.addEventListener('click', async () => {

		const categoryPath =
			document.getElementById('categorySelect').value;

		const groupId = window.categoryGroupId;

		if (!categoryPath || categoryPath === '__new__' || !groupId) {
			alert('記事のカテゴリーを選択してください');
			return;
		}

		await window.ReferenceManager.saveReference({
			parameterName: 'groupId',
			parameterValue: groupId,
			groupId: groupId,
			headers: {
				'X-Requested-With': 'XMLHttpRequest'
			}
		});

	});

// =====================================================
// 選択した参考文献を脚注として挿入
// =====================================================

document.getElementById('insertReferenceButton')
	.addEventListener('click', () => {

		const checked =
			document.querySelector(
				'input[name="referenceCheck"]:checked'
			);

		const referenceName =
			document.getElementById('referenceName')
				.value.trim();

		if (!referenceName && !checked) {
			return;
		}

		const reference = referenceName
			? { referenceName: referenceName }
			: JSON.parse(checked.value);

		window.FootnoteManager.insert(
			document.getElementById('content'),
			reference
		);

		document.getElementById('referenceSelector')
			.style.display = 'none';

	});

// =====================================================
// キャンセル
// =====================================================

document.getElementById('cancelReferenceButton')
	.addEventListener('click', () => {

		document.getElementById('referenceSelector')
			.style.display = 'none';
	});