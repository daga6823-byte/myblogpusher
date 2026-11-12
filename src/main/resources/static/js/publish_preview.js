/**
 * 英単語辞典への登録とスラッグの再生成を担当するJavaScript
 *
 * 形態素解析結果の単語を英語辞書へ登録し、
 * 登録成功後に辞書情報を使用して記事タイトルのスラッグを再生成する。
 *
 * 辞書登録とスラッグ再生成はPOST通信で行う。
 * 再生成したスラッグは画面上の入力欄と投稿フォームのhidden項目へ反映する。
 *
 * 投稿前確認画面の再読み込みやGETによる再取得は行わず、
 * 現在の画面を維持する。
 */

/**
 * CSRFトークンをCookieから取得する。
 *
 * XSRF-TOKEN Cookieの値を取得し、URLデコードして返す。
 * Cookieが存在しない場合はnullを返す。
 */
function getCsrfToken() {
	const cookie = document.cookie
		.split('; ')
		.find(row => row.startsWith('XSRF-TOKEN='));

	return cookie
		? decodeURIComponent(cookie.substring('XSRF-TOKEN='.length))
		: null;
}

function registerWord(btn) {
	const form = btn.closest('.dictionary-register-form');

	if (!form) {
		alert('辞書登録フォームを取得できません。');
		return;
	}

	// 表示中の変換結果をフォームに反映する
	syncEnglish(btn);

	const formData = new URLSearchParams(new FormData(form));

	fetch(form.action, {
		method: 'POST',
		headers: {
			'Content-Type': 'application/x-www-form-urlencoded',
			'X-Requested-With': 'XMLHttpRequest',
			'X-CSRF-TOKEN': getCsrfToken()
		},
		body: formData,
	})
		.then(response => {
			if (!response.ok) {
				throw new Error('辞書登録に失敗しました。');
			}

			// 登録した辞書を使ってスラッグを再生成する
			const workId = document.querySelector(
				'input[name="workId"]'
			).value;
			const title = document.querySelector(
				'input[name="title"]'
			).value;

			const params = new URLSearchParams({
				workId: workId,
				title: title
			});

			return fetch('/publish/generate-slug', {
				method: 'POST',
				headers: {
					'Content-Type': 'application/x-www-form-urlencoded',
					'X-CSRF-TOKEN': getCsrfToken()
				},
				body: params
			});
		})
		.then(response => {
			if (!response.ok) {
				throw new Error('スラッグの再生成に失敗しました。');
			}
			return response.text().then(text => {
				try {
					return JSON.parse(text);
				} catch (error) {
					console.error('スラッグ再生成APIのレスポンス:', text);
					throw new Error('スラッグ再生成APIからJSON以外の応答が返されました。');
				}
			});
		})
		.then(data => {
			document.getElementById('slugInput').value = data.slug;
			document.getElementById('slugHidden').value = data.slug;
			alert('辞書を登録しました。');
		})
		.catch(error => {
			console.error(error);
			alert(error.message);
		});
}

/**
 * 投稿送信前処理
 *
 * ・投稿確認ダイアログを表示する
 * ・画面で編集したslugをhidden項目へ反映する
 * ・二重送信防止のため投稿ボタンを無効化する
 */
function publishSubmit() {

	if (!confirm("この記事を投稿しますか？")) {
		return false;
	}

	const form = document.getElementById("publishForm");

	if (!form) {
		alert('投稿フォームを取得できません。');
		return false;
	}

	const workIdInput = form.querySelector('input[name="workId"]');
	const slugInput = document.getElementById('slugInput');
	const slugHidden = document.getElementById('slugHidden');

	if (!workIdInput || !workIdInput.value || !slugInput || !slugHidden) {
		alert('記事情報を取得できません。');
		return false;
	}

	const slug = slugInput.value.trim();

	if (!slug) {
		alert('スラッグを入力してください。');
		return false;
	}

	// 確認画面で編集したslugを投稿リクエストへ反映する
	slugHidden.value = slug;

	const button = document.getElementById("publishButton");

	if (button) {
		button.disabled = true;
		button.textContent = "投稿中...";
	}

	// 元のタブで投稿リクエストを送信する
	form.target = "_self";

	form.submit();

	return false;
}


/**
 * 辞書登録用hidden値同期
 *
 * 表示用の英語変換結果(input)を、
 * 辞書登録フォーム送信用hiddenへ反映する。
 */
function syncEnglish(btn) {
    const reading = btn.getAttribute('data-reading');
    const convertedInput =
        document.getElementById('converted_' + reading);
    const hiddenInput =
        document.getElementById('hidden_' + reading);

    if (!convertedInput || !hiddenInput) {
        throw new Error('英語変換欄を取得できません。');
    }

    hiddenInput.value = convertedInput.value.trim();
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
						'application/x-www-form-urlencoded',
					'X-CSRF-TOKEN': getCsrfToken()
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

