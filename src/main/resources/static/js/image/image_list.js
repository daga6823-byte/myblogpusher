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
const imagesPerPage = 27;

// -----------------------------------------------------
// 画像一覧取得
// -----------------------------------------------------

function loadImageList() {
	fetch('/article/images/all')
		.then(response => response.json())
		.then(images => {
			allImages = images;
			updateImageList();
		})
		.catch(error => {
			console.error('画像一覧の取得に失敗しました:', error);
		});
}

// -----------------------------------------------------
// 画像一覧表示
// -----------------------------------------------------

function updateImageList() {
	const list = document.querySelector('.image-list');
	if (!list) return;

	const filteredImages = getFilteredImages();

	const sortSelect =
		document.getElementById('imageSortSelect');

	const sortType =
		sortSelect ? sortSelect.value : 'nameAsc';

	filteredImages.sort((a, b) => {

		if (sortType === 'nameAsc') {

			return a.fileName.localeCompare(
				b.fileName,
				'ja'
			);

		}

		if (sortType === 'nameDesc') {

			return b.fileName.localeCompare(
				a.fileName,
				'ja'
			);

		}

		const dateA =
			new Date(a.uploadDate).getTime();

		const dateB =
			new Date(b.uploadDate).getTime();

		if (sortType === 'dateAsc') {

			return dateA - dateB;

		}

		return dateB - dateA;

	});

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

document.getElementById('imageSortSelect')
	.addEventListener('change', function() {

		currentPage = 0;

		updateImageList();

	});

loadImageList();

console.log('image_list.js loaded');

// -----------------------------------------------------
// フォルダ名変更
// -----------------------------------------------------

const imageCategorySelect =
	document.getElementById('imageCategorySelect');

const renameFolderButton =
	document.getElementById('renameFolderButton');

function updateRenameFolderButton() {
	renameFolderButton.disabled =
		!imageCategorySelect.value ||
		imageCategorySelect.value === 'all';
}

imageCategorySelect.addEventListener(
	'change',
	updateRenameFolderButton
);

renameFolderButton.addEventListener('click', async function() {
	const oldFolderName = imageCategorySelect.value;

	if (!oldFolderName || oldFolderName === 'all') {
		return;
	}

	const newFolderName = prompt(
		'変更後のフォルダ名を入力してください。',
		oldFolderName
	);

	if (newFolderName === null) {
		return;
	}

	const trimmedFolderName = newFolderName.trim();

	if (!trimmedFolderName || trimmedFolderName === oldFolderName) {
		return;
	}

	if (trimmedFolderName.includes('/')) {
		alert('フォルダ名に / は使用できません。');
		return;
	}

	if (!confirm(
		`フォルダ「${oldFolderName}」を「${trimmedFolderName}」に変更しますか？`
	)) {
		return;
	}

	renameFolderButton.disabled = true;

	try {
		const response = await fetch('/image/folder/rename', {
			method: 'POST',
			headers: {
				'Content-Type': 'application/x-www-form-urlencoded'
			},
			body: new URLSearchParams({
				oldFolderName: oldFolderName,
				newFolderName: trimmedFolderName
			})
		});

		const result = await response.json();

		if (!response.ok || result.result !== 'ok') {
			throw new Error(
				result.message || 'フォルダ名の変更に失敗しました。'
			);
		}

		alert('フォルダ名を変更しました。');
		window.location.assign(
			'/image/list?folderName=' +
			encodeURIComponent(trimmedFolderName)
		);

	} catch (error) {
		alert(error.message || 'フォルダ名の変更に失敗しました。');
	} finally {
		updateRenameFolderButton();
	}
});

updateRenameFolderButton();
