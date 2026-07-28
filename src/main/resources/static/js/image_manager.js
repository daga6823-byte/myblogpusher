// =====================================================
// image_manager.js
//
// 画像管理
// ・画像一覧表示
// ・画像アップロード
// ・カテゴリー別フォルダ取得
// ・Markdownへ画像挿入
// =====================================================

// -----------------------------------------------------
// 画像一覧表示フラグ
// false：現在のカテゴリーのみ
// true ：全カテゴリー
// -----------------------------------------------------
// 画像挿入位置
let imageInsertPosition = null;

// ページ番号
let imagePage = 0;

// 選択中カテゴリー
let imageCategoryId = null;

// -----------------------------------------------------
// カテゴリーに対応するデフォルトフォルダ名を取得する
// -----------------------------------------------------
function loadDefaultFolderName() {

	const categoryId = document.getElementById('categorySelect').value;
	const folderInput = document.getElementById('imageFolderName');

	// 新規カテゴリーはフォルダ名取得不可
	if (!categoryId || categoryId === '__new__') {
		folderInput.value = '';
		return;
	}

	fetch('/article/images/default-folder?categoryId=' + categoryId)
		.then(res => res.json())
		.then(data => {
			folderInput.value = data.folderName || '';
		})
		.catch(err => console.error(err));

}

// -----------------------------------------------------
// 画像一覧を取得してモーダルへ表示する
//
// imageShowAll=false
//     現在カテゴリーのみ
//
// imageShowAll=true
//     全カテゴリー
// -----------------------------------------------------
function loadImageList() {

	const list = document.getElementById('imageList');

	list.innerHTML = '';

	let url =
		'/article/images?page=' + imagePage;

	// カテゴリー選択時だけ絞り込む
	if (imageCategoryId) {
		url += '&categoryId=' + imageCategoryId;
	}

	fetch(url)
		.then(res => res.json())
		.then(page => {

			const images = page.content;

			images.forEach(img => {

				const div = document.createElement('div');

				div.style.cursor = 'pointer';
				div.style.textAlign = 'center';

				div.innerHTML =
					`
					<img src="${img.url}"
						 style="width:100%;height:150px;object-fit:cover;"
						 onclick="insertImage('${img.url}')">

					<div style="
						margin-top:5px;
						font-size:13px;
						word-break:break-all;
					">
						${img.fileName}
					</div>
					`;

				list.appendChild(div);

			});


			document.getElementById('imagePageInfo').textContent =
				(page.number + 1) + ' / ' + page.totalPages;


			document.getElementById('imagePrevButton').disabled =
				page.first;


			document.getElementById('imageNextButton').disabled =
				page.last;

		});

}

// -----------------------------------------------------
// 画像を本文へ挿入する
//
// 挿入時に指定したサイズをHTML styleへ反映する
// -----------------------------------------------------
function insertImage(url) {

	const textarea =
		document.querySelector('textarea[name="content"]');

	const widthInput = document.getElementById('imageWidth');

	const width =
		widthInput && widthInput.value
			? widthInput.value
			: '';

	let imageTag;

	if (width) {

		imageTag =
			'\n<img src="' + url +
			'" style="max-width:' + width +
			'px; width:100%;">\n';

	} else {

		imageTag =
			'\n<img src="' + url +
			'" style="width:100%;">\n';
	}


	if (imageInsertPosition !== null) {

		textarea.value =
			textarea.value.substring(0, imageInsertPosition)
			+ imageTag
			+ textarea.value.substring(imageInsertPosition);

		textarea.selectionStart =
			textarea.selectionEnd =
			imageInsertPosition + imageTag.length;

	} else {

		textarea.value += imageTag;

	}

	textarea.focus();

	imageInsertPosition = null;

	document.getElementById('imageModal').style.display = 'none';
}

// -----------------------------------------------------
// 画像モーダル表示
// -----------------------------------------------------
document.getElementById('imageButton').addEventListener('click', function() {

	const textarea = document.querySelector('textarea[name="content"]');

	textarea.focus();

	imageInsertPosition = textarea.selectionStart;

	imageCategoryId = null;
	imagePage = 0;

	loadDefaultFolderName();
	loadImageList();

	document.getElementById('imageModal').style.display = 'block';

});

// -----------------------------------------------------
// 画像アップロード
// -----------------------------------------------------
document.getElementById('imageUploadButton').addEventListener('click', function() {

	const fileInput = document.getElementById('imageFileInput');
	const folderName = document.getElementById('imageFolderName').value.trim();
	const categoryId = document.getElementById('categorySelect').value;
	const workId = new URLSearchParams(window.location.search).get('workId');
	const status = document.getElementById('imageUploadStatus');

	if (!fileInput.files.length) {
		alert('画像ファイルを選択してください');
		return;
	}

	const formData = new FormData();

	formData.append('file', fileInput.files[0]);

	if (categoryId && categoryId !== '__new__') {
		formData.append('categoryId', categoryId);
	}

	if (folderName) {
		formData.append('folderName', folderName);
	}

	if (workId) {
		formData.append('workId', workId);
	}

	status.textContent = 'アップロード中...';

	fetch('/article/images/upload', {
		method: 'POST',
		body: formData
	})
		.then(res => res.json())
		.then(data => {

			if (data.result === 'ok') {

				status.textContent = 'アップロードしました';

				fileInput.value = '';

				loadImageList();

			} else {

				status.textContent =
					data.message || 'アップロードに失敗しました';

			}

		})
		.catch(err => {

			console.error(err);

			status.textContent = 'アップロードに失敗しました';

		});

});

// カテゴリー変更時
document.getElementById('imageCategorySelect')
	.addEventListener('change', function() {

		imageCategoryId = this.value || null;
		imagePage = 0;

		loadImageList();

	});


// 新規画像登録
const newImageButton = document.getElementById('newImageButton');

if (newImageButton) {

	newImageButton.addEventListener('click', function() {

		location.href =
			'/article/images/new';

	});

}

document.getElementById('imagePrevButton')
	.addEventListener('click', function() {

		if (imagePage > 0) {
			imagePage--;
			loadImageList();
		}

	});


const imageNextButton = document.getElementById('imageNextButton');

if (imageNextButton) {

	imageNextButton.addEventListener('click', function() {

		imagePage++;
		loadImageList();

	});

}
