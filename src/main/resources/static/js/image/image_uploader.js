/**
 * image_uploader.js
 *
 * 記事編集画面からの画像アップロード処理を担当する。
 */

import { imageState } from './image_state.js';
import { loadImageFolders, loadImageCategories } from './image_folders.js';
import { loadImageList } from './image_gallery.js';

document.getElementById('imageUploadButton').addEventListener(
	'click',
	function() {

		const fileInput =
			document.getElementById('imageFileInput');

		let folderName = '';

		const folderSelect =
			document.getElementById('imageFolderSelect');

		const newFolderInput =
			document.getElementById('newImageFolderName');

		if (newFolderInput.value.trim()) {

			folderName =
				newFolderInput.value.trim();

		} else {

			folderName =
				folderSelect.value;
		}

		const categoryId =
			document.getElementById('categorySelect').value;

		const status =
			document.getElementById('imageUploadStatus');

		if (!fileInput.files.length) {

			alert('画像ファイルを選択してください');
			return;
		}

		const formData = new FormData();

		formData.append(
			'file',
			fileInput.files[0]);

		if (categoryId && categoryId !== '__new__') {

			formData.append(
				'categoryId',
				categoryId);
		}

		if (folderName) {

			formData.append(
				'folderName',
				folderName);
		}

		status.textContent = 'アップロード中...';

		fetch('/article/images/upload', {
			method: 'POST',
			body: formData
		})
			.then(res => res.json())
			.then(data => {

				if (data.result === 'ok') {

					status.textContent =
						'アップロードしました';

					fileInput.value = '';

					const currentFolderName =
						imageState.folderName;

					loadImageFolders();
					loadImageCategories();

					imageState.folderName =
						currentFolderName;

					loadImageList();

				} else {

					status.textContent =
						data.message
						|| 'アップロードに失敗しました';
				}
			})
			.catch(err => {

				console.error(err);

				status.textContent =
					'アップロードに失敗しました';
			});
	}
);
