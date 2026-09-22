// =====================================================
// image_manager.js
//
// 画像管理
// ・画像一覧表示
// ・画像アップロード
// ・カテゴリー別フォルダ取得
// ・Markdownへ画像挿入
// =====================================================

// 画像一覧表示フラグ
let imageInsertPosition = null;
let imagePage = 0;
let imageFolderName = null;
let imageSearchKeyword = '';
let imageSortType = 'dateDesc';

// デフォルトフォルダ名を取得
function loadDefaultFolderName() {

	const categorySelect = document.getElementById('categorySelect');

	if (!categorySelect || !categorySelect.value) {
		return;
	}

	fetch('/article/images/default-folder?categoryId=' + categorySelect.value)
		.then(res => res.json())
		.then(data => {

			const folderSelect =
				document.getElementById('imageFolderSelect');

			if (folderSelect && data.folderName) {
				folderSelect.value = data.folderName;
			}
		})
		.catch(err => {
			console.error(
				'画像デフォルトフォルダの取得に失敗しました',
				err);
		});
}

// 画像一覧を表示する
function loadImageList() {

	const list = document.getElementById('imageList');

	list.innerHTML = '';

	fetch('/article/images')
		.then(res => res.json())
		.then(images => {

			// カテゴリー（フォルダ）で絞り込む
			if (imageFolderName) {

				images = images.filter(
					img => img.folderName === imageFolderName
				);
			}

			// ファイル名で部分一致検索する
			if (imageSearchKeyword) {

				const keyword =
					imageSearchKeyword.toLowerCase();

				images = images.filter(img =>
					img.fileName
					&& img.fileName.toLowerCase().includes(keyword)
				);
			}

			// 指定された条件でソートする
			images.sort((a, b) => {

				switch (imageSortType) {

					case 'dateAsc': {

						const dateA = a.uploadDate
							? new Date(a.uploadDate).getTime()
							: 0;

						const dateB = b.uploadDate
							? new Date(b.uploadDate).getTime()
							: 0;

						return dateA - dateB;
					}

					case 'nameAsc': {

						return (a.fileName || '').localeCompare(
							b.fileName || '',
							'ja'
						);
					}

					case 'nameDesc': {

						return (b.fileName || '').localeCompare(
							a.fileName || '',
							'ja'
						);
					}

					case 'dateDesc':
					default: {

						const dateA = a.uploadDate
							? new Date(a.uploadDate).getTime()
							: 0;

						const dateB = b.uploadDate
							? new Date(b.uploadDate).getTime()
							: 0;

						return dateB - dateA;
					}
				}
			});

			const imagesPerPage = 12;
			const totalPages =
				Math.ceil(images.length / imagesPerPage);

			if (totalPages === 0) {

				imagePage = 0;

			} else if (imagePage >= totalPages) {

				imagePage = totalPages - 1;
			}

			const start = imagePage * imagesPerPage;

			const pageImages =
				images.slice(start, start + imagesPerPage);

			pageImages.forEach(img => {

				const div = document.createElement('div');

				div.style.cursor = 'pointer';
				div.style.textAlign = 'center';

				div.innerHTML = `
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

			// ページ選択肢を作り直す
			const pageSelect =
				document.getElementById('imagePageSelect');

			pageSelect.innerHTML = '';

			for (let i = 0; i < totalPages; i++) {

				const option =
					document.createElement('option');

				option.value = i;
				option.textContent = i + 1;

				if (i === imagePage) {
					option.selected = true;
				}

				pageSelect.appendChild(option);
			}

			pageSelect.disabled = totalPages === 0;

			document.getElementById('imagePageInfo').textContent =
				totalPages === 0
					? '0ページ'
					: '/ ' + totalPages + 'ページ';

			document.getElementById('imagePrevButton').disabled =
				imagePage === 0;

			document.getElementById('imageNextButton').disabled =
				totalPages === 0
				|| imagePage + 1 >= totalPages;
		})
		.catch(err => {

			console.error(
				'画像一覧の取得に失敗しました',
				err);
		});
}

// 画像をMarkdownへ挿入する
function insertImage(url) {

	const textarea =
		document.querySelector('textarea[name="content"]');

	const widthInput =
		document.getElementById('imageWidth');

	const width =
		widthInput && widthInput.value
			? widthInput.value
			: '';

	let imageTag;

	if (width) {

		imageTag =
			'\n<img src="' + url
			+ '" style="max-width:' + width
			+ 'px; width:100%;">\n';

	} else {

		imageTag =
			'\n<img src="' + url + '">\n';
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

// 画像選択モーダルを開く
document.getElementById('imageButton').addEventListener(
	'click',
	function() {

		const textarea =
			document.querySelector('textarea[name="content"]');

		textarea.focus();

		imageInsertPosition =
			textarea.selectionStart;

		imageFolderName = null;
		imageSearchKeyword = '';
		imageSortType = 'dateDesc';
		imagePage = 0;

		const searchInput =
			document.getElementById('imageSearchInput');

		if (searchInput) {
			searchInput.value = '';
		}

		const sortSelect =
			document.getElementById('imageSortSelect');

		if (sortSelect) {
			sortSelect.value = 'dateDesc';
		}

		loadImageFolders();
		loadImageCategories();
		loadImageList();

		document.getElementById('imageModal').style.display = 'block';
	}
);

// 画像アップロード
document.getElementById('imageUploadButton').addEventListener(
	'click',
	function() {

		const fileInput =
			document.getElementById('imageFileInput');

		let folderName = '';

		const folderSelect =
			document.getElementById('imageFolderSelect');

		const newFolderInput =
			document.getElementById('newImageFolderName');

		if (newFolderInput.value.trim()) {

			folderName =
				newFolderInput.value.trim();

		} else {

			folderName =
				folderSelect.value;
		}

		const categoryId =
			document.getElementById('categorySelect').value;

		const status =
			document.getElementById('imageUploadStatus');

		if (!fileInput.files.length) {

			alert('画像ファイルを選択してください');
			return;
		}

		const formData = new FormData();

		formData.append(
			'file',
			fileInput.files[0]);

		if (categoryId && categoryId !== '__new__') {

			formData.append(
				'categoryId',
				categoryId);
		}

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

					status.textContent =
						'アップロードしました';

					fileInput.value = '';

					const currentFolderName =
						imageFolderName;

					loadImageFolders();
					loadImageCategories();

					imageFolderName =
						currentFolderName;

					loadImageList();

				} else {

					status.textContent =
						data.message
						|| 'アップロードに失敗しました';
				}
			})
			.catch(err => {

				console.error(err);

				status.textContent =
					'アップロードに失敗しました';
			});
	}
);

// カテゴリー filter change
document.getElementById('imageCategorySelect')
	.addEventListener(
		'change',
		function() {

			imageFolderName =
				this.value || null;

			imagePage = 0;

			loadImageList();
		}
	);

// ファイル名検索
document.getElementById('imageSearchInput')
	.addEventListener(
		'input',
		function() {

			imageSearchKeyword =
				this.value.trim();

			// 検索条件が変わったため1ページ目から表示する。
			imagePage = 0;

			loadImageList();
		}
	);

// ソート変更
document.getElementById('imageSortSelect')
	.addEventListener(
		'change',
		function() {

			imageSortType =
				this.value;

			// ソート条件が変わったため1ページ目から表示する。
			imagePage = 0;

			loadImageList();
		}
	);

const newImageButton =
	document.getElementById('newImageButton');

if (newImageButton) {

	newImageButton.addEventListener(
		'click',
		function() {

			location.href =
				'/article/images/new';
		});
}

// 前のページ
document.getElementById('imagePrevButton')
	.addEventListener(
		'click',
		function() {

			if (imagePage > 0) {

				imagePage--;

				loadImageList();
			}
		});

// ページ選択
document.getElementById('imagePageSelect')
	.addEventListener(
		'change',
		function() {

			imagePage =
				Number(this.value);

			loadImageList();
		});

const imageNextButton =
	document.getElementById('imageNextButton');

if (imageNextButton) {

	imageNextButton.addEventListener(
		'click',
		function() {

			imagePage++;

			loadImageList();
		});
}

const imageFolderSelect =
	document.getElementById('imageFolderSelect');

const newImageFolderName =
	document.getElementById('newImageFolderName');

if (imageFolderSelect) {

	imageFolderSelect.addEventListener(
		'change',
		function() {

			if (this.value === '__new__') {

				newImageFolderName.style.display =
					'block';

			} else {

				newImageFolderName.style.display =
					'none';

				newImageFolderName.value = '';
			}
		});
}

// 画像カテゴリー一覧を取得
function loadImageCategories() {

	const select =
		document.getElementById('imageCategorySelect');

	if (!select) {
		return;
	}

	fetch('/image/categories')
		.then(res => res.json())
		.then(categories => {

			const currentValue =
				imageFolderName;

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

			if (currentValue) {

				select.value =
					currentValue;
			}
		});
}

// 画像保存先フォルダ一覧を取得
function loadImageFolders() {

	const select =
		document.getElementById('imageFolderSelect');

	if (!select) {
		return;
	}

	fetch('/image/folders')
		.then(res => res.json())
		.then(folders => {

			const currentValue =
				select.value;

			select.innerHTML = '';

			const none =
				document.createElement('option');

			none.value = '';
			none.textContent =
				'選択してください';

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

			if (currentValue
				&& currentValue !== '__new__') {

				select.value =
					currentValue;
			}
		});
}