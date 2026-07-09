// カテゴリー編集モーダル制御

const modal = document.getElementById('categoryModal');
const modalTitle = document.getElementById('categoryModalTitle');

const categoryIdInput = document.getElementById('categoryId');
const categoryNameInput = document.getElementById('categoryName');
const displayNameInput = document.getElementById('displayName');
const parentCategoryInput = document.getElementById('parentCategoryId');

let editMode = false;

// 親カテゴリー一覧生成
function rebuildParentCategorySelect(currentId, selectedId) {

	parentCategoryInput.innerHTML = '';

	const none = document.createElement('option');
	none.value = '';
	none.textContent = 'なし';
	parentCategoryInput.appendChild(none);

	categories.forEach(c => {

		if (String(c.categoryId) === String(currentId)) {
			return;
		}

		const option = document.createElement('option');
		option.value = c.categoryId;
		option.textContent = c.displayName;

		if (String(c.categoryId) === String(selectedId)) {
			option.selected = true;
		}

		parentCategoryInput.appendChild(option);
	});
}

// 編集
document.querySelectorAll('.btn-rename').forEach(btn => {
	btn.addEventListener('click', () => {

		editMode = true;

		modalTitle.textContent = 'カテゴリー編集';

		categoryIdInput.value = btn.dataset.categoryId;
		categoryNameInput.value = btn.dataset.categoryName;
		displayNameInput.value = btn.dataset.displayName;
		rebuildParentCategorySelect(
			categoryIdInput.value,
			btn.dataset.parentCategoryId
		);

		modal.style.display = 'block';
	});
});

// 保存
document.getElementById('saveCategoryButton').addEventListener('click', () => {

	const params = new URLSearchParams();

	params.append('categoryName', categoryNameInput.value.trim());
	params.append('displayName', displayNameInput.value.trim());

	if (parentCategoryInput.value) {
		params.append('parentCategoryId', parentCategoryInput.value);
	}

	let url = '/category/add';

	if (editMode) {
		url = '/category/rename';
		params.append('categoryId', categoryIdInput.value);
		params.append('newName', categoryNameInput.value.trim());
	}

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

// キャンセル
document.getElementById('cancelCategoryButton').addEventListener('click', () => {
	modal.style.display = 'none';
});

// カテゴリー追加
document.getElementById('addCategoryButton').addEventListener('click', () => {

	editMode = false;

	modalTitle.textContent = 'カテゴリー追加';

	categoryIdInput.value = '';
	categoryNameInput.value = '';
	displayNameInput.value = '';
	rebuildParentCategorySelect('', '');

	modal.style.display = 'block';
});
