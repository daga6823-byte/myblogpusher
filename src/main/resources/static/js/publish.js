// =====================================================
// publish.js
//
// 公開確認画面への遷移を担当
// ・入力チェック
// ・POSTでプレビュー画面へ遷移
// =====================================================

// -----------------------------------------------------
// 公開確認画面へ遷移する
// -----------------------------------------------------
document.getElementById('publishButton').addEventListener('click', function() {

	// URLからworkIdを取得
	const workId = new URLSearchParams(window.location.search).get('workId');

	// 入力値取得
	const title = document.querySelector('input[name="title"]').value;
	const content = document.querySelector('textarea[name="content"]').value;
	const categorySelect = document.querySelector('select[name="categorySelect"]').value;
	const newCategoryName =
		document.querySelector('input[name="newCategoryName"]')?.value || '';

	// 必須チェック
	if (!title || !content) {
		alert('タイトルと本文を入力してください');
		return;
	}

	// -------------------------------------------------
	// POST用フォームを動的生成
	// （URLパラメータを使わず画面遷移するため）
	// -------------------------------------------------
	const form = document.createElement('form');
	form.method = 'POST';
	form.action = '/publish/preview';

	const fields = {
		workId: workId || '',
		title: title,
		content: content,
		categorySelect: categorySelect,
		newCategoryName: newCategoryName
	};

	// hidden項目生成
	for (const [key, value] of Object.entries(fields)) {

		const input = document.createElement('input');

		input.type = 'hidden';
		input.name = key;
		input.value = value;

		form.appendChild(input);
	}

	// フォーム送信
	document.body.appendChild(form);
	form.submit();

});