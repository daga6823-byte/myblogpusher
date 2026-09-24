// カテゴリー編集モーダル制御

const modal = document.getElementById('categoryModal');

const modalTitle = document.getElementById('categoryModalTitle');

const categoryIdInput = document.getElementById('categoryId');

const categoryNameInput = document.getElementById('categoryName');

const displayNameInput = document.getElementById('displayName');

const parentCategoryInput = document.getElementById('parentCategoryIds');

let editMode = false;

/**
 * 現在のソート条件。
 *
 * 初期状態はカテゴリー名の昇順にする。
 */
let currentSortKey = 'categoryName';

let currentSortDirection = 'asc';

/**
 * 親カテゴリー一覧生成
 */
function rebuildParentCategorySelect(currentId, selectedIds) {

	parentCategoryInput.innerHTML = '';

	const selectedIdSet = new Set(

		(selectedIds || []).map(id => String(id))

	);

	categories.forEach(c => {

		if (!c.displayName || c.displayName.trim() === '') {

			return;

		}

		if (String(c.categoryId) === String(currentId)) {

			return;

		}

		const wrapper = document.createElement('label');

		const checkbox = document.createElement('input');

		checkbox.type = 'checkbox';

		checkbox.name = 'parentCategoryIds';

		checkbox.value = c.categoryId;

		if (selectedIdSet.has(String(c.categoryId))) {

			checkbox.checked = true;

		}

		wrapper.appendChild(checkbox);

		wrapper.appendChild(

			document.createTextNode(' ' + c.displayName)

		);

		parentCategoryInput.appendChild(wrapper);

		parentCategoryInput.appendChild(

			document.createElement('br')

		);

	});

}

/**
 * 指定された項目でカテゴリー一覧を並び替える。
 */
function sortCategoryRows(rows) {

	rows.sort((a, b) => {

		const columnIndex = {
			categoryName: 0,
			displayName: 1,
			parentCategoryName: 2,
			typoCount: 3
		}[currentSortKey];

		if (columnIndex === undefined) {

			return 0;

		}

		const aCell =
			a.querySelectorAll('td')[columnIndex];

		const bCell =
			b.querySelectorAll('td')[columnIndex];

		if (!aCell || !bCell) {

			return 0;

		}

		let result;

		if (currentSortKey === 'typoCount') {

			const aValue =
				Number.parseInt(
					aCell.textContent.trim(),
					10
				) || 0;

			const bValue =
				Number.parseInt(
					bCell.textContent.trim(),
					10
				) || 0;

			result = aValue - bValue;

		} else {

			result =
				aCell.textContent.trim().localeCompare(
					bCell.textContent.trim(),
					'ja'
				);

		}

		return currentSortDirection === 'asc'
			? result
			: -result;

	});

}

/**
 * ソートボタンに現在のソート状態を表示する。
 */
function updateSortButtons() {

	document.querySelectorAll('.sort-button')
		.forEach(button => {

			const sortKey =
				button.dataset.sort;

			const baseText =
				button.textContent
					.replace(/[↑↓]/g, '')
					.trim();

			if (sortKey === currentSortKey) {

				const arrow =
					currentSortDirection === 'asc'
						? ' ↑'
						: ' ↓';

				button.textContent =
					baseText + arrow;

			} else {

				button.textContent =
					baseText;

			}

		});

}

/**
 * カテゴリー一覧を現在のソート条件で並び替える。
 */
function updateCategoryList() {

	const list =
		document.getElementById('categoryList');

	if (!list) {

		return;

	}

	const rows =
		Array.from(
			list.querySelectorAll('tr')
		);

	sortCategoryRows(rows);

	rows.forEach(row => {

		list.appendChild(row);

	});

	updateSortButtons();

}

/**
 * ソートボタンを初期化する。
 */
function initializeSortButtons() {

	document.querySelectorAll('.sort-button')
		.forEach(button => {

			button.addEventListener(
				'click',
				() => {

					const sortKey =
						button.dataset.sort;

					if (sortKey === currentSortKey) {

						currentSortDirection =
							currentSortDirection === 'asc'
								? 'desc'
								: 'asc';

					} else {

						currentSortKey = sortKey;

						currentSortDirection = 'asc';

					}

					updateCategoryList();

				}
			);

		});

}

/**
 * 編集
 */
document.querySelectorAll('.btn-update').forEach(btn => {

	btn.addEventListener('click', () => {

		editMode = true;

		modalTitle.textContent = 'カテゴリー編集';

		categoryIdInput.value = btn.dataset.categoryId;

		categoryNameInput.value = btn.dataset.categoryName;

		displayNameInput.value = btn.dataset.displayName;

		const category = categories.find(

			c => String(c.categoryId) === String(categoryIdInput.value)

		);

		const selectedIds = category

			? category.parentCategoryIds

			: [];

		rebuildParentCategorySelect(

			categoryIdInput.value,

			selectedIds

		);

		modal.style.display = 'block';

	});

});

/**
 * 保存
 */
const saveCategoryButton = document.getElementById('saveCategoryButton');

if (saveCategoryButton) {

	saveCategoryButton.addEventListener('click', () => {

		const params = new URLSearchParams();

		let url = '/category/add';

		if (editMode) {

			url = '/category/update';

			params.append(

				'categoryId',

				categoryIdInput.value

			);

			params.append(

				'newName',

				categoryNameInput.value.trim()

			);

		} else {

			params.append(

				'categoryName',

				categoryNameInput.value.trim()

			);

		}

		params.append(

			'displayName',

			displayNameInput.value.trim()

		);

		parentCategoryInput

			.querySelectorAll('input[name="parentCategoryIds"]:checked')

			.forEach(checkbox => {

				params.append(

					'parentCategoryIds',

					checkbox.value

				);

			});

		fetch(url, {

			method: 'POST',

			headers: {

				'Content-Type': 'application/x-www-form-urlencoded'

			},

			body: params.toString()

		})

			.then(res => res.json())

			.then(data => {

				if (data.result === 'ok') {

					location.reload();

				} else {

					alert(data.message);

				}

			});

	});

}

/**
 * キャンセル
 */
const cancelCategoryButton = document.getElementById('cancelCategoryButton');

if (cancelCategoryButton) {

	cancelCategoryButton.addEventListener('click', () => {

		modal.style.display = 'none';

	});

}

/**
 * カテゴリー追加
 */
const addCategoryButton = document.getElementById('addCategoryButton');

if (addCategoryButton) {

	addCategoryButton.addEventListener('click', () => {

		editMode = false;

		modalTitle.textContent = 'カテゴリー追加';

		categoryIdInput.value = '';

		categoryNameInput.value = '';

		displayNameInput.value = '';

		rebuildParentCategorySelect(null, []);

		modal.style.display = 'block';

	});

}

/**
 * カテゴリー一覧の初期化
 */
function initializeCategoryList() {

	initializeSortButtons();

	updateCategoryList();

}

if (document.readyState === 'loading') {

	document.addEventListener(
		'DOMContentLoaded',
		initializeCategoryList
	);

} else {

	initializeCategoryList();

}