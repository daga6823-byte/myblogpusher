// =====================================================
// 記事編集画面のカテゴリー階層選択を担当するJS
//
// サーバーから一括取得したカテゴリー情報を使用し、
// 親カテゴリーの選択に応じて子カテゴリーの選択肢を表示する。
// =====================================================

const categorySelectors = document.getElementById('categorySelectors');
const categoryPathInput = document.getElementById('categorySelect');

const categoryData = window.categorySelects || [];

// =====================================================
// カテゴリー表示名を取得する。
//
// displayNameが設定されている場合はdisplayNameを優先し、
// 未設定の場合はcategoryNameを使用する。
// =====================================================
function getCategoryDisplayName(category) {

	if (category.displayName
		&& category.displayName.trim() !== '') {
		return category.displayName;
	}

	return category.categoryName;
}

// =====================================================
// 指定された親カテゴリーを持つカテゴリーを取得する。
// =====================================================
function findChildren(parentCategoryId) {

	// 最上位カテゴリーを取得
	if (parentCategoryId == null) {
		return categoryData.filter(category =>
			category.parentCategoryId == null
		);
	}

	// 未登録カテゴリーの下には候補を表示しない
	if (parentCategoryId === '__pending__') {
		return [];
	}

	// 指定された親カテゴリーIDに属する子カテゴリーを取得
	return categoryData.filter(category =>
		String(category.parentCategoryId) === String(parentCategoryId)
	);
}

// =====================================================
// カテゴリー選択欄を作成する。
// =====================================================
function createCategorySelect(parentCategoryId) {

	const select = document.createElement('select');
	select.className = 'category-level';

	const placeholder = document.createElement('option');
	placeholder.value = '';
	placeholder.textContent = '選択してください';

	select.appendChild(placeholder);

	const children = findChildren(parentCategoryId);

	children
		.sort((a, b) =>
			getCategoryDisplayName(a).localeCompare(
				getCategoryDisplayName(b),
				'ja'
			)
		)
		.forEach(category => {

			const option = document.createElement('option');

			option.value = category.categoryId;
			option.dataset.categoryName = category.categoryName;
			option.dataset.groupId = category.groupId ?? '';

			option.textContent = getCategoryDisplayName(category);

			select.appendChild(option);
		});

	// 新しいカテゴリーの作成 विकल्प
	const newOption = document.createElement('option');
	newOption.value = '__new__';
	newOption.textContent = '＋新しいカテゴリーを作成';

	select.appendChild(newOption);

	select.addEventListener('change', handleCategoryChange);

	return select;
}

// =====================================================
// カテゴリー選択時の処理。
//
// 選択されたカテゴリーに子カテゴリーが存在する場合は、
// 次の階層の選択欄を追加する。
// =====================================================
function handleCategoryChange(event) {
	const select = event.target;

	// 選択した階層より下のセレクトをすべて削除する。
	while (select.nextElementSibling) {
		select.nextElementSibling.remove();
	}

	const selectedCategoryId = select.value;

	// 「新しいカテゴリーを作成」が選択された場合。
	// 現在の選択経路を親カテゴリーとして保持し、新規カテゴリー名の入力欄を表示する。
	if (selectedCategoryId === '__new__') {

		const newCategoryInput =
			document.getElementById('newCategoryNameInput');

		const newCategoryDisplayNameInput =
			document.getElementById('newCategoryDisplayNameInput');

		if (newCategoryInput) {
			newCategoryInput.style.display = 'block';
			newCategoryInput.value = '';
			document.getElementById('newCategoryName').value = '';
			newCategoryInput.focus();
		}

		if (newCategoryDisplayNameInput) {
			newCategoryDisplayNameInput.style.display = 'block';
			newCategoryDisplayNameInput.value = '';
			document.getElementById('newCategoryDisplayName').value = '';
		}

		updateCategoryPath();

		return;
	}

	// 通常のカテゴリーを選択した場合は、新規カテゴリー入力を非表示にする。
	const newCategoryInput =
		document.getElementById('newCategoryNameInput');

	const newCategoryDisplayNameInput =
		document.getElementById('newCategoryDisplayNameInput');

	if (newCategoryInput) {
		newCategoryInput.style.display = 'none';
		newCategoryInput.value = '';
	}

	if (newCategoryDisplayNameInput) {
		newCategoryDisplayNameInput.style.display = 'none';
		newCategoryDisplayNameInput.value = '';
	}

	document.getElementById('newCategoryName').value = '';
	document.getElementById('newCategoryDisplayName').value = '';

	if (!selectedCategoryId) {

		updateCategoryPath();

		return;
	}

	const selectedOption = select.selectedOptions[0];
	categorySelectors.appendChild(createCategorySelect(selectedCategoryId));

	updateCategoryPath();
}
/**
 * 選択されたカテゴリー経路からgroupIdを取得する。
 *
 * 古いリクエストの結果で新しい選択を上書きしないよう、
 * リクエスト番号で管理する。
 */
let categoryGroupIdRequestId = 0;

async function updateCategoryGroupId(categoryPath) {
	const requestId = ++categoryGroupIdRequestId;

	window.categoryGroupId = null;

	if (!categoryPath) {
		return;
	}

	try {
		const response = await fetch(
			`/article/category/group-id?categoryPath=${encodeURIComponent(categoryPath)}`
		);

		if (!response.ok) {
			return;
		}

		const groupId = await response.json();

		if (requestId !== categoryGroupIdRequestId) {
			return;
		}

		window.categoryGroupId = groupId;

	} catch (error) {
		console.error(
			'カテゴリーgroupIdの取得に失敗しました:',
			error
		);
	}
}

// =====================================================
// 現在選択されているカテゴリー経路をhidden inputへ設定する。
//
// 例：
// reference/batman/gadget/batman
// =====================================================
function updateCategoryPath() {

	const selectedCategories = [];
	let selectedGroupId = null;

	categorySelectors.querySelectorAll('.category-level').forEach(select => {

		if (!select.value || select.value === '__new__') {
			return;
		}

		const option = select.selectedOptions[0];

		if (select.value === '__pending__') {
			selectedCategories.push(option.textContent);
			selectedGroupId = null;
			return;
		}

		selectedCategories.push(option.dataset.categoryName);
		selectedGroupId = option.dataset.groupId || null;
	});

	categoryPathInput.value = selectedCategories.join('/');
	window.categoryGroupId = selectedGroupId;
}

// =====================================================
// 初期カテゴリー選択欄を作成する。
//
// ArticleCategory.parentCategoryIdを使わず、
// parentCategoryIdsが空のカテゴリーをルートとして扱う。
// =====================================================
// =====================================================
// 初期カテゴリー選択欄を作成する。
// =====================================================

function initializeCategorySelectors() {

	categorySelectors.innerHTML = '';

	// ルートカテゴリーの選択欄を作成する。
	categorySelectors.appendChild(
		createCategorySelect(null)
	);

	const newCategoryInput =
		document.getElementById('newCategoryNameInput');

	const newCategoryDisplayNameInput =
		document.getElementById('newCategoryDisplayNameInput');

	if (newCategoryInput) {
		newCategoryInput.style.display = 'none';
	}

	if (newCategoryDisplayNameInput) {
		newCategoryDisplayNameInput.style.display = 'none';
	}
}

// =====================================================
// 新規カテゴリーの表示用入力欄と送信用hidden項目を同期する。
// =====================================================
function syncNewCategoryInputs() {

	const nameInput =
		document.getElementById('newCategoryNameInput');
	const nameHidden =
		document.getElementById('newCategoryName');

	const displayNameInput =
		document.getElementById('newCategoryDisplayNameInput');
	const displayNameHidden =
		document.getElementById('newCategoryDisplayName');

	if (nameInput && nameHidden) {

		// 保存後に引き継いだhidden値を表示用入力欄へ復元する。
		nameInput.value = nameHidden.value;

		if (nameHidden.value) {
			nameInput.style.display = 'block';
		}

		nameInput.addEventListener('input', () => {
			nameHidden.value = nameInput.value;
		});
	}

	if (displayNameInput && displayNameHidden) {

		// 保存後に引き継いだhidden値を表示用入力欄へ復元する。
		displayNameInput.value = displayNameHidden.value;

		if (displayNameHidden.value) {
			displayNameInput.style.display = 'block';
		}

		displayNameInput.addEventListener('input', () => {
			displayNameHidden.value = displayNameInput.value;
		});
	}
}


initializeCategorySelectors();
syncNewCategoryInputs();
restoreCategorySelection();


// =====================================================
// 保存後のカテゴリー選択状態を復元する。
//
// 登録済みカテゴリーは通常の選択肢として復元し、
// DB未登録の新規カテゴリー名は一時的な選択肢として表示する。
// =====================================================

function restoreCategorySelection() {

	const newCategoryNameInput =
		document.getElementById('newCategoryNameInput');

	const newCategoryNameHidden =
		document.getElementById('newCategoryName');

	const newCategoryDisplayNameInput =
		document.getElementById('newCategoryDisplayNameInput');

	const newCategoryDisplayNameHidden =
		document.getElementById('newCategoryDisplayName');

	if (!window.categoryPath) {
		return;
	}

	const pathParts = window.categoryPath
		.split('/')
		.filter(part => part !== '');

	if (pathParts.length === 0) {
		return;
	}

	let parentCategoryId = null;
	let lastGroupId = null;

	for (const categoryName of pathParts) {

		const category = categoryData.find(item =>
			item.categoryName === categoryName &&
			(
				parentCategoryId === null
					? item.parentCategoryId == null
					: String(item.parentCategoryId) ===
					String(parentCategoryId)
			)
		);

		const selects =
			categorySelectors.querySelectorAll('.category-level');

		const select = selects[selects.length - 1];

		if (!select) {
			return;
		}

		if (!category) {

			const option = document.createElement('option');

			option.value = '__pending__';
			option.textContent = categoryName;
			option.selected = true;

			select.appendChild(option);

			if (newCategoryNameInput && newCategoryNameHidden) {
				newCategoryNameInput.value = categoryName;
				newCategoryNameHidden.value = categoryName;
				newCategoryNameInput.style.display = 'block';
			}

			if (
				newCategoryDisplayNameInput &&
				newCategoryDisplayNameHidden &&
				newCategoryDisplayNameHidden.value
			) {
				newCategoryDisplayNameInput.value =
					newCategoryDisplayNameHidden.value;

				newCategoryDisplayNameInput.style.display = 'block';
			}

			break;
		}

		let option = Array.from(select.options).find(
			item => String(item.value) ===
				String(category.categoryId)
		);

		if (!option) {

			option = document.createElement('option');

			option.value = category.categoryId;
			option.dataset.categoryName = category.categoryName;
			option.dataset.groupId = category.groupId;
			option.textContent = getCategoryDisplayName(category);

			select.appendChild(option);
		}

		select.value = String(category.categoryId);

		parentCategoryId = category.categoryId;
		lastGroupId = category.groupId;

		categorySelectors.appendChild(
			createCategorySelect(parentCategoryId)
		);
	}

	categoryPathInput.value = window.categoryPath;
	window.categoryGroupId = lastGroupId;
}
