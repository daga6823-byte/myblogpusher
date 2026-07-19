/**
 * 画像管理画面からの新規画像登録
 */

document.getElementById('uploadButton')
.addEventListener('click', function(){

	const categoryId =
		document.getElementById('categorySelect').value;

	const file =
		document.getElementById('imageFile').files[0];

	const folderName =
		document.getElementById('folderName').value;

	const status =
		document.getElementById('status');


	if (!categoryId) {
		alert('カテゴリーを選択してください');
		return;
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