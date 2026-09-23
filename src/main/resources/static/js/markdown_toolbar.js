/**
 * Markdown編集ツールバーを担当するJavaScript
 *
 * 本文の選択範囲に対してMarkdown記法を適用する。
 * 文字サイズ・文字色についてはHTMLタグを使用して装飾する。
 *
 * ＋ボタンの挿入メニューはinsert_menu.jsで管理する。
 */

const markdownEditor = document.getElementById('content');

/**
 * 選択範囲を指定した記法で囲む。
 *
 * @param {string} prefix 開始文字列
 * @param {string} suffix 終了文字列
 */
function wrapMarkdownSelection(prefix, suffix) {
	if (!markdownEditor) {
		return;
	}

	const start = markdownEditor.selectionStart;
	const end = markdownEditor.selectionEnd;
	const selectedText = markdownEditor.value.substring(start, end);

	const replacement =
		prefix + selectedText + suffix;

	markdownEditor.setRangeText(
		replacement,
		start,
		end,
		'select'
	);

	markdownEditor.focus();
}

/**
 * 行単位のMarkdown記法を適用する。
 *
 * @param {string} prefix 行頭に追加する文字列
 */
function applyLinePrefix(prefix) {
	if (!markdownEditor) {
		return;
	}

	const start = markdownEditor.selectionStart;
	const end = markdownEditor.selectionEnd;
	const value = markdownEditor.value;

	const lineStart =
		value.lastIndexOf('\n', start - 1) + 1;

	let lineEnd = value.indexOf('\n', end);

	if (lineEnd === -1) {
		lineEnd = value.length;
	}

	const selectedLines =
		value.substring(lineStart, lineEnd);

	const lines = selectedLines.split('\n');

	const replacement = lines
		.map(line => {
			if (line.startsWith(prefix)) {
				return line;
			}

			return prefix + line;
		})
		.join('\n');

	markdownEditor.setRangeText(
		replacement,
		lineStart,
		lineEnd,
		'select'
	);

	markdownEditor.focus();
}

/**
 * 見出しを適用する。
 *
 * @param {number} level 見出しレベル
 */
function applyHeading(level) {
	const prefix = '#'.repeat(level) + ' ';
	applyLinePrefix(prefix);
}

/**
 * 箇条書きを適用する。
 */
function applyUnorderedList() {
	applyLinePrefix('- ');
}

/**
 * 番号付きリストを適用する。
 */
function applyOrderedList() {
	if (!markdownEditor) {
		return;
	}

	const start = markdownEditor.selectionStart;
	const end = markdownEditor.selectionEnd;
	const value = markdownEditor.value;

	const lineStart =
		value.lastIndexOf('\n', start - 1) + 1;

	let lineEnd = value.indexOf('\n', end);

	if (lineEnd === -1) {
		lineEnd = value.length;
	}

	const selectedLines =
		value.substring(lineStart, lineEnd);

	const lines = selectedLines.split('\n');

	const replacement = lines
		.map((line, index) => {
			if (/^\d+\.\s/.test(line)) {
				return line;
			}

			return `${index + 1}. ${line}`;
		})
		.join('\n');

	markdownEditor.setRangeText(
		replacement,
		lineStart,
		lineEnd,
		'select'
	);

	markdownEditor.focus();
}

/**
 * 文字サイズを適用する。
 *
 * @param {string} size CSSの文字サイズ
 */
function applyTextSize(size) {
	wrapMarkdownSelection(
		`<span style="font-size: ${size};">`,
		'</span>'
	);
}

/**
 * 文字色を適用する。
 *
 * @param {string} color CSSの文字色
 */
function applyTextColor(color) {
	wrapMarkdownSelection(
		`<span style="color: ${color};">`,
		'</span>'
	);
}

/**
 * Markdown書式ボタンを処理する。
 */
document.querySelectorAll('.markdown-tool[data-markdown]')
	.forEach(button => {
		button.addEventListener('click', () => {
			const markdownType =
				button.dataset.markdown;

			switch (markdownType) {

				case 'bold':
					wrapMarkdownSelection('**', '**');
					break;

				case 'italic':
					wrapMarkdownSelection('*', '*');
					break;

				case 'strike':
					wrapMarkdownSelection('~~', '~~');
					break;

				case 'code':
					wrapMarkdownSelection('`', '`');
					break;

				case 'h1':
					applyHeading(1);
					break;

				case 'h2':
					applyHeading(2);
					break;

				case 'h3':
					applyHeading(3);
					break;

				case 'unordered-list':
					applyUnorderedList();
					break;

				case 'ordered-list':
					applyOrderedList();
					break;

				case 'quote':
					applyLinePrefix('> ');
					break;

				default:
					break;
			}
		});
	});

/**
 * 文字サイズメニューの開閉。
 */
const textSizeButton =
	document.getElementById('textSizeButton');

const textSizeMenu =
	document.getElementById('textSizeMenu');

if (textSizeButton && textSizeMenu) {
	textSizeButton.addEventListener('click', event => {
		event.stopPropagation();

		const isHidden =
			textSizeMenu.style.display === 'none'
			|| textSizeMenu.style.display === '';

		textSizeMenu.style.display =
			isHidden ? 'block' : 'none';

		if (textColorPalette) {
			textColorPalette.style.display = 'none';
		}
	});
}

/**
 * 文字サイズを選択する。
 */
document.querySelectorAll('.text-size-option')
	.forEach(button => {
		button.addEventListener('click', () => {
			const size = button.dataset.size;

			if (size) {
				applyTextSize(size);
			}

			if (textSizeMenu) {
				textSizeMenu.style.display = 'none';
			}
		});
	});

/**
 * 文字色メニューの開閉。
 */
const textColorButton =
	document.getElementById('textColorButton');

const textColorPalette =
	document.getElementById('textColorPalette');

if (textColorButton && textColorPalette) {
	textColorButton.addEventListener('click', event => {
		event.stopPropagation();

		const isHidden =
			textColorPalette.style.display === 'none'
			|| textColorPalette.style.display === '';

		textColorPalette.style.display =
			isHidden ? 'grid' : 'none';

		if (textSizeMenu) {
			textSizeMenu.style.display = 'none';
		}
	});
}

/**
 * 文字色を選択する。
 */
document.querySelectorAll('.text-color-option')
	.forEach(button => {
		button.addEventListener('click', () => {
			const color = button.dataset.color;

			if (color) {
				applyTextColor(color);
			}

			if (textColorPalette) {
				textColorPalette.style.display = 'none';
			}
		});
	});

/**
 * ツールバー外をクリックした場合に
 * 文字サイズ・文字色メニューを閉じる。
 */
document.addEventListener('click', event => {
	if (textSizeMenu
		&& !textSizeMenu.contains(event.target)
		&& event.target !== textSizeButton) {
		textSizeMenu.style.display = 'none';
	}

	if (textColorPalette
		&& !textColorPalette.contains(event.target)
		&& event.target !== textColorButton) {
		textColorPalette.style.display = 'none';
	}
});