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

	if (category === 'all') {
		return allImages;
	}

	return allImages.filter(
		image => image.folderName === category
	);
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