/**
 * 記事編集画面のカテゴリー階層選択を担当するJS
 *
 * サーバーから一括取得したカテゴリー情報を使用し、
 * 親カテゴリーの選択に応じて子カテゴリーの選択肢を表示する。
 */

const categorySelectors = document.getElementById('categorySelectors');
const categoryPathInput = document.getElementById('categorySelect');

const categoryData = window.categorySelects || [];

/**
 * カテゴリー表示名を取得する。
 *
 * displayNameが設定されている場合はdisplayNameを優先し、
 * 未設定の場合はcategoryNameを使用する。
 */
function getCategoryDisplayName(category) {

	if (category.displayName
		&& category.displayName.trim() !== '') {
		return category.displayName;
	}

	return category.categoryName;
}

/**
 * 指定された親カテゴリーを持つカテゴリーを取得する。
 */
function findChildren(parentCategoryId) {

	return categoryData.filter(category =>
		(category.parentCategoryIds || [])
			.some(id => String(id) === String(parentCategoryId))
	);
}

/**
 * カテゴリー選択欄を作成する。
 */
function createCategorySelect(parentCategoryId) {

	const select = document.createElement('select');

	select.className = 'category-level';

	const placeholder = document.createElement('option');

	placeholder.value = '';
	placeholder.textContent = '選択してください';

	select.appendChild(placeholder);

	findChildren(parentCategoryId)
		.sort((a, b) =>
			getCategoryDisplayName(a)
				.localeCompare(
					getCategoryDisplayName(b),
					'ja'
				)
		)
		.forEach(category => {

			const option = document.createElement('option');

			option.value = category.categoryId;
			option.textContent = getCategoryDisplayName(category);

			select.appendChild(option);
		});

	select.addEventListener('change', handleCategoryChange);

	return select;
}

/**
 * カテゴリー選択時の処理。
 *
 * 選択されたカテゴリーに子カテゴリーが存在する場合は、
 * 次の階層の選択欄を追加する。
 */
function handleCategoryChange(event) {
	const select = event.target;

	// 選択した階層より下のセレクトをすべて削除する。
	while (select.nextElementSibling) {
		select.nextElementSibling.remove();
	}

	const selectedCategoryId = select.value;

	if (!selectedCategoryId) {
		updateCategoryPath();
		return;
	}

	const children = findChildren(selectedCategoryId);

	if (children.length > 0) {
		categorySelectors.appendChild(
			createCategorySelect(selectedCategoryId)
		);
	}

	updateCategoryPath();
}

/**
 * 現在選択されているカテゴリー経路をhidden inputへ設定する。
 *
 * 例：
 * reference/batman/gadget/batman
 */
function updateCategoryPath() {
	const selectedCategories = [];

	categorySelectors.querySelectorAll('.category-level').forEach(select => {
		if (!select.value) return;

		const category = categoryData.find(
			item => String(item.categoryId) === String(select.value)
		);

		if (category) {
			selectedCategories.push(category.categoryName);
		}
	});

	// 保存時は既存のcategorySelectパラメータとしてカテゴリー経路を送る。
	categoryPathInput.value = selectedCategories.join('/');
}

/**
 * 初期カテゴリー選択欄を作成する。
 *
 * ArticleCategory.parentCategoryIdを使わず、
 * parentCategoryIdsが空のカテゴリーをルートとして扱う。
 */
function initializeCategorySelectors() {
	categorySelectors.innerHTML = '';

	const select = document.createElement('select');
	select.className = 'category-level';

	const placeholder = document.createElement('option');
	placeholder.value = '';
	placeholder.textContent = '選択してください';
	select.appendChild(placeholder);

	categoryData
		.filter(category =>
			!category.parentCategoryIds ||
			category.parentCategoryIds.length === 0
		)
		.sort((a, b) =>
			getCategoryDisplayName(a).localeCompare(
				getCategoryDisplayName(b),
				'ja'
			)
		)
		.forEach(category => {
			const option = document.createElement('option');
			option.value = category.categoryId;
			option.textContent = getCategoryDisplayName(category);
			select.appendChild(option);
		});

	select.addEventListener('change', handleCategoryChange);

	categorySelectors.appendChild(select);
}

function restoreCategorySelection() {

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

	for (const categoryName of pathParts) {

		const category = categoryData.find(item => {

			if (item.categoryName !== categoryName) {
				return false;
			}

			if (parentCategoryId === null) {
				return !item.parentCategoryIds
					|| item.parentCategoryIds.length === 0;
			}

			return (item.parentCategoryIds || [])
				.some(id =>
					String(id) === String(parentCategoryId)
				);
		});

		if (!category) {
			return;
		}

		const selects =
			categorySelectors.querySelectorAll('.category-level');

		const select = selects[selects.length - 1];

		select.value = category.categoryId;

		parentCategoryId = category.categoryId;

		const children = findChildren(parentCategoryId);

		if (children.length === 0) {
			break;
		}

		categorySelectors.appendChild(
			createCategorySelect(parentCategoryId)
		);
	}

	updateCategoryPath();
}

initializeCategorySelectors();
restoreCategorySelection();

