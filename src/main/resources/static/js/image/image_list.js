// =====================================================
// image_list.js
//
// 登録済み画像一覧の全体制御
// ・画像一覧取得
// ・画像カード表示
// ・各機能の初期化
// =====================================================

// -----------------------------------------------------
// 画像一覧の状態
// -----------------------------------------------------

let allImages = [];
let currentPage = 0;

// 一覧画面で1ページに表示する画像数。
// 表示件数の仕様は既存設定を維持する。
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

// -----------------------------------------------------
// 画像一覧表示
// -----------------------------------------------------

function updateImageList() {
	const list = document.querySelector('.image-list');
	if (!list) return;

	const filteredImages = getFilteredImages();
	const totalPages =
		Math.ceil(filteredImages.length / imagesPerPage);

	// 件数が減って現在ページが存在しなくなった場合は末尾へ戻す。
	if (totalPages === 0) {
		currentPage = 0;
	} else if (currentPage >= totalPages) {
		currentPage = totalPages - 1;
	}

	const start = currentPage * imagesPerPage;
	const pageImages =
		filteredImages.slice(start, start + imagesPerPage);

	list.innerHTML = '';

	pageImages.forEach(image => {
		const card = document.createElement('div');
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
				data-folder-name="${image.folderName}"
				data-file-name="${image.fileName}">
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
	if (!dateValue) return '';

	const date = new Date(dateValue);

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
// 初期表示
// -----------------------------------------------------

loadImageList();

console.log('image_list.js loaded');