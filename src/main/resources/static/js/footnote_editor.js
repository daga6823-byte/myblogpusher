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

	// 次の脚注番号を採番
	const matches = [...content.matchAll(/\[\^(\d+)\]/g)];
	const nextNo = matches.length
		? Math.max(...matches.map(m => Number(m[1]))) + 1
		: 1;

	const url = prompt("リンクを入力してください");

	if (!url) {
		return;
	}

	const marker = `[^${nextNo}]`;

	// カーソル位置へ脚注マーカー挿入
	const start = textarea.selectionStart;
	const end = textarea.selectionEnd;

	textarea.value =
		content.substring(0, start)
		+ marker
		+ content.substring(end);

	// 文末へ脚注追加
	if (!textarea.value.endsWith("\n")) {
		textarea.value += "\n";
	}

	textarea.value += `\n${marker}: ${url}`;

	// カーソル位置更新
	const pos = start + marker.length;
	textarea.focus();
	textarea.setSelectionRange(pos, pos);

	textarea.dispatchEvent(new Event("input"));
});
