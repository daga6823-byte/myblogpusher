// ===== 誤字登録 =====
// 入力された誤字パターンを誤字辞書へ登録する
// 登録後は添削画面を再表示して最新状態に更新する

document.getElementById('addTypoButton')?.addEventListener('click', () => {
	const wrongWord = document.getElementById('newWrongWord').value.trim();
	const correctWord = document.getElementById('newCorrectWord').value.trim();
	const isGeneral = document.getElementById('newTypoIsGeneral').checked;
	const categorySelect = document.getElementById('categorySelect').value;
	const newCategoryName = document.getElementById('newCategoryName').value;

	if (!correctWord) {
		alert('正しい表記を入力してください。');
		return;
	}

	const params = new URLSearchParams();
	params.append('wrongWord', wrongWord);
	params.append('correctWord', correctWord);
	params.append('categorySelect', categorySelect);
	params.append('newCategoryName', newCategoryName);
	params.append('isGeneral', isGeneral);

	fetch('/article/typo/add', {
		method: 'POST',
		headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
		body: params.toString()
	})
		.then(res => res.json())
		.then(data => {
			if (data.result === 'ok') {
				alert('誤字パターンを登録しました。');
				document.getElementById('newWrongWord').value = '';
				document.getElementById('newCorrectWord').value = '';

				// 修正後、フォームを再送信して添削画面を最新の状態で再表示する
				document.querySelector('form').action = '/article/correct';
				document.querySelector('form').submit();
			} else if (data.result === 'duplicate') {
				alert(data.message);
			}
		});
});
