// カテゴリー編集モーダル制御

const modal = document.getElementById('categoryModal');
const modalTitle = document.getElementById('categoryModalTitle');

const categoryIdInput = document.getElementById('categoryId');
const categoryNameInput = document.getElementById('categoryName');
const displayNameInput = document.getElementById('displayName');
const parentCategoryInput = document.getElementById('parentCategoryIds');

let editMode = false;

// 親カテゴリー一覧生成
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

// 編集
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

// 保存
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

// キャンセル
const cancelCategoryButton = document.getElementById('cancelCategoryButton');

if (cancelCategoryButton) {

	cancelCategoryButton.addEventListener('click', () => {
		modal.style.display = 'none';
	});

}

// カテゴリー追加
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