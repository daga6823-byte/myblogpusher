// =====================================================
// code_editor.js
//
// コードブロック挿入機能
// ・挿入メニュー表示
// ・コード入力ダイアログ
// ・Markdownコードブロック挿入
// =====================================================

// -----------------------------------------------------
// コードブロックを挿入する位置
// ボタン押下時のカーソル位置を保持する
// -----------------------------------------------------
let savedCursorPos = 0;

// -----------------------------------------------------
// 挿入メニューの表示・非表示切替
// -----------------------------------------------------
document.getElementById('insertMenuButton').addEventListener('click', function() {

	const menu = document.getElementById('insertMenu');

	menu.style.display =
		menu.style.display === 'none'
			? 'block'
			: 'none';

});

// -----------------------------------------------------
// コード入力画面を表示する
// -----------------------------------------------------
document.getElementById('codeBlockButton').addEventListener('click', function() {

	const textarea = document.getElementById('content');

	// カーソル位置を保存
	savedCursorPos = textarea.selectionStart;

	// メニューを閉じる
	document.getElementById('insertMenu').style.display = 'none';

	// コード入力画面を表示
	document.getElementById('codeBlockEditor').style.display = 'block';

	// 入力欄へフォーカス
	document.getElementById('codeInput').focus();

});

// -----------------------------------------------------
// Markdownコードブロックを本文へ挿入する
// -----------------------------------------------------
document.getElementById('insertCodeButton').addEventListener('click', function() {

	const code = document.getElementById('codeInput')
		.value
		.replace(/^[\s　\t\n]+|[\s　\t\n]+$/g, '');

	const textarea = document.getElementById('content');

	const before = textarea.value.substring(0, savedCursorPos);
	const after = textarea.value.substring(savedCursorPos);

	// カーソル位置の直前が改行でない場合は改行を追加
	const prefix =
		(before.length > 0 && !before.endsWith('\n'))
			? '\n'
			: '';

	textarea.value =
		before
		+ prefix
		+ '```\n'
		+ code
		+ '\n```\n'
		+ after;

	// 入力欄をクリア
	document.getElementById('codeInput').value = '';

	// ダイアログを閉じる
	document.getElementById('codeBlockEditor').style.display = 'none';

	// 本文へフォーカスを戻す
	textarea.focus();

});