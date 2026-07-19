// =====================================================
// image_list.js
//
// 登録済み画像一覧
// ・カテゴリー絞り込み
// ・画像編集
// ・画像インポート
// ・新規画像登録画面遷移
// =====================================================

// -----------------------------------------------------
// Supabase未登録画像をインポート
// -----------------------------------------------------
document.getElementById('importButton').addEventListener('click', () => {

	const status = document.getElementById('importStatus');

	status.textContent = 'インポート中...';

	fetch('/image/import', {
		method: 'POST'
	})
		.then(res => res.json())
		.then(data => {
			status.textContent = `${data.importedCount}件インポートしました`;
			location.reload();
		})
		.catch(() => {
			status.textContent = 'インポートに失敗しました';
		});

});

// -----------------------------------------------------
// カテゴリー絞り込み
// -----------------------------------------------------
document.getElementById('imageCategorySelect').addEventListener('change', function() {

	if (this.value === 'all') {
		location.href = '/image/list';
	} else {
		location.href = '/image/list?categoryId=' + this.value;
	}

});

// -----------------------------------------------------
// 新規画像登録画面
// -----------------------------------------------------
document.getElementById('newImageButton').addEventListener('click', function() {

	location.href = '/image/new';

});

// -----------------------------------------------------
// 編集モーダル表示
// -----------------------------------------------------
document.querySelectorAll('.btn-update').forEach(btn => {

	btn.addEventListener('click', () => {

		document.getElementById('imageId').value =
			btn.dataset.imageId;

		document.getElementById('imageCategoryId').value =
			btn.dataset.categoryId ?? '';

		document.getElementById('imageFolderName').value =
			btn.dataset.folderName ?? '';

		document.getElementById('imageEditModal').style.display =
			'block';

	});

});

// -----------------------------------------------------
// 編集キャンセル
// -----------------------------------------------------
document.getElementById('cancelImageButton').addEventListener('click', () => {

	document.getElementById('imageEditModal').style.display =
		'none';

});

// -----------------------------------------------------
// 編集保存
// -----------------------------------------------------
document.getElementById('saveImageButton').addEventListener('click', () => {

	const params = new URLSearchParams();

	params.append(
		'imageId',
		document.getElementById('imageId').value);

	params.append(
		'categoryId',
		document.getElementById('imageCategoryId').value);

	params.append(
		'folderName',
		document.getElementById('imageFolderName').value);

	fetch('/image/update', {
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