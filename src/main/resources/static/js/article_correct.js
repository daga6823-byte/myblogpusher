let contentChanged = false;

['categorySelect', 'newCategoryName', 'title', 'content'].forEach(id => {
	const el = document.getElementById(id);
	if (el) {
		el.addEventListener('input', () => { contentChanged = true; });
		el.addEventListener('change', () => { contentChanged = true; });
	}
});

const selectAllCheckbox = document.getElementById('selectAllCheckbox');

selectAllCheckbox?.addEventListener('change', () => {
	document.querySelectorAll('.typo-checkbox').forEach(cb => {
		cb.checked = selectAllCheckbox.checked;
	});
});

function syncSelectAllState() {
	if (!selectAllCheckbox) return;

	const checkboxes = document.querySelectorAll('.typo-checkbox');
	const total = checkboxes.length;
	const checkedCount = document.querySelectorAll('.typo-checkbox:checked').length;

	if (checkedCount === 0) {
		selectAllCheckbox.checked = false;
		selectAllCheckbox.indeterminate = false;
	} else if (checkedCount === total) {
		selectAllCheckbox.checked = true;
		selectAllCheckbox.indeterminate = false;
	} else {
		selectAllCheckbox.checked = false;
		selectAllCheckbox.indeterminate = true; // 一部だけ選択中
	}
}

document.querySelectorAll('.typo-checkbox').forEach(cb => {
	cb.addEventListener('change', syncSelectAllState);
});

window.addEventListener('DOMContentLoaded', syncSelectAllState);

//選択中のチェックボックス
function isAlreadyCorrect(text, start, matchedLength, correctWord) {
    if (!correctWord) return false;
    const searchFrom = Math.max(0, start - correctWord.length);
    let idx = text.indexOf(correctWord, searchFrom);
    while (idx !== -1 && idx <= start) {
        const correctEnd = idx + correctWord.length;
        if (start + matchedLength <= correctEnd) {
            return true; // すでに正しい単語の内部に収まっている
        }
        idx = text.indexOf(correctWord, idx + 1);
    }
    return false;
}

function safeReplaceAll(text, wrong, correct) {
    let result = '';
    let cursor = 0;
    let idx = text.indexOf(wrong, cursor);

    while (idx !== -1) {
        if (isAlreadyCorrect(text, idx, wrong.length, correct)) {
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

	if (ok) {
		const textarea = document.getElementById('content');
		let text = textarea.value;

		checkboxes.forEach(cb => {
			text = safeReplaceAll(text, cb.dataset.wrong, cb.dataset.correct);
		});

		textarea.value = text;
		contentChanged = true;
		alert('修正しました。');

		// 修正後、フォームを再送信して添削画面を最新の状態で再表示する
		document.querySelector('form').action = '/article/correct';
		document.querySelector('form').submit();
	}
});

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

document.getElementById('backButton').addEventListener('click', () => {
	const workId = document.getElementById('workId').value;
	const editUrl = workId ? ('/article/edit?workId=' + workId) : '/article/edit';

	if (contentChanged) {
		const wantSave = confirm('保存していない変更があります。保存しますか？');
		if (wantSave) {
			const form = document.querySelector('form');
			form.action = '/article/save';
			form.submit();
		} else {
			window.location.href = editUrl;
		}
	} else {
		window.location.href = editUrl;
	}
});

function getHighlightMatches(text, typoPairs) {
	// サーバー側と同じく、長い単語を優先(部分一致の食い合いを防ぐ)
	const sorted = [...typoPairs].sort((a, b) => b.wrong.length - a.wrong.length);
	const matches = [];

	sorted.forEach(({ wrong, correct }) => {
		if (!wrong) return;
		let idx = text.indexOf(wrong);
		while (idx !== -1) {
			const end = idx + wrong.length;
			const overlaps = matches.some(m => idx < m.end && end > m.start);
			if (!overlaps && !isAlreadyCorrect(text, idx, wrong.length, correct)) {
				matches.push({ start: idx, end });
			}
			idx = text.indexOf(wrong, idx + 1);
		}
	});

	matches.sort((a, b) => a.start - b.start);
	return matches;
}

function renderHighlight() {
	const textarea = document.getElementById('content');
	const highlightDiv = document.getElementById('contentHighlight');
	const text = textarea.value;

	const typoPairs = Array.from(document.querySelectorAll('.typo-checkbox'))
		.map(cb => ({ wrong: cb.dataset.wrong, correct: cb.dataset.correct }));

	const matches = getHighlightMatches(text, typoPairs);

	let html = '';
	let cursor = 0;
	matches.forEach(({ start, end }) => {
		html += escapeHtml(text.slice(cursor, start));
		html += '<mark>' + escapeHtml(text.slice(start, end)) + '</mark>';
		cursor = end;
	});
	html += escapeHtml(text.slice(cursor));

	highlightDiv.innerHTML = html;
}

function escapeHtml(text) {
	return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

document.getElementById('content')?.addEventListener('scroll', () => {
	const textarea = document.getElementById('content');
	const highlightDiv = document.getElementById('contentHighlight');
	highlightDiv.scrollTop = textarea.scrollTop;
});

window.addEventListener('DOMContentLoaded', renderHighlight);

function updateButtonState() {
	const content = document.getElementById('content').value.trim();
	const categorySelect = document.getElementById('categorySelect').value;
	const newCategoryName = document.getElementById('newCategoryName').value.trim();

	const hasCategory = categorySelect !== '' &&
		(categorySelect !== '__new__' || newCategoryName !== '');
	const hasContent = content !== '';

	const enabled = hasCategory && hasContent;

	document.querySelector('[formaction="/article/save"]').disabled = !enabled;
}

['categorySelect', 'newCategoryName', 'content'].forEach(id => {
	const el = document.getElementById(id);
	if (el) {
		el.addEventListener('input', updateButtonState);
		el.addEventListener('change', updateButtonState);
	}
});

window.addEventListener('DOMContentLoaded', updateButtonState);
