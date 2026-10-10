
/**
 * インデックス投稿前確認画面の送信処理
 *
 * ・GitHub反映前に確認ダイアログを表示する
 * ・二重送信防止のため送信ボタンを無効化する
 */
function publishIndexSubmit() {
	if (!confirm('このインデックスをGitHubへ反映しますか？')) {
		return false;
	}

	const button = document.getElementById('publishButton');

	if (button) {
		button.disabled = true;
		button.textContent = '反映中...';
	}

	return true;
}
