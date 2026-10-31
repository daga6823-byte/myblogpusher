/**
 * image_gallery.js
 *
 * 画像一覧の表示・検索・並び替え・ページング、
 * 画像選択モーダルを開く処理、Markdownへの画像挿入を担当する。
 */

import { imageState } from './image_state.js';
import { loadImageFolders, loadImageCategories } from './image_folders.js';

// 画像一覧を表示する
export function loadImageList() {

	const list = document.getElementById('imageList');

	const requestId = ++imageState.loadRequestId;

	const params = new URLSearchParams();

	if (imageState.folderName) {
		params.set('folderName', imageState.folderName);
	}

	if (imageState.searchKeyword) {
		params.set('keyword', imageState.searchKeyword);
	}

	const query = params.toString();

	fetch('/article/images' + (query ? '?' + query : ''))
		.then(res => res.json())
		.then(images => {

			// 古いリクエストの結果は表示しない。
			if (requestId !== imageState.loadRequestId) {
				return;
			}

			list.innerHTML = '';

			// カテゴリー（フォルダ）で絞り込む
			if (imageState.folderName) {

				images = images.filter(
					img => img.folderName === imageState.folderName
				);
			}

			// 指定された条件でソートする
			images.sort((a, b) => {

				switch (imageState.sortType) {

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

				imageState.page = 0;

			} else if (imageState.page >= totalPages) {

				imageState.page = totalPages - 1;
			}

			const start = imageState.page * imagesPerPage;

			const pageImages =
				images.slice(start, start + imagesPerPage);

			pageImages.forEach(img => {

				const div = document.createElement('div');

				div.style.cursor = 'pointer';

				div.style.textAlign = 'center';

				const imageElement = document.createElement('img');
				imageElement.src = img.url;
				imageElement.style.cssText =
					'width:100%;height:150px;object-fit:cover;';

				const fileNameElement = document.createElement('div');
				fileNameElement.style.cssText =
					'margin-top:5px;font-size:13px;word-break:break-all;';
				fileNameElement.textContent = img.fileName || '';

				div.appendChild(imageElement);
				div.appendChild(fileNameElement);

				imageElement.addEventListener('click', async function() {
					if (imageState.thumbnailMode) {
						const thumbnailPreview =
							document.getElementById('thumbnailPreview');
						const thumbnailUrl =
							document.getElementById('thumbnailUrl');
						const workIdInput =
							document.querySelector(
								'#publishForm input[name="workId"]'
							);

						if (!workIdInput || !workIdInput.value) {
							alert('記事のワークIDを取得できません。');
							return;
						}

						try {
							const response = await fetch(
								'/publish/preview/thumbnail',
								{
									method: 'POST',
									headers: {
										'Content-Type':
											'application/x-www-form-urlencoded'
									},
									body: new URLSearchParams({
										workId: workIdInput.value,
										thumbnailUrl: img.url
									})
								}
							);

							if (!response.ok) {
								throw new Error('サムネイル保存に失敗しました。');
							}

							if (thumbnailPreview) {
								thumbnailPreview.src = img.url;
								thumbnailPreview.style.display = 'block';
							}

							if (thumbnailUrl) {
								thumbnailUrl.value = img.url;
							}

							imageState.thumbnailMode = false;
							document.getElementById('imageModal').style.display = 'none';
						} catch (error) {
							console.error(error);
							alert('サムネイルを保存できませんでした。');
						}

						return;
					}

					insertImage(img.url);
				});

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

				if (i === imageState.page) {
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
				imageState.page === 0;

			document.getElementById('imageNextButton').disabled =
				totalPages === 0
				|| imageState.page + 1 >= totalPages;
		})
		.catch(err => {

			console.error(
				'画像一覧の取得に失敗しました',
				err);
		});
}

// 画像をMarkdownへ挿入する
export function insertImage(url) {

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

	if (imageState.insertPosition !== null) {

		textarea.value =
			textarea.value.substring(0, imageState.insertPosition)
			+ imageTag
			+ textarea.value.substring(imageState.insertPosition);

		textarea.selectionStart =
			textarea.selectionEnd =
			imageState.insertPosition + imageTag.length;

	} else {

		textarea.value += imageTag;
	}

	textarea.focus();

	imageState.insertPosition = null;

	document.getElementById('imageModal').style.display = 'none';
}

// 画像選択モーダルを開く
const imageButton =
	document.getElementById('imageButton');

if (imageButton) {

	imageButton.addEventListener(
		'click',
		function() {

			const textarea =
				document.querySelector('textarea[name="content"]');

			textarea.focus();

			imageState.insertPosition =
				textarea.selectionStart;

			imageState.folderName = null;
			imageState.searchKeyword = '';
			imageState.sortType = 'dateDesc';
			imageState.page = 0;

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
}

// カテゴリー filter change
document.getElementById('imageCategorySelect')
	.addEventListener(
		'change',
		function() {

			imageState.folderName =
				this.value || null;

			imageState.page = 0;

			loadImageList();
		}
	);

// ファイル名検索
document.getElementById('imageSearchInput')
	.addEventListener(
		'input',
		function() {

			imageState.searchKeyword =
				this.value.trim();

			// 検索条件が変わったため1ページ目から表示する。
			imageState.page = 0;

			loadImageList();
		}
	);

// ソート変更
document.getElementById('imageSortSelect')
	.addEventListener(
		'change',
		function() {

			imageState.sortType =
				this.value;

			// ソート条件が変わったため1ページ目から表示する。
			imageState.page = 0;

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

			if (imageState.page > 0) {

				imageState.page--;

				loadImageList();
			}
		});

// ページ選択
document.getElementById('imagePageSelect')
	.addEventListener(
		'change',
		function() {

			imageState.page =
				Number(this.value);

			loadImageList();
		});

const imageNextButton =
	document.getElementById('imageNextButton');

if (imageNextButton) {

	imageNextButton.addEventListener(
		'click',
		function() {

			imageState.page++;

			loadImageList();
		});
}