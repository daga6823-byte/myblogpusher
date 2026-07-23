// 誤字修正適用処理
// 選択した誤字パターンを本文へ反映する

document.addEventListener('DOMContentLoaded', () => {

	document.getElementById('applyFixesButton')?.addEventListener('click', () => {
		const checkboxes = document.querySelectorAll('.typo-checkbox:checked');

		if (checkboxes.length === 0) {
			alert('修正する項目が選択されていません。');
			return;
		}

		const confirmList = Array.from(checkboxes)
			.map(cb => `「${cb.dataset.wrong}」→「${cb.dataset.correct}」`)
			.join('\n');

		const ok = confirm('以下の内容を修正してもよろしいですか？\n\n' + confirmList);

		if (!ok) {
			return;
		}

		const textarea = document.getElementById('content');
		let text = textarea.value;

		checkboxes.forEach(cb => {
			text = safeReplaceAll(
				text,
				cb.dataset.wrong,
				cb.dataset.correct
			);
		});
	});

	textarea.value = text;
	contentChanged = true;

	alert('修正しました。');

	// 修正後、フォームを再送信して添削画面を最新の状態で再表示する
	document.querySelector('form').action = '/article/correct';
	document.querySelector('form').submit();
});


// 文字列を全置換する
// String.replace() は1件目しか置換しないため split/join を利用する
function safeReplaceAll(text, target, replacement) {
	return text.split(target).join(replacement);
}