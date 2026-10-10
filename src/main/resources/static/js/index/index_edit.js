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


// =====================================================
// 投稿前確認画面への遷移
// =====================================================
(() => {
	const publishButton = document.getElementById('publishButton');
	const contentForm = publishButton?.closest('form');

	if (!publishButton || !contentForm) {
		return;
	}

	publishButton.addEventListener('click', () => {
		const groupId = contentForm.querySelector('input[name="groupId"]')?.value;
		const textarea = document.getElementById('content');
		const titleInput = document.getElementById('title');

		if (!groupId || !textarea || !titleInput) {
			return;
		}

		// タイトル欄の内容をFront Matterへ反映してから送信する。
		updateIndexFrontMatter();

		if (!titleInput.value.trim() || !textarea.value.trim()) {
			alert('タイトルと本文を入力してください。');
			return;
		}

		// 編集中の内容をPOSTし、サーバー側で下書き保存後に確認画面を表示する。
		const form = document.createElement('form');
		form.method = 'post';
		form.action = '/index/publish/preview';

		[
			['groupId', groupId],
			['content', textarea.value]
		].forEach(([name, value]) => {
			const input = document.createElement('input');
			input.type = 'hidden';
			input.name = name;
			input.value = value;
			form.appendChild(input);
		});

		document.body.appendChild(form);
		form.submit();
	});
})();
// =====================================================
// ワークスペースの自動保存
//
// 最後の入力から5秒後にindex_workspaceへ保存する。
// 手動保存・投稿成功後の削除処理はサーバー側で行う。
// =====================================================
(() => {
	let workspaceTimer;

	function saveIndexWorkspace() {
		const groupId = document.querySelector('input[name="groupId"]');
		const titleInput = document.getElementById('title');
		const textarea = document.getElementById('content');

		if (!groupId?.value || !titleInput || !textarea) {
			return;
		}

		// コントローラーの@RequestParamに合わせてフォーム形式で送信する。
		const params = new URLSearchParams({
			groupId: groupId.value,
			content: textarea.value
		});

		fetch('/index/workspace/autosave', {
			method: 'POST',
			headers: {
				'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
			},
			body: params.toString()
		}).then(response => {
			if (!response.ok) {
				throw new Error(`HTTP ${response.status}`);
			}
		}).catch(error => {
			console.error('インデックスの自動保存に失敗しました:', error);
		});
	}

	function scheduleIndexWorkspaceSave() {
		clearTimeout(workspaceTimer);
		workspaceTimer = setTimeout(saveIndexWorkspace, 5000);
	}

	['title', 'content'].forEach(id => {
		const element = document.getElementById(id);

		if (element) {
			element.addEventListener('input', scheduleIndexWorkspaceSave);
		}
	});
})();

// 10分ごとにセッションを維持する。
setInterval(() => {
	fetch('/article/session/keepalive', {
		method: 'POST'
	}).catch(error => {
		console.error('セッション維持に失敗しました:', error);
	});
}, 10 * 60 * 1000);

