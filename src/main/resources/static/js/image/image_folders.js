/**
 * image_folders.js
 *
 * カテゴリー別のデフォルト画像フォルダ取得、
 * 画像カテゴリー一覧・保存先フォルダ一覧の取得を担当する。
 */

import { imageState } from './image_state.js';

// デフォルトフォルダ名を取得
export function loadDefaultFolderName() {

	const categorySelect = document.getElementById('categorySelect');

	if (!categorySelect || !categorySelect.value) {
		return;
	}

	fetch('/article/images/default-folder?categoryId=' + categorySelect.value)
		.then(res => res.json())
		.then(data => {

			const folderSelect =
				document.getElementById('imageFolderSelect');

			if (folderSelect && data.folderName) {
				folderSelect.value = data.folderName;
			}
		})
		.catch(err => {
			console.error(
				'画像デフォルトフォルダの取得に失敗しました',
				err);
		});
}

// 画像カテゴリー一覧を取得
export function loadImageCategories() {

	const select =
		document.getElementById('imageCategorySelect');

	if (!select) {
		return;
	}

	fetch('/image/categories')
		.then(res => res.json())
		.then(categories => {

			const currentValue =
				imageState.folderName;

			select.innerHTML = '';

			const all =
				document.createElement('option');

			all.value = '';
			all.textContent = 'すべて';

			select.appendChild(all);

			categories.forEach(category => {

				const option =
					document.createElement('option');

				option.value =
					category.folderName;

				option.textContent =
					category.folderName;

				select.appendChild(option);
			});

			if (currentValue) {

				select.value =
					currentValue;
			}
		});
}

// 画像保存先フォルダ一覧を取得
export function loadImageFolders() {

	const select =
		document.getElementById('imageFolderSelect');

	if (!select) {
		return;
	}

	fetch('/image/folders')
		.then(res => res.json())
		.then(folders => {

			const currentValue =
				select.value;

			select.innerHTML = '';

			const none =
				document.createElement('option');

			none.value = '';
			none.textContent =
				'選択してください';

			select.appendChild(none);

			folders.forEach(folder => {

				const option =
					document.createElement('option');

				option.value = folder;
				option.textContent = folder;

				select.appendChild(option);
			});

			const newOption =
				document.createElement('option');

			newOption.value = '__new__';
			newOption.textContent =
				'＋ 新しいフォルダを追加';

			select.appendChild(newOption);

			if (currentValue
				&& currentValue !== '__new__') {

				select.value =
					currentValue;
			}
		});
}

// 新しいフォルダ名入力欄の表示切り替え
const imageFolderSelect =
	document.getElementById('imageFolderSelect');

const newImageFolderName =
	document.getElementById('newImageFolderName');

if (imageFolderSelect) {

	imageFolderSelect.addEventListener(
		'change',
		function() {

			if (this.value === '__new__') {

				newImageFolderName.style.display =
					'block';

			} else {

				newImageFolderName.style.display =
					'none';

				newImageFolderName.value = '';
			}
		});
}