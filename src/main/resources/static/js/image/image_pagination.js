// =====================================================
// image_pagination.js
//
// 登録済み画像一覧のページ切り替えを担当する。
// =====================================================

// -----------------------------------------------------
// ページ選択
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

const prevButton =
	document.getElementById('prevPageButton');

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

const nextButton =
	document.getElementById('nextPageButton');

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
		prevButton.disabled = currentPage === 0;
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