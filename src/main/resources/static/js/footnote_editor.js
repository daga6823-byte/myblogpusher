// =====================================================
// footnote_editor.js
//
// 脚注挿入機能
// ・脚注入力ダイアログ表示
// ・カーソル位置へ [^n] を挿入
// ・本文末尾へ [^n]: 脚注テキスト を追記
// =====================================================

// -----------------------------------------------------
// 脚注を挿入する位置
// ボタン押下時のカーソル位置を保持する
// -----------------------------------------------------
let savedFootnoteCursorPos = 0;

// -----------------------------------------------------
// 本文中の [^数字] を走査し、次の脚注番号を決定する
// 既存の参照・定義の両方を対象に最大値+1を返す
// -----------------------------------------------------
function getNextFootnoteNumber(text) {

	const matches = [...text.matchAll(/\[\^(\d+)\]/g)];

	if (matches.length === 0) {
		return 1;
	}

	const max = Math.max(...matches.map(m => parseInt(m[1], 10)));

	return max + 1;
}

// -----------------------------------------------------
// 脚注入力画面を表示する
// -----------------------------------------------------
document.getElementById('footnoteButton').addEventListener('click', function() {

	const textarea = document.getElementById('content');

	// カーソル位置を保存
	savedFootnoteCursorPos = textarea.selectionStart;

	// メニューを閉じる
	document.getElementById('insertMenu').style.display = 'none';

	// 脚注入力画面を表示
	document.getElementById('footnoteEditor').style.display = 'block';

	// 入力欄へフォーカス
	document.getElementById('footnoteInput').focus();

});

// -----------------------------------------------------
// 脚注を本文へ挿入する
// カーソル位置に [^n] を挿入し、末尾に [^n]: テキスト を追記する
// -----------------------------------------------------
document.getElementById('insertFootnoteButton').addEventListener('click', function() {

	const footnoteText = document.getElementById('footnoteInput')
		.value
		.replace(/^[\s　\t\n]+|[\s　\t\n]+$/g, '');

	if (footnoteText === '') {
		return;
	}

	const textarea = document.getElementById('content');
	const num = getNextFootnoteNumber(textarea.value);
	const ref = `[^${num}]`;

	// カーソル位置に参照 [^n] を挿入
	const before = textarea.value.substring(0, savedFootnoteCursorPos);
	const after = textarea.value.substring(savedFootnoteCursorPos);

	textarea.value = before + ref + after;

	// 本文末尾に定義行 [^n]: テキスト を追記
	const body = textarea.value;
	const separator = body.endsWith('\n') ? '' : '\n';

	textarea.value = body + separator + `[^${num}]: ${footnoteText}\n`;

	// 入力欄をクリア
	document.getElementById('footnoteInput').value = '';

	// ダイアログを閉じる
	document.getElementById('footnoteEditor').style.display = 'none';

	// 本文へフォーカスを戻す
	textarea.focus();

});

// -----------------------------------------------------
// 脚注入力をキャンセルする
// -----------------------------------------------------
document.getElementById('cancelFootnoteButton').addEventListener('click', function() {

	// 入力欄をクリア
	document.getElementById('footnoteInput').value = '';

	// ダイアログを閉じる
	document.getElementById('footnoteEditor').style.display = 'none';

	// 本文へフォーカスを戻す
	document.getElementById('content').focus();

});
