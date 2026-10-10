/**
 * カテゴリーインデックス編集画面の参考文献・脚注機能
 *
 * 参考文献の取得・絞り込み・登録と、
 * 共通の脚注処理を利用した本文への挿入を担当する。
 */

let referenceCache = [];

/**
 * 参考文献カテゴリーの選択肢を取得する。
 */
async function loadReferenceCategories() {
	const select = document.getElementById('referenceCategorySelect');

	if (!select) {
		return;
	}

	select.innerHTML = '';

	const allOption = document.createElement('option');
	allOption.value = '';
	allOption.textContent = 'すべて';
	select.appendChild(allOption);

	const response = await fetch('/category/reference/categories');

	if (!response.ok) {
		alert('参考文献カテゴリーの取得に失敗しました');
		return;
	}

	const categories = await response.json();

	categories.forEach(category => {
		const option = document.createElement('option');
		option.value = category.categoryId;
		option.textContent = category.categoryName;
		select.appendChild(option);
	});
}

/**
 * 取得済みの参考文献を一覧表示する。
 */
function displayReferences(references) {
	const list = document.getElementById('referenceList');

	if (!list) {
		return;
	}

	list.innerHTML = '';

	references.forEach(reference => {
		const label = document.createElement('label');
		const checkbox = document.createElement('input');

		checkbox.type = 'checkbox';
		checkbox.name = 'referenceCheck';
		checkbox.value = JSON.stringify(reference);

		label.appendChild(checkbox);
		label.appendChild(
			document.createTextNode(reference.referenceName)
		);

		list.appendChild(label);
		list.appendChild(document.createElement('br'));
	});
}

/**
 * 現在のカテゴリーに紐づく参考文献を取得する。
 */
async function loadReferences() {
	const groupId = window.categoryGroupId;

	if (!groupId) {
		alert('カテゴリー情報を取得できません');
		return false;
	}

	const response = await fetch(
		`/category/reference/list?groupId=${encodeURIComponent(groupId)}`
	);

	if (!response.ok) {
		alert('参考文献の取得に失敗しました');
		return false;
	}

	referenceCache = await response.json();
	displayReferences(referenceCache);

	return true;
}

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

		await loadReferenceCategories();

		if (!await loadReferences()) {
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
		const categoryId = referenceCategorySelect.value;

		if (!categoryId) {
			displayReferences(referenceCache);
			return;
		}

		displayReferences(
			referenceCache.filter(
				reference =>
					String(reference.categoryId) === String(categoryId)
			)
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
		const categoryPath = window.categoryPath;
		const referenceName =
			document.getElementById('referenceName').value.trim();
		const url =
			document.getElementById('referenceUrl').value.trim();

		if (!categoryPath) {
			alert('カテゴリー情報を取得できません');
			return;
		}

		if (!referenceName) {
			alert('参考文献名を入力してください');
			return;
		}

		const response = await fetch('/category/reference/save', {
			method: 'POST',
			headers: {
				'Content-Type': 'application/x-www-form-urlencoded'
			},
			body:
				`categoryPath=${encodeURIComponent(categoryPath)}`
				+ `&referenceName=${encodeURIComponent(referenceName)}`
				+ `&url=${encodeURIComponent(url)}`
		});

		if (!response.ok) {
			alert('参考文献の登録に失敗しました');
			return;
		}

		document.getElementById('referenceName').value = '';
		document.getElementById('referenceUrl').value = '';

		// 登録後、現在のカテゴリーの参考文献一覧を再取得する。
		await loadReferences();
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

		if (!checked) {
			alert('参考文献を選択してください');
			return;
		}

		const textarea = document.getElementById('content');
		const reference = JSON.parse(checked.value);

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
