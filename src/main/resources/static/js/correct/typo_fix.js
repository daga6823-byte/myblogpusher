// 誤字修正適用処理
// 選択した誤字パターンを本文へ反映する

// 選択中のチェックボックス
function isAlreadyCorrect(text, start, matchedLength, correctWord) {
	if (!correctWord) return false;

	const searchFrom = Math.max(0, start - correctWord.length);

	let idx = text.indexOf(correctWord, searchFrom);

	while (idx !== -1 && idx <= start) {
		const correctEnd = idx + correctWord.length;

		if (start + matchedLength <= correctEnd) {
			return true;
		}

		idx = text.indexOf(correctWord, idx + 1);
	}

	return false;
}

// 誤字を安全に一括置換する
function safeReplaceAll(text, wrong, correct) {
	let result = '';
	let cursor = 0;
	let idx = text.indexOf(wrong, cursor);

	while (idx !== -1) {

		if (isAlreadyCorrect(
			text,
			idx,
			wrong.length,
			correct
		)) {
			result += text.slice(cursor, idx + wrong.length);
		} else {
			result += text.slice(cursor, idx) + correct;
		}

		cursor = idx + wrong.length;
		idx = text.indexOf(wrong, cursor);
	}

	result += text.slice(cursor);

	return result;
}

document.addEventListener('click', (event) => {

	if (event.target.id !== 'applyFixesButton') {
		return;
	}

	const checkboxes = document.querySelectorAll('.typo-checkbox:checked');

	if (checkboxes.length === 0) {
		alert('修正する項目が選択されていません。');
		return;
	}

	const confirmList = Array.from(checkboxes)
		.map(cb => `「${cb.dataset.wrong}」→「${cb.dataset.correct}」`)
		.join('\n');

	const ok = confirm(
		'以下の内容を修正してもよろしいですか？\n\n' + confirmList
	);

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

	textarea.value = text;
	contentChanged = true;

	alert('修正しました。');

	const form = document.querySelector('form');
	form.action = '/article/correct';
	form.submit();
});