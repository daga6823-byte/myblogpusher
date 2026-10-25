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

	const slugInput = document.getElementById('slugInput');
	const slugHidden = document.getElementById('slugHidden');

	if (slugInput && slugHidden) {
		slugHidden.value = slugInput.value;
	}

	const form = document.getElementById("publishForm");
	const workIdInput = form.querySelector('input[name="workId"]');
	const slug = slugHidden.value;
	const workId = workIdInput.value;

	// 投稿状況の確認用タブを先に開き、ポップアップブロックを回避する
	const waitTab = window.open("about:blank", "_blank");

	if (waitTab) {
		const params = new URLSearchParams({ workId, slug });
		waitTab.location.href = `/publish/wait?${params.toString()}`;
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

/**
 * 編集画面へ戻る前に、確認画面で変更したslugを保存する
 */
const publishBackButton =
	document.getElementById('publishBackButton');

if (publishBackButton) {
	publishBackButton.addEventListener('click', async function() {
		const workIdInput =
			document.querySelector(
				'#publishForm input[name="workId"]'
			);
		const slugInput = document.getElementById('slugInput');

		if (!workIdInput || !workIdInput.value || !slugInput) {
			alert('記事情報を取得できません。');
			return;
		}

		try {
			const response = await fetch('/publish/preview/slug', {
				method: 'POST',
				headers: {
					'Content-Type':
						'application/x-www-form-urlencoded'
				},
				body: new URLSearchParams({
					workId: workIdInput.value,
					slug: slugInput.value
				})
			});

			if (!response.ok) {
				throw new Error('スラッグ保存に失敗しました。');
			}

			location.href = '/article/edit?workId='
				+ encodeURIComponent(workIdInput.value);
		} catch (error) {
			console.error(error);
			alert('スラッグを保存できませんでした。');
		}
	});
}


/**
 * 投稿前確認画面の本文画像プレビュー
 *
 * Markdownの画像記法とHTMLのimgタグを画像として表示する。
 * imgタグのsrcがMarkdownリンクで囲まれている場合もURLを取り出す。
 * コードブロック内の記法は変換しない。
 */
function renderArticleImagePreview() {
	const preview = document.getElementById('articleContentPreview');

	if (!preview) {
		return;
	}

	const content = preview.dataset.content || '';
	const fragment = document.createDocumentFragment();
	const lines = content.split('\n');

	let inCodeBlock = false;

	/**
	 * 画像要素を生成する。
	 */
	function appendImage(url, alt = '') {
		const image = document.createElement('img');
		image.src = url;
		image.alt = alt;
		image.style.maxWidth = '100%';
		image.style.height = 'auto';

		fragment.appendChild(image);
	}

	/**
	 * 本文中の画像記法を画像要素へ変換する。
	 */
	function appendLine(line) {
		const pattern =
			/\\?<img\b[^>]*>|!\[([^\]]*)\]\((https?:\/\/[^)\s]+)\)/gi;

		let lastIndex = 0;
		let match;

		while ((match = pattern.exec(line)) !== null) {
			fragment.appendChild(
				document.createTextNode(line.slice(lastIndex, match.index))
			);

			if (match[0].toLowerCase().includes('<img')) {
				const srcMatch = match[0].match(
					/\bsrc\s*=\s*["']([^"']+)["']/i
				);

				if (srcMatch) {
					let url = srcMatch[1];

					// src="[URL](URL)" 形式から実際のURLを取り出す
					const markdownLink = url.match(
						/^\[(https?:\/\/.+?)\]\((https?:\/\/.+?)\)$/
					);

					if (markdownLink) {
						url = markdownLink[2];
					}

					if (/^https?:\/\/\S+$/.test(url)) {
						appendImage(url);
					} else {
						fragment.appendChild(
							document.createTextNode(match[0])
						);
					}
				} else {
					fragment.appendChild(
						document.createTextNode(match[0])
					);
				}
			} else {
				appendImage(match[2], match[1]);
			}

			lastIndex = pattern.lastIndex;
		}

		fragment.appendChild(
			document.createTextNode(line.slice(lastIndex))
		);
	}

	for (const line of lines) {
		if (/^\s*\\?```/.test(line)) {
			inCodeBlock = !inCodeBlock;
			fragment.appendChild(document.createTextNode(line));
			fragment.appendChild(document.createElement('br'));
			continue;
		}

		if (inCodeBlock) {
			fragment.appendChild(document.createTextNode(line));
		} else {
			appendLine(line);
		}

		fragment.appendChild(document.createElement('br'));
	}

	preview.replaceChildren(fragment);
}

renderArticleImagePreview();

