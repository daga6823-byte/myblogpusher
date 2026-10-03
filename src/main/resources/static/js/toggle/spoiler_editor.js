// =====================================================
// spoiler_editor.js
//
// ネタバレ挿入機能
// ・ネタバレ入力画面
// ・Hugo spoilerショートコード挿入
// =====================================================

// -----------------------------------------------------
// ネタバレを挿入する位置
//
// ボタン押下時のカーソル位置を保持する
// -----------------------------------------------------

let savedSpoilerCursorPos = 0;

// -----------------------------------------------------
// ネタバレボタン
//
// MarkdownツールバーのSpoilerボタンを取得する。
// -----------------------------------------------------

const spoilerButton =
	document.querySelector('.markdown-tool[data-markdown="spoiler"]');

// -----------------------------------------------------
// ネタバレ入力画面を表示する
// -----------------------------------------------------

if (spoilerButton) {

	spoilerButton.addEventListener('click', function() {

		const textarea =
			document.getElementById('content');

		// カーソル位置を保存
		savedSpoilerCursorPos =
			textarea.selectionStart;

		// ネタバレ入力画面を表示
		document.getElementById('spoilerEditor')
			.style.display = 'block';

		// 入力欄へフォーカス
		document.getElementById('spoilerInput').focus();
	});
}

// -----------------------------------------------------
// Hugo spoilerショートコードを本文へ挿入する
// -----------------------------------------------------

document.getElementById('insertSpoilerButton')
	?.addEventListener('click', function() {

		const spoiler =
			document.getElementById('spoilerInput')
				.value
				.replace(/^[\s　\t\n]+|[\s　\t\n]+$/g, '');

		const textarea =
			document.getElementById('content');

		const before =
			textarea.value.substring(
				0,
				savedSpoilerCursorPos
			);

		const after =
			textarea.value.substring(
				savedSpoilerCursorPos
			);

		// カーソル位置の直前が改行でない場合は改行を追加
		const prefix =
			(before.length > 0 && !before.endsWith('\n'))
				? '\n'
				: '';

		textarea.value =
			before
			+ prefix
			+ '{{< spoiler >}}\n'
			+ spoiler
			+ '\n{{< /spoiler >}}\n'
			+ after;

		// 入力欄をクリア
		document.getElementById('spoilerInput').value = '';

		// 入力画面を閉じる
		document.getElementById('spoilerEditor')
			.style.display = 'none';

		// 本文へフォーカスを戻す
		textarea.focus();
	});

// -----------------------------------------------------
// ネタバレ入力をキャンセルする
// -----------------------------------------------------

document.getElementById('cancelSpoilerButton')
	?.addEventListener('click', function() {

		// 入力欄をクリア
		document.getElementById('spoilerInput').value = '';

		// 入力画面を閉じる
		document.getElementById('spoilerEditor')
			.style.display = 'none';

		// 本文へフォーカスを戻す
		document.getElementById('content').focus();
	});

