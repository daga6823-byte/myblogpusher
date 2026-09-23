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
/**
 * 現在の文字サイズ。
 *
 * Wordのようにボタンで少しずつサイズを変更する。
 */
let currentTextSize = 16;

/**
 * サイズ入力欄に現在の文字サイズを反映する。
 */
function updateTextSizeInput() {
	const input =
		document.getElementById('textSizeInput');

	if (input) {
		input.value = currentTextSize;
	}
}

/**
 * 選択範囲に現在の文字サイズを適用する。
 *
 * すでにfont-sizeを指定しているspanが選択範囲を
 * 包んでいる場合は、既存のサイズだけを置き換える。
 */
function applyCurrentTextSize() {
	if (!markdownEditor) {
		return;
	}

	const start = markdownEditor.selectionStart;
	const end = markdownEditor.selectionEnd;
	const selectedText =
		markdownEditor.value.substring(start, end);

	if (!selectedText) {
		return;
	}

	const sizePattern =
		/^<span style="font-size:\s*[^;]+;">([\s\S]*)<\/span>$/;

	const match = selectedText.match(sizePattern);

	const replacement = match
		? `<span style="font-size: ${currentTextSize}px;">${match[1]}</span>`
		: `<span style="font-size: ${currentTextSize}px;">${selectedText}</span>`;

	markdownEditor.setRangeText(
		replacement,
		start,
		end,
		'select'
	);

	markdownEditor.focus();
}

/**
 * 文字サイズ入力値を反映する。
 */
function applyTextSizeInput() {
	const input =
		document.getElementById('textSizeInput');

	if (!input) {
		return;
	}

	const size = Number.parseInt(input.value, 10);

	if (Number.isNaN(size)) {
		input.value = currentTextSize;
		return;
	}

	currentTextSize = Math.min(
		72,
		Math.max(8, size)
	);

	updateTextSizeInput();
	applyCurrentTextSize();
}

/**
 * 現在の文字色。
 *
 * 初期状態は黒。
 */
let currentTextColor = '#000000';

/**
 * 文字色表示を更新する。
 */
function updateTextColorDisplay() {
	const indicator =
		document.getElementById('textColorIndicator');

	if (indicator) {
		indicator.style.backgroundColor =
			currentTextColor;
	}
}

/**
 * 選択範囲に現在の文字色を適用する。
 */
function applyCurrentTextColor() {
	wrapMarkdownSelection(
		`<span style="color: ${currentTextColor};">`,
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
 * 文字サイズ候補を生成する。
 */
function initializeTextSizeOptions() {

	const textSizeInput =
		document.getElementById('textSizeInput');

	if (!textSizeInput) {
		return;
	}

	const minTextSize = 8;
	const maxTextSize = 80;
	const textSizeStep = 2;

	textSizeInput.innerHTML = '';

	for (
		let size = minTextSize;
		size <= maxTextSize;
		size += textSizeStep
	) {

		const option =
			document.createElement('option');

		option.value = size;
		option.textContent = `${size}px`;

		textSizeInput.appendChild(option);
	}
}

/**
 * 文字サイズを小さくする。
 */
const textSizeDecreaseButton =
	document.getElementById('textSizeDecreaseButton');

if (textSizeDecreaseButton) {
	textSizeDecreaseButton.addEventListener('click', () => {
		currentTextSize = Math.max(
			8,
			currentTextSize - 2
		);

		updateTextSizeInput();
		applyCurrentTextSize();
	});
}

const textSizeIncreaseButton =
	document.getElementById('textSizeIncreaseButton');

if (textSizeIncreaseButton) {

	textSizeIncreaseButton.addEventListener('click', () => {

		currentTextSize = Math.min(

			72,

			currentTextSize + 2

		);

		updateTextSizeInput();

		applyCurrentTextSize();

	});

}

const textSizeInput =
	document.getElementById('textSizeInput');

if (textSizeInput) {
	textSizeInput.addEventListener('change', () => {
		applyTextSizeInput();
	});

	textSizeInput.addEventListener('keydown', event => {
		if (event.key === 'Enter') {
			event.preventDefault();
			applyTextSizeInput();
		}
	});
}

initializeTextSizeOptions();
updateTextSizeInput();

/**
 * 文字色ボタンとカラーパレット。
 */
const textColorButton =
	document.getElementById('textColorButton');

const textColorPalette =
	document.getElementById('textColorPalette');

if (textColorButton && textColorPalette) {

	/*
	 * 文字色ボタンをクリックすると
	 * カラーパレットを開閉する。
	 */
	textColorButton.addEventListener('click', event => {
		event.stopPropagation();

		const isHidden =
			textColorPalette.style.display === 'none'
			|| textColorPalette.style.display === '';

		textColorPalette.style.display =
			isHidden ? 'grid' : 'none';
	});
}

/**
 * 文字色を選択する。
 *
 * 選択した色を現在色として保持し、
 * 次回以降の文字色ボタンにも反映する。
 */
document.querySelectorAll('.text-color-option')
	.forEach(button => {
		button.addEventListener('click', () => {
			const color = button.dataset.color;

			if (!color) {
				return;
			}

			currentTextColor = color;

			updateTextColorDisplay();
			applyCurrentTextColor();

			if (textColorPalette) {
				textColorPalette.style.display = 'none';
			}
		});
	});

/*
 * 初期色は黒。
 */
updateTextColorDisplay();

/**
 * ツールバー外をクリックした場合に
 * カラーパレットを閉じる。
 */
document.addEventListener('click', event => {
	if (textColorPalette
		&& !textColorPalette.contains(event.target)
		&& event.target !== textColorButton) {
		textColorPalette.style.display = 'none';
	}
});
