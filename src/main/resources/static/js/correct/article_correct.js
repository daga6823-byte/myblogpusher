// article_collect.js

// 添削画面共通初期化
// 各機能JS読み込み後に必要な初期処理のみ実行する

window.addEventListener('DOMContentLoaded', () => {

	// ハイライト初期表示
	renderHighlight();

	// 保存ボタン状態初期化
	updateButtonState();

	// 全選択チェック状態初期化
	syncSelectAllState();

});

const backButton = document.getElementById('backButton');

if (backButton) {
    backButton.addEventListener('click', () => {
        const workId =
            document.querySelector('input[name="workId"]').value;

        window.location.href =
            `/article/edit?workId=${workId}`;
    });
}
