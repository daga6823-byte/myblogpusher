/**
 * 辞書登録処理
 *
 * 形態素解析結果の単語を英語辞書へ登録する。
 * 登録前に入力値を確認し、成功後は画面を再読み込みする。
 */
function registerWord(btn) {

	const reading = btn.getAttribute('data-reading');
	const converted = btn.getAttribute('data-converted');

	const english = prompt('英単語を入力してください', converted);

	if (!english) {
		return;
	}

	fetch('/dictionary/register', {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json'
		},
		body: JSON.stringify({
			japanese: reading,
			english: english
		})
	})
		.then(res => {

			if (res.ok) {

				alert('登録しました');

				// 登録結果を反映するため画面を再表示
				location.reload();
			}

		});
}


/**
 * 投稿送信前処理
 *
 * ・投稿確認ダイアログを表示する
 * ・二重送信防止のため投稿ボタンを無効化する
 * ・画面で編集したslugをhidden項目へ反映する
 */
function publishSubmit() {

	if (!confirm("この記事を投稿しますか？")) {
		return false;
	}

	// 画面上で編集されたslugを送信用hiddenへ反映
	const slugInput = document.getElementById('slugInput');
	const slugHidden = document.getElementById('slugHidden');

	if (slugInput && slugHidden) {
		slugHidden.value = slugInput.value;
	}

	const button = document.getElementById("publishButton");

	if (button) {

		button.disabled = true;
		button.textContent = "投稿中...";
	}

	return true;
}


/**
 * 辞書登録用hidden値同期
 *
 * 表示用の英語変換結果(input)を、
 * 辞書登録フォーム送信用hiddenへ反映する。
 */
function syncEnglish(btn) {

	const reading = btn.getAttribute('data-reading');

	const converted =
		document.getElementById('converted_' + reading).value;

	document.getElementById('hidden_' + reading).value =
		converted;
}

/**
 * 形態素解析結果変更監視
 *
 * 変換結果を変更できる画面でのみslugを自動更新する。
 */
document.querySelectorAll('[id^="converted_"]')
	.forEach(input => {

		input.addEventListener('input', () => {

			if (typeof updateSlug === 'function') {
				updateSlug();
			}

		});

	});

/**
 * サムネイル選択モーダルを開く
 *
 * 既存の画像一覧をサムネイル選択用として使用する。
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



