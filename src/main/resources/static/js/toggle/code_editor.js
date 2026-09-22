// =====================================================
// code_editor.js
//
// コードブロック挿入機能
// ・コード入力ダイアログ
// ・Markdownコードブロック挿入
// =====================================================

// -----------------------------------------------------
// コードブロックを挿入する位置
// ボタン押下時のカーソル位置を保持する
// -----------------------------------------------------
let savedCursorPos = 0;

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

// -----------------------------------------------------
// コード入力をキャンセルする
// -----------------------------------------------------
document.getElementById('cancelCodeButton').addEventListener('click', function() {

	// 入力欄をクリア
	document.getElementById('codeInput').value = '';

	// ダイアログを閉じる
	document.getElementById('codeBlockEditor').style.display = 'none';

	// 本文へフォーカスを戻す
	document.getElementById('content').focus();

});

// ペーストする位置を、ボタンにフォーカスが移る前に保存する。
// PC・iPhoneのどちらでも動作するよう、pointerdownを使用する。
document.getElementById('pasteButton').addEventListener('pointerdown', function() {

	const textarea = document.getElementById('content');

	savedCursorPos = textarea.selectionStart;

});

// -----------------------------------------------------
// クリップボードの内容を本文へ貼り付ける
// -----------------------------------------------------

document.getElementById('pasteButton').addEventListener('click', async function() {

	const textarea = document.getElementById('content');

	try {

		const text = await navigator.clipboard.readText();

		if (!text) {

			return;

		}

		const before = textarea.value.substring(0, savedCursorPos);

		const after = textarea.value.substring(savedCursorPos);

		textarea.value = before + text + after;

		// 挿入後の位置にカーソルを移動する。
		savedCursorPos += text.length;

		textarea.focus();

		textarea.setSelectionRange(
			savedCursorPos,
			savedCursorPos
		);

	} catch (error) {

		console.error(
			'クリップボードの読み取りに失敗しました:',
			error
		);

		alert('クリップボードの内容を取得できませんでした。');

	}

});

// -----------------------------------------------------
// カーソル移動
// -----------------------------------------------------

const contentTextarea = document.getElementById('content');

// 左へ1文字移動
document.getElementById('cursorLeftButton').addEventListener('click', function() {

	contentTextarea.focus();

	const position = contentTextarea.selectionStart;

	if (position > 0) {
		contentTextarea.setSelectionRange(
			position - 1,
			position - 1
		);
	}
});

// 右へ1文字移動
document.getElementById('cursorRightButton').addEventListener('click', function() {

	contentTextarea.focus();

	const position = contentTextarea.selectionStart;

	if (position < contentTextarea.value.length) {
		contentTextarea.setSelectionRange(
			position + 1,
			position + 1
		);
	}
});

// 上へ1行移動
document.getElementById('cursorUpButton').addEventListener('click', function() {

	moveCursorVertically(-1);

});

// 下へ1行移動
document.getElementById('cursorDownButton').addEventListener('click', function() {

	moveCursorVertically(1);

});

// 上下方向へカーソルを移動する
function moveCursorVertically(direction) {

	contentTextarea.focus();

	const position = contentTextarea.selectionStart;
	const text = contentTextarea.value;

	// 現在位置より前の最後の改行を探す。
	const currentLineStart = text.lastIndexOf('\n', position - 1) + 1;

	// 現在行のカーソル位置を取得する。
	const column = position - currentLineStart;

	if (direction < 0) {

		// 先頭行なら何もしない。
		if (currentLineStart === 0) {
			return;
		}

		const previousLineEnd = currentLineStart - 1;
		const previousLineStart =
			text.lastIndexOf('\n', previousLineEnd - 1) + 1;

		const previousLineLength =
			previousLineEnd - previousLineStart;

		const targetColumn =
			Math.min(column, previousLineLength);

		const targetPosition =
			previousLineStart + targetColumn;

		contentTextarea.setSelectionRange(
			targetPosition,
			targetPosition
		);

	} else {

		const currentLineEnd =
			text.indexOf('\n', position);

		// 最終行なら何もしない。
		if (currentLineEnd === -1) {
			return;
		}

		const nextLineStart = currentLineEnd + 1;
		const nextLineEnd =
			text.indexOf('\n', nextLineStart);

		const nextLineLength =
			(nextLineEnd === -1
				? text.length
				: nextLineEnd) - nextLineStart;

		const targetColumn =
			Math.min(column, nextLineLength);

		const targetPosition =
			nextLineStart + targetColumn;

		contentTextarea.setSelectionRange(
			targetPosition,
			targetPosition
		);
	}
}

