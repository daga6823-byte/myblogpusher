// ===== 推敲（LanguageTool） =====
// LanguageToolによる文章推敲検索と修正適用を行う
// 結果表示はrenderProofreadResultsで行い、
// 修正適用時は本文を更新して添削画面を再表示する

// ===== 推敲（LanguageTool） =====
// LanguageToolによる文章推敲検索と修正適用を行う

document.getElementById('proofreadButton')?.addEventListener('click', async () => {
	const content = document.getElementById('content').value;
	const categorySelect = document.getElementById('categorySelect').value;
	const newCategoryName = document.getElementById('newCategoryName').value;

	const params = new URLSearchParams();
	params.append('content', content);
	params.append('categorySelect', categorySelect);
	params.append('newCategoryName', newCategoryName);

	const button = document.getElementById('proofreadButton');
	const originalLabel = button.textContent;
	button.textContent = '解析中...';
	button.disabled = true;

	try {
		const res = await fetch('/article/proofread/scan', {
			method: 'POST',
			headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
			body: params.toString()
		});

		if (!res.ok) {
			alert('推敲処理に失敗しました。');
			return;
		}

		const results = await res.json();
		renderProofreadResults(content, results);

	} catch (err) {
		alert('推敲処理に失敗しました。');
	} finally {
		button.textContent = originalLabel;
		button.disabled = false;
	}
});


// 推敲結果を一覧表示する
function renderProofreadResults(content, results) {
	const emptyMessage = document.getElementById('proofreadEmptyMessage');
	const container = document.getElementById('proofreadListContainer');
	const applyButton = document.getElementById('applyProofreadButton');

	proofreadContentSnapshot = content;

	if (results.length === 0) {
		proofreadRanges = [];
		emptyMessage.style.display = 'block';
		container.innerHTML = '';
		applyButton.style.display = 'none';
		renderHighlight();
		return;
	}

	emptyMessage.style.display = 'none';

	proofreadRanges = results.map(r => ({
		start: r.fromPos,
		end: r.toPos,
		message: r.message
	}));

	let html = '<table><thead><tr>' +
		'<th><input type="checkbox" id="proofSelectAllCheckbox"></th>' +
		'<th>該当箇所</th><th>指摘内容</th><th>修正候補</th>' +
		'</tr></thead><tbody>';

	results.forEach(r => {
		const hasSuggestion = r.suggestion && r.suggestion.trim() !== '';
		const disabledAttr = hasSuggestion ? '' : 'disabled';
		const checkedAttr = hasSuggestion ? 'checked' : '';

		html += `<tr>
            <td><input type="checkbox" class="proof-checkbox"
            data-index="${r.index}"
            data-from="${r.fromPos}"
            data-to="${r.toPos}"
            data-suggestion="${escapeAttr(r.suggestion)}"
            ${checkedAttr}
            ${disabledAttr}></td>
            <td>${escapeHtml(r.matchedText)}</td>
            <td>${escapeHtml(r.message)}</td>
            <td>${hasSuggestion ? escapeHtml(r.suggestion) : '（候補なし）'}</td>
        </tr>`;
	});

	html += '</tbody></table>';

	container.innerHTML = html;
	applyButton.style.display = 'inline-block';

	document.getElementById('proofSelectAllCheckbox')?.addEventListener('change', (e) => {
		document.querySelectorAll('.proof-checkbox:not(:disabled)').forEach(cb => {
			cb.checked = e.target.checked;
		});
	});

	renderHighlight();

}


// 選択した推敲候補を本文へ適用する
document.getElementById('applyProofreadButton')?.addEventListener('click', () => {
	const checkboxes = Array.from(document.querySelectorAll('.proof-checkbox:checked'));

	if (checkboxes.length === 0) {
		alert('修正する項目が選択されていません。');
		return;
	}

	const textarea = document.getElementById('content');

	if (textarea.value !== proofreadContentSnapshot) {
		alert('本文が変更されています。お手数ですが再度「推敲」を実行してください。');
		return;
	}

	// 位置がずれないよう、後ろの箇所から処理する
	const sorted = checkboxes
		.map(cb => ({
			from: parseInt(cb.dataset.from, 10),
			to: parseInt(cb.dataset.to, 10),
			suggestion: cb.dataset.suggestion
		}))
		.sort((a, b) => b.from - a.from);

	const confirmList = sorted
		.map(c => `「${textarea.value.slice(c.from, c.to)}」→「${c.suggestion}」`)
		.join('\n');

	const ok = confirm('以下の内容を修正してもよろしいですか？\n\n' + confirmList);

	if (!ok) return;

	let text = textarea.value;

	sorted.forEach(c => {
		text = text.slice(0, c.from) + c.suggestion + text.slice(c.to);
	});

	textarea.value = text;
	contentChanged = true;

	alert('修正しました。');

	// 修正後、フォームを再送信して添削画面を最新の状態で再表示する
	document.querySelector('form').action = '/article/correct';
	document.querySelector('form').submit();
});
