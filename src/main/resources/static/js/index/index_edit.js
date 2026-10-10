/**
 * カテゴリーインデックス編集画面のUI制御を担当するJavaScript
 *
 * Hugo Front Matterの初期化・更新と、保存前の内容更新を管理する。
 * 記事編集画面のJavaScriptには依存しない。
 */

// =====================================================
// 初期化
// =====================================================
window.addEventListener('DOMContentLoaded', () => {
	const textarea = document.getElementById('content');

	if (!textarea) {
		return;
	}

	// 既存Front Matterがある場合は、タイトル欄へ先に反映する。
	initIndexFrontMatter();

	// 新規作成時はタイトル欄の値を使って初期化する。
	if (textarea.value.trim() === '') {
		updateIndexFrontMatter();
	}

	updateIndexButtonState();
});

// =====================================================
// Front Matter解析
// =====================================================
function parseIndexFrontMatter(text) {
	const match = text.match(/^---\r?\n([\s\S]*?)\r?\n---\r?\n*/);

	return match ? match[1] : null;
}

// =====================================================
// Front Matterの値を取得
// =====================================================
function getIndexFrontMatterValue(frontMatter, key) {
	const escapedKey = key.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
	const match = frontMatter.match(
		new RegExp('^' + escapedKey + ':\\s*"(.*)"\\s*$', 'm')
	);

	return match ? match[1].replace(/\\"/g, '"').replace(/\\\\/g, '\\') : '';
}

// =====================================================
// YAML文字列のエスケープ
// =====================================================
function escapeIndexYaml(value) {
	return String(value ?? '')
		.replace(/\\/g, '\\\\')
		.replace(/"/g, '\\"')
		.replace(/\r?\n/g, ' ');
}

// =====================================================
// Front Matterを初期化
// =====================================================
function initIndexFrontMatter() {
	const textarea = document.getElementById('content');
	const titleInput = document.getElementById('title');

	if (!textarea || !titleInput) {
		return;
	}

	const frontMatter = parseIndexFrontMatter(textarea.value);

	if (!frontMatter) {
		return;
	}

	titleInput.value = getIndexFrontMatterValue(frontMatter, 'title');
}

// =====================================================
// Front Matterを最新状態へ更新
// =====================================================
function updateIndexFrontMatter() {
	const textarea = document.getElementById('content');
	const titleInput = document.getElementById('title');

	if (!textarea || !titleInput) {
		return;
	}

	const currentText = textarea.value;
	const frontMatter = parseIndexFrontMatter(currentText);

	// Front Matter以外の本文を維持する。
	const bodyMatch = currentText.match(/^---\r?\n[\s\S]*?\r?\n---\r?\n*/);
	const bodyText = bodyMatch
		? currentText.substring(bodyMatch[0].length)
		: currentText;

	const title = escapeIndexYaml(titleInput.value);

	// 既存のdescriptionは維持し、新規作成時は空文字にする。
	const description = frontMatter
		? escapeIndexYaml(getIndexFrontMatterValue(frontMatter, 'description'))
		: '';

	const newFrontMatter =
		'---\n'
		+ 'title: "' + title + '"\n'
		+ 'description: "' + description + '"\n'
		+ '---\n\n';

	textarea.value = newFrontMatter + bodyText;
}

// =====================================================
// 保存ボタン押下時にFront Matterを更新
// =====================================================
document.addEventListener('DOMContentLoaded', () => {
	const form = document.querySelector('form');

	if (!form) {
		return;
	}

	form.addEventListener('submit', () => {
		updateIndexFrontMatter();
	});
});

// =====================================================
// ボタン状態更新
// =====================================================
function updateIndexButtonState() {
	const textarea = document.getElementById('content');
	const titleInput = document.getElementById('title');

	if (!textarea || !titleInput) {
		return;
	}

	const updateButtons = () => {
		const hasContent = textarea.value.trim() !== ''
			&& titleInput.value.trim() !== '';

		document.querySelectorAll('button[type="submit"]').forEach(button => {
			button.disabled = !hasContent;
		});
	};

	textarea.addEventListener('input', updateButtons);
	titleInput.addEventListener('input', updateButtons);

	updateButtons();
}

// =====================================================
// クリップボードの内容を本文へ貼り付ける
// =====================================================
(() => {
	const textarea = document.getElementById('content');
	const pasteButton = document.getElementById('pasteButton');

	if (!textarea || !pasteButton) {
		return;
	}

	let savedCursorPos = textarea.selectionStart;

	// ボタンにフォーカスが移る前にカーソル位置を保存する。
	pasteButton.addEventListener('pointerdown', () => {
		savedCursorPos = textarea.selectionStart;
	});

	pasteButton.addEventListener('click', async () => {
		try {
			const text = await navigator.clipboard.readText();

			if (!text) {
				return;
			}

			const before = textarea.value.substring(0, savedCursorPos);
			const after = textarea.value.substring(savedCursorPos);

			textarea.value = before + text + after;
			savedCursorPos += text.length;

			textarea.focus();
			textarea.setSelectionRange(savedCursorPos, savedCursorPos);
			textarea.dispatchEvent(new Event('input', { bubbles: true }));
		} catch (error) {
			console.error('クリップボードの読み取りに失敗しました:', error);
			alert('クリップボードの内容を取得できませんでした。');
		}
	});
})();

// =====================================================
// 編集内容のクリア
// =====================================================
(() => {
	const textarea = document.getElementById('content');
	const titleInput = document.getElementById('title');
	const clearButton = document.getElementById('clearButton');

	if (!textarea || !titleInput || !clearButton) {
		return;
	}

	clearButton.addEventListener('click', () => {
		if (!confirm('タイトルと本文をクリアしますか？')) {
			return;
		}

		titleInput.value = '';
		textarea.value = '';

		textarea.dispatchEvent(new Event('input', { bubbles: true }));
		titleInput.dispatchEvent(new Event('input', { bubbles: true }));

		textarea.focus();
	});
})();
