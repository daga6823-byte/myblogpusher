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

		// 編集画面のカテゴリー選択がある場合だけ取得する。
		const categorySelect =
			document.getElementById('categorySelect');

		const categoryId =
			categorySelect ? categorySelect.value : '';

		const status =
			document.getElementById('imageUploadStatus');

		if (!fileInput.files.length) {

			alert('画像ファイルを選択してください');
			return;
		}

		const formData = new FormData();

		for (const file of fileInput.files) {
			formData.append('files', file);
		}

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

				const successCount = data.successCount ?? 0;
				const totalCount = data.totalCount ?? 0;
				const failureCount = totalCount - successCount;

				if (successCount > 0) {

					status.textContent = failureCount === 0
						? `${successCount}枚の画像をアップロードしました`
						: `${successCount}枚成功、${failureCount}枚失敗しました`;

					const currentFolderName =
						imageState.folderName;

					loadImageFolders();
					loadImageCategories();

					imageState.folderName =
						currentFolderName;

					loadImageList();

				} else {

					status.textContent =
						data.results?.map(result =>
							`${result.fileName}: ${result.message}`
						).join(' / ')
						|| 'アップロードに失敗しました';
				}

				if (successCount > 0) {
					fileInput.value = '';
				}
			})
			.catch(err => {

				console.error(err);

				status.textContent =
					'アップロードに失敗しました';
			});
	}
);
