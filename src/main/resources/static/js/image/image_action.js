// =====================================================

// image_action.js

//

// 登録済み画像に対する操作を担当する。

// ・新規画像登録

// ・画像編集

// ・画像削除

// ・画像インポート

// =====================================================

// -----------------------------------------------------

// 新規画像登録画面

// -----------------------------------------------------

document.getElementById('newImageButton')

	.addEventListener('click', function() {

		location.href = '/image/new';

	});

// -----------------------------------------------------

// 画像インポート

// -----------------------------------------------------

document.getElementById('importButton')

	.addEventListener('click', () => {

		const status =

			document.getElementById('importStatus');

		status.textContent = 'インポート中...';

		fetch('/image/import', {

			method: 'POST'

		})

			.then(res => res.json())

			.then(data => {

				status.textContent =

					`${data.importedCount}件インポートしました`;

				location.reload();

			})

			.catch(() => {

				status.textContent =

					'インポートに失敗しました';

			});

	});

// -----------------------------------------------------

// 編集・削除ボタンのイベント設定

// -----------------------------------------------------

function bindImageButtons() {

	// 編集

	document.querySelectorAll('.btn-update')

		.forEach(btn => {

			btn.addEventListener('click', () => {

				document.getElementById('imageId').value =

					btn.dataset.imageId;

				document.getElementById('imageFileName').value =

					btn.dataset.fileName ?? '';

				document.getElementById('imageFolderName').value =

					btn.dataset.folderName ?? '';

				document.getElementById('imageFile').value = '';

				document.getElementById('imageEditModal')

					.style.display = 'block';

			});

		});

	// 削除

	document.querySelectorAll('.btn-image-delete')

		.forEach(btn => {

			btn.addEventListener('click', () => {

				const imageId =

					btn.dataset.imageId;

				if (!confirm('この画像を削除しますか？')) {

					return;

				}

				const params = new URLSearchParams();

				params.append('imageId', imageId);

				fetch('/article/images/delete', {

					method: 'POST',

					headers: {

						'Content-Type':

							'application/x-www-form-urlencoded'

					},

					body: params.toString()

				})

					.then(res => res.json())

					.then(data => {

						if (data.result === 'ok') {

							// DB変更後、最新の全画像を取得し直す。

							loadImageList();

						} else {

							alert(data.message);

						}

					});

			});

		});

}

// -----------------------------------------------------

// 編集キャンセル

// -----------------------------------------------------

document.getElementById('cancelImageButton')

	.addEventListener('click', () => {

		document.getElementById('imageEditModal')

			.style.display = 'none';

	});

// -----------------------------------------------------

// 編集保存

// -----------------------------------------------------

document.getElementById('saveImageButton')

	.addEventListener('click', () => {

		const formData = new FormData();

		formData.append(

			'imageId',

			document.getElementById('imageId').value

		);

		formData.append(

			'folderName',

			document.getElementById('imageFolderName').value

		);

		formData.append(

			'fileName',

			document.getElementById('imageFileName').value

		);

		const fileInput =

			document.getElementById('imageFile');

		if (fileInput.files.length > 0) {

			formData.append(

				'file',

				fileInput.files[0]);

		}

		fetch('/image/update', {

			method: 'POST',

			body: formData

		})

			.then(res => res.json())

			.then(data => {

				if (data.result === 'ok') {

					// 編集モーダルを閉じる。
					document.getElementById('imageEditModal')
						.style.display = 'none';

					// DB・Storage更新後の最新画像情報を再取得する。
					// 現在のカテゴリーとページ位置は維持する。
					loadImageList();

				} else {

					alert(data.message);

				}

			});

	});