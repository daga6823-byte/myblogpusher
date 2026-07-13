// =====================================================
// workspace.js
//
// ワークスペース管理
// ・5秒後自動保存
// ・セッション維持(KeepAlive)
// =====================================================

// -----------------------------------------------------
// 自動保存タイマー
// 入力のたびにリセットされる
// -----------------------------------------------------
let workspaceTimer;

// -----------------------------------------------------
// ワークスペースをサーバへ保存する
//
// 新規カテゴリー(__new__)はまだcategoryIdが存在しないため
// categoryIdは送信しない
// -----------------------------------------------------
function saveWorkspace() {

	const categorySelect = document.getElementById('categorySelect').value;

	const categoryId =
		(categorySelect && categorySelect !== '__new__')
			? categorySelect
			: null;

	const data = {
		title: document.getElementById('title').value,
		content: document.getElementById('content').value,
		categoryId: categoryId
	};

	fetch('/article/workspace/save', {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json'
		},
		body: JSON.stringify(data)
	}).catch(err => console.error(err));

}

// -----------------------------------------------------
// 自動保存予約
//
// 最後の入力から5秒経過したら保存する
// 入力が続く間はタイマーをリセットする
// -----------------------------------------------------
function scheduleWorkspaceSave() {

	clearTimeout(workspaceTimer);

	workspaceTimer = setTimeout(() => {
		saveWorkspace();
	}, 5000);

}

// -----------------------------------------------------
// タイトル・本文編集時に自動保存予約
// -----------------------------------------------------
['title', 'content'].forEach(id => {

	const el = document.getElementById(id);

	if (el) {
		el.addEventListener('input', scheduleWorkspaceSave);
	}

});

// -----------------------------------------------------
// セッション維持
//
// 編集中にセッション切れにならないよう
// 10分ごとにKeepAliveを送信する
// -----------------------------------------------------
setInterval(() => {

	fetch('/article/session/keepalive', {
		method: 'POST'
	}).catch(err => console.error(err));

}, 10 * 60 * 1000);