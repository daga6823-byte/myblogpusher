// =====================================================
// image_list.js
//
// 登録済み画像一覧
// ・カテゴリー絞り込み
// ・画像編集
// ・画像インポート
// ・新規画像登録画面遷移
// ・画像編集
// ・画像削除
// ・画像インポート
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

	const category = this.value;

	if (category === 'all') {
		location.href = '/image/list?page=0';
	} else {
		location.href =
			'/image/list?folderName='
			+ encodeURIComponent(category)
			+ '&page=0';
	}

});

// -----------------------------------------------------
// ページ切替
// -----------------------------------------------------
const pageSelect = document.getElementById('pageSelect');

if (pageSelect) {

	pageSelect.addEventListener('change', function() {

		const category = document.getElementById('imageCategorySelect').value;
		const page = this.value;

		if (category === 'all') {
			location.href = '/image/list?page=' + page;
		} else {
			location.href =
				'/image/list?folderName='
				+ encodeURIComponent(category)
				+ '&page=' + page;
		}

	});

}

// -----------------------------------------------------
// 前ページ
// -----------------------------------------------------
const prevButton = document.getElementById('prevPageButton');

if (prevButton) {

	prevButton.addEventListener('click', function() {

		const category = document.getElementById('imageCategorySelect').value;
		const current = Number(document.getElementById('pageSelect').value);

		if (current <= 0) {
			return;
		}

		const page = current - 1;

		if (category === 'all') {
			location.href = '/image/list?page=' + page;
		} else {
			location.href =
				'/image/list?folderName='
				+ encodeURIComponent(category)
				+ '&page=' + page;
		}

	});

}

// -----------------------------------------------------
// 次ページ
// -----------------------------------------------------
const nextButton = document.getElementById('nextPageButton');

if (nextButton) {

	nextButton.addEventListener('click', function() {

		const category = document.getElementById('imageCategorySelect').value;
		const current = Number(document.getElementById('pageSelect').value);

		const page = current + 1;

		if (category === 'all') {
			location.href = '/image/list?page=' + page;
		} else {
			location.href =
				'/image/list?folderName='
				+ encodeURIComponent(category)
				+ '&page=' + page;
		}

	});

}

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

// -----------------------------------------------------
// 画像削除
// -----------------------------------------------------
document.querySelectorAll('.btn-image-delete').forEach(btn => {

	btn.addEventListener('click', () => {

		const imageId = btn.dataset.imageId;

		if (!confirm('この画像を削除しますか？')) {
			return;
		}

		const params = new URLSearchParams();
		params.append('imageId', imageId);

		fetch('/article/images/delete', {
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

});
