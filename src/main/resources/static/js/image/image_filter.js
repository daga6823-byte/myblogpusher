// =====================================================
// image_filter.js
//
// 登録済み画像一覧のカテゴリー絞り込みを担当する。
// =====================================================

// -----------------------------------------------------
// カテゴリー絞り込み後の画像一覧を取得
// -----------------------------------------------------

function getFilteredImages() {

	const category =
		document.getElementById('imageCategorySelect').value;

	const searchInput =
		document.getElementById('imageSearchInput');

	const keyword =
		searchInput ? searchInput.value.trim().toLowerCase() : '';

	return allImages.filter(image => {

		// カテゴリー条件
		if (
			category !== 'all'
			&& image.folderName !== category
		) {
			return false;
		}

		// ファイル名条件
		if (
			keyword
			&& (
				!image.fileName
				|| !image.fileName.toLowerCase().includes(keyword)
			)
		) {
			return false;
		}

		return true;
	});
}

// -----------------------------------------------------
// カテゴリー変更
// -----------------------------------------------------

document.getElementById('imageCategorySelect')
	.addEventListener('change', function() {

		// カテゴリー変更時は先頭ページへ戻す。
		currentPage = 0;

		updateImageList();
	});

// -----------------------------------------------------
// ファイル名検索
// -----------------------------------------------------

document.getElementById('imageSearchInput')
	.addEventListener('input', function() {

		// 検索条件変更時は先頭ページへ戻す。
		currentPage = 0;

		updateImageList();

	});
