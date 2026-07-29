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
let imageFolderName = null;

// -----------------------------------------------------
// カテゴリーに対応するデフォルトフォルダ名を取得する
// -----------------------------------------------------
function loadDefaultFolderName() {

	const categoryId = document.getElementById('categorySelect').value;
	const folderSelect =
		document.getElementById('imageFolderSelect');

	// 新規カテゴリーはフォルダ名取得不可
	if (!categoryId || categoryId === '__new__') {

		folderSelect.value = '';

		return;
	}

	fetch('/article/images/default-folder?categoryId=' + categoryId)
		.then(res => res.json())
		.then(data => {
			folderSelect.value = data.folderName || '';
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
	if (imageFolderName) {
		url += '&folderName=' + encodeURIComponent(imageFolderName);
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

	// 新規フォルダ追加時にフォルダ選択へ反映する
	loadImageFolders();

	// 画像一覧絞り込み用カテゴリーを更新する
	// 保存先フォルダとは別管理
	loadImageCategories();

	// 新規画像を一覧へ反映する
	loadImageList();

	document.getElementById('imageModal').style.display = 'block';

});

// -----------------------------------------------------
// 画像アップロード
// -----------------------------------------------------
document.getElementById('imageUploadButton').addEventListener('click', function() {

	const fileInput = document.getElementById('imageFileInput');
	let folderName = '';

	const folderSelect =
		document.getElementById('imageFolderSelect');

	if (folderSelect.value === '__new__') {

		folderName =
			document.getElementById('newImageFolderName')
				.value.trim();

	} else {

		folderName = folderSelect.value;

	}

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

				// 新規フォルダ追加時に保存先フォルダへ反映する
				loadImageFolders();

				// 新規フォルダ・画像追加後に一覧絞り込み条件を更新する
				loadImageCategories();

				// 新規画像を一覧へ反映する
				loadImageList();

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

		imageFolderName = this.value || null;
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

const imageFolderSelect =
	document.getElementById('imageFolderSelect');

const newImageFolderName =
	document.getElementById('newImageFolderName');

if (imageFolderSelect) {

	imageFolderSelect.addEventListener('change', function() {

		if (this.value === '__new__') {

			newImageFolderName.style.display = 'block';

		} else {

			newImageFolderName.style.display = 'none';

			newImageFolderName.value = '';

		}

	});

}

// -----------------------------------------------------
// 画像一覧絞り込み用フォルダを再取得する
//
// image_assetに登録されている保存先フォルダ名を
// 画像一覧の絞り込み条件として表示する。
// 保存先フォルダ選択とは別管理。
// -----------------------------------------------------
function loadImageCategories() {

	const select =
		document.getElementById('imageCategorySelect');

	if (!select) {
		return;
	}

	fetch('/image/categories')
		.then(res => res.json())
		.then(categories => {

			select.innerHTML = '';

			const all =
				document.createElement('option');

			all.value = '';
			all.textContent = 'すべて';

			select.appendChild(all);


			categories.forEach(category => {

				const option =
					document.createElement('option');

				option.value =
					category.folderName;

				option.textContent =
					category.folderName;

				select.appendChild(option);

			});

		});

}

// -----------------------------------------------------
// 保存先フォルダ一覧を再取得する
//
// 初回表示時、および新規フォルダ作成後に呼び出す。
// image_assetに存在するフォルダを選択肢として表示する。
// -----------------------------------------------------
function loadImageFolders() {

	const select =
		document.getElementById('imageFolderSelect');

	if (!select) {
		return;
	}

	fetch('/image/folders')
		.then(res => res.json())
		.then(folders => {

			const currentValue = select.value;

			select.innerHTML = '';

			const none =
				document.createElement('option');

			none.value = '';
			none.textContent = '選択してください';

			select.appendChild(none);


			folders.forEach(folder => {

				const option =
					document.createElement('option');

				option.value = folder;
				option.textContent = folder;

				select.appendChild(option);

			});


			const newOption =
				document.createElement('option');

			newOption.value = '__new__';
			newOption.textContent =
				'＋ 新しいフォルダを追加';

			select.appendChild(newOption);


			if (currentValue && currentValue !== '__new__') {
				select.value = currentValue;
			}

		});

}


