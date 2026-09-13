/**
 * 画像管理画面からの新規画像登録
 */

document.getElementById('uploadButton')
	.addEventListener('click', function() {

		const categoryId =
			document.getElementById('categorySelect').value;

		const file =
			document.getElementById('imageFile').files[0];

		const folderName =
			document.getElementById('folderName').value;

		const status =
			document.getElementById('status');

		if (categoryId) {

			formData.append(
				'categoryId',
				categoryId);

		}
		if (!file) {
			alert('画像を選択してください');
			return;
		}


		const formData = new FormData();

		formData.append(
			'file',
			file);

		formData.append(
			'categoryId',
			categoryId);


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

					location.href =
						'/image/list';

				} else {

					status.textContent =
						'登録失敗';

				}

			})
			.catch(() => {

				status.textContent =
					'登録失敗';

			});

	});

// カテゴリー変更時にデフォルトフォルダ名を取得
document.getElementById('categorySelect')
	.addEventListener('change', function() {

		const categoryId = this.value;

		const folderInput =
			document.getElementById('folderName');


		// 未分類の場合は空欄
		if (!categoryId) {

			folderInput.value = '';

			return;
		}


		fetch(
			'/article/images/default-folder?categoryId=' + categoryId
		)
			.then(res => res.json())
			.then(data => {

				folderInput.value =
					data.folderName || '';

			})
			.catch(err => {

				console.error(err);

			});

	});
