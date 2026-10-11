/**
 * インデックス投稿前確認画面の送信処理
 *
 * ・投稿前に確認ダイアログを表示する
 * ・二重送信防止のため送信ボタンを無効化する
 */
function publishIndexSubmit() {
	if (!confirm('この記事を投稿しますか？')) {
		return false;
	}

	const button = document.getElementById('publishButton');

	if (button) {
		button.disabled = true;
		button.textContent = '投稿中...';
	}

	return true;
}

/**
 * サムネイル選択モーダルを開く
 *
 * 既存の画像管理モジュールをサムネイル選択用として使用する。
 */
const thumbnailSelectButton =
	document.getElementById('thumbnailSelectButton');

if (thumbnailSelectButton) {

	thumbnailSelectButton.addEventListener(
		'click',
		async function() {

			const { imageState } =
				await import('/js/image/image_state.js');

			const {
				loadImageFolders,
				loadImageCategories
			} = await import('/js/image/image_folders.js');

			const { loadImageList } =
				await import('/js/image/image_gallery.js');

			imageState.thumbnailMode = true;
			imageState.insertPosition = null;
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

			document.getElementById('imageModal').style.display =
				'block';
		});
}