// =====================================================
// footnote_editor.js
//
// 脚注挿入機能
// ・脚注入力ダイアログ表示
// ・カーソル位置へ [^n] を挿入
// ・本文末尾へ [^n]: 脚注テキスト を追記
// =====================================================

// -----------------------------------------------------
// 脚注入力画面を表示する
// -----------------------------------------------------
document.getElementById('footnoteButton').addEventListener('click', () => {

	// メニューを閉じる
	document.getElementById('insertMenu').style.display = 'none';

	const textarea = document.getElementById('content');

	if (!textarea) {
		return;
	}

	const content = textarea.value;

	const nextNo = getNextFootnoteNumber(content);

	const title = prompt("参考文献名を入力してください");

	if (!title) {
		return;
	}

	const url = prompt("リンクを入力してください");

	if (!url) {
		return;
	}

	const marker = `[^${nextNo}]`;

	const start = textarea.selectionStart;
	const end = textarea.selectionEnd;

	// 本文中へ [1] を挿入
	textarea.value =
		content.substring(0, start)
		+ marker
		+ content.substring(end);

	// 文末へ脚注定義追加
	if (!textarea.value.endsWith("\n")) {
		textarea.value += "\n";
	}

	textarea.value +=
		`\n${marker}: [${title}](${url})`;

	const pos = start + marker.length;

	textarea.focus();
	textarea.setSelectionRange(pos, pos);

	textarea.dispatchEvent(new Event("input"));
});
