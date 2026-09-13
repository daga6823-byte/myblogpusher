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
// 画像一覧の状態
// -----------------------------------------------------

let allImages = [];
let currentPage = 0;

// 一覧画面で1ページに表示する画像数。
const imagesPerPage = 12;

// -----------------------------------------------------
// 画像一覧取得
// -----------------------------------------------------

function loadImageList() {

	fetch('/article/images')
		.then(res => res.json())
		.then(images => {

			allImages = images;
			currentPage = 0;

			updateImageList();
		})
		.catch(() => {

			console.error('画像一覧の取得に失敗しました');

		});
}

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

	// カテゴリー変更時は先頭ページへ戻す。
	currentPage = 0;

	updateImageList();

});

// -----------------------------------------------------
// ページ切替
// -----------------------------------------------------

const pageSelect = document.getElementById('pageSelect');

if (pageSelect) {

	pageSelect.addEventListener('change', function() {

		currentPage = Number(this.value);

		updateImageList();

	});

}

// -----------------------------------------------------
// 前ページ
// -----------------------------------------------------

const prevButton = document.getElementById('prevPageButton');

if (prevButton) {

	prevButton.addEventListener('click', function() {

		if (currentPage <= 0) {

			return;

		}

		currentPage--;

		updateImageList();

	});

}

// -----------------------------------------------------
// 次ページ
// -----------------------------------------------------

const nextButton = document.getElementById('nextPageButton');

if (nextButton) {

	nextButton.addEventListener('click', function() {

		const filteredImages = getFilteredImages();

		const totalPages =
			Math.ceil(filteredImages.length / imagesPerPage);

		if (currentPage + 1 >= totalPages) {

			return;

		}

		currentPage++;

		updateImageList();

	});

}

// -----------------------------------------------------
// カテゴリー絞り込み後の画像一覧を取得
// -----------------------------------------------------

function getFilteredImages() {

	const category =
		document.getElementById('imageCategorySelect').value;

	if (category === 'all') {

		return allImages;

	}

	return allImages.filter(
		image => image.folderName === category
	);

}

// -----------------------------------------------------
// 画像一覧表示
// -----------------------------------------------------

function updateImageList() {

	const list =
		document.querySelector('.image-list');

	if (!list) {

		return;

	}

	const filteredImages =
		getFilteredImages();

	const totalPages =
		Math.ceil(filteredImages.length / imagesPerPage);

	// 件数が減って現在ページが存在しなくなった場合は末尾へ戻す。
	if (totalPages === 0) {

		currentPage = 0;

	} else if (currentPage >= totalPages) {

		currentPage = totalPages - 1;

	}

	const start =
		currentPage * imagesPerPage;

	const pageImages =
		filteredImages.slice(
			start,
			start + imagesPerPage
		);

	list.innerHTML = '';

	pageImages.forEach(image => {

		const card =
			document.createElement('div');

		card.className = 'image-card';

		card.innerHTML = `
			<img src="${image.url}" class="image-thumbnail">
			<div class="image-path">
				${image.folderName}/${image.fileName}
			</div>
			<div class="image-category">
				${image.categoryName}
			</div>
			<div class="image-date">
				${formatImageDate(image.uploadDate)}
			</div>
			<button type="button"
				class="btn-correct btn-update"
				data-image-id="${image.imageId}"
				data-folder-name="${image.folderName}">
				編集
			</button>
			<button type="button"
				class="btn-delete btn-image-delete"
				data-image-id="${image.imageId}">
				削除
			</button>
		`;

		list.appendChild(card);

	});

	updatePageControls(totalPages);

	// JSで生成したボタンへイベントを設定する。
	bindImageButtons();

}

// -----------------------------------------------------
// 日付表示
// -----------------------------------------------------

function formatImageDate(dateValue) {

	if (!dateValue) {

		return '';

	}

	const date =
		new Date(dateValue);

	if (isNaN(date.getTime())) {

		return '';

	}

	return date.toLocaleDateString(
		undefined,
		{
			year: 'numeric',
			month: '2-digit',
			day: '2-digit'
		}
	);

}

// -----------------------------------------------------
// ページング表示更新
// -----------------------------------------------------

function updatePageControls(totalPages) {

	const pageSelect =
		document.getElementById('pageSelect');

	if (pageSelect) {

		pageSelect.innerHTML = '';

		for (let i = 0; i < totalPages; i++) {

			const option =
				document.createElement('option');

			option.value = i;
			option.textContent = i + 1;
			option.selected = i === currentPage;

			pageSelect.appendChild(option);

		}

	}

	const prevButton =
		document.getElementById('prevPageButton');

	if (prevButton) {

		prevButton.disabled =
			currentPage === 0;

	}

	const nextButton =
		document.getElementById('nextPageButton');

	if (nextButton) {

		nextButton.disabled =
			totalPages === 0 ||
			currentPage + 1 >= totalPages;

	}

	const pageText =
		document.querySelector('.page-area span');

	if (pageText) {

		pageText.textContent =
			` / ${totalPages}ページ`;

	}

}

// -----------------------------------------------------
// 新規画像登録画面
// -----------------------------------------------------
document.getElementById('newImageButton').addEventListener('click', function() {

	location.href = '/image/new';

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
// 編集・削除ボタンのイベント設定
// -----------------------------------------------------

function bindImageButtons() {

	document.querySelectorAll('.btn-update').forEach(btn => {

		btn.addEventListener('click', () => {

			document.getElementById('imageId').value =
				btn.dataset.imageId;

			document.getElementById('imageFolderName').value =
				btn.dataset.folderName ?? '';

			document.getElementById('imageEditModal').style.display =
				'block';

		});

	});

	document.querySelectorAll('.btn-image-delete').forEach(btn => {

		btn.addEventListener('click', () => {

			const imageId =
				btn.dataset.imageId;

			if (!confirm('この画像を削除しますか？')) {

				return;

			}

			const params =
				new URLSearchParams();

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
// 初期表示
// -----------------------------------------------------

loadImageList();

console.log("image_list.js loaded");

