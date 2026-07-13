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
let imageShowAll = false;

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

	const categoryId = document.getElementById('categorySelect').value;
	const list = document.getElementById('imageList');

	list.innerHTML = '';

	const useCategory =
		!imageShowAll &&
		categoryId &&
		categoryId !== '__new__';

	const query =
		useCategory
			? '?categoryId=' + categoryId
			: '';

	fetch('/article/images' + query)
		.then(res => res.json())
		.then(images => {

			images.forEach(img => {

				const div = document.createElement('div');

				div.style.cursor = 'pointer';

				div.innerHTML =
					`<img src="${img}"
						  style="width:100%;height:150px;object-fit:cover;"
						  onclick="insertImage('${img}')">`;

				list.appendChild(div);

			});

		});

}

// -----------------------------------------------------
// 画像をMarkdownへ挿入する
// -----------------------------------------------------
function insertImage(url) {

	const textarea =
		document.querySelector('textarea[name="content"]');

	textarea.value += '\n![image](' + url + ')\n';

	document.getElementById('imageModal').style.display = 'none';

}

// -----------------------------------------------------
// 画像モーダル表示
// -----------------------------------------------------
document.getElementById('imageButton').addEventListener('click', function() {

	imageShowAll = false;

	document.getElementById('imageScopeToggle').textContent =
		'全カテゴリー表示に切り替え';

	loadDefaultFolderName();
	loadImageList();

	document.getElementById('imageModal').style.display = 'block';

});

// -----------------------------------------------------
// 表示範囲切替
// -----------------------------------------------------
document.getElementById('imageScopeToggle').addEventListener('click', function() {

	imageShowAll = !imageShowAll;

	this.textContent =
		imageShowAll
			? '現在のカテゴリーのみ表示に切り替え'
			: '全カテゴリー表示に切り替え';

	loadImageList();

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

	if (!categoryId || categoryId === '__new__') {
		alert('先にカテゴリーを選択してください');
		return;
	}

	const formData = new FormData();

	formData.append('file', fileInput.files[0]);
	formData.append('categoryId', categoryId);

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