// =====================================================
// workspace.js
//
// ワークスペース管理
// ・5秒後自動保存
// ・セッション維持(KeepAlive)
// =====================================================

// -----------------------------------------------------
// 自動保存タイマー
//
// 入力のたびにリセットされる
// -----------------------------------------------------
let workspaceTimer;
let workspaceSavePromise = Promise.resolve();

// -----------------------------------------------------
// ワークスペースをサーバへ保存する
//
// categoryGroupIdはCategoryRelation.groupIdを使用する。
// categorySelectはカテゴリーPathなので、ここでは使用しない。
// -----------------------------------------------------
function saveWorkspace() {

	const categoryGroupIdElement =
		document.getElementById('categoryGroupId');

	const categoryGroupId =
		categoryGroupIdElement
			? categoryGroupIdElement.value || null
			: null;

	const data = {

		title: document.getElementById('title').value,

		content: document.getElementById('content').value,

		categoryGroupId: categoryGroupId
			? Number(categoryGroupId)
			: null

	};

	workspaceSavePromise = fetch('/article/workspace/save', {

		method: 'POST',

		headers: {

			'Content-Type': 'application/json'

		},

		body: JSON.stringify(data)

	}).catch(err => {

		console.error(err);

	});

	return workspaceSavePromise;
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

// -----------------------------------------------------
// 一時保存時の自動保存競合を防止
// -----------------------------------------------------
const saveButton =
	document.querySelector('button[formaction="/article/save"]');

if (saveButton) {

	saveButton.addEventListener('click', async function(event) {

		event.preventDefault();

		clearTimeout(workspaceTimer);

		saveButton.disabled = true;

		try {

			await workspaceSavePromise;

			saveButton.form.requestSubmit(saveButton);

		} catch (err) {

			console.error(err);

			alert('ワークスペースの保存に失敗しました');

		} finally {

			saveButton.disabled = false;

		}

	});

}

