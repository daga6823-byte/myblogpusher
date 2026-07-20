// ===== 誤字検索（LanguageTool） =====

document.getElementById('scanTypoButton')?.addEventListener('click', async () => {
	const content = document.getElementById('content').value;
	const categorySelect = document.getElementById('categorySelect').value;
	const newCategoryName = document.getElementById('newCategoryName').value;

	const params = new URLSearchParams();
	params.append('content', content);
	params.append('categorySelect', categorySelect);
	params.append('newCategoryName', newCategoryName);

	const button = document.getElementById('scanTypoButton');
	const originalLabel = button.textContent;
	button.textContent = '検索中...';
	button.disabled = true;

	try {
		const res = await fetch('/article/typo/scan', {
			method: 'POST',
			headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
			body: params.toString()
		});

		if (!res.ok) {
			alert('誤字検索に失敗しました。');
			return;
		}

		const results = await res.json();
		renderTypoScanResults(results);

	} catch (err) {
		alert('誤字検索に失敗しました。');
	} finally {
		button.textContent = originalLabel;
		button.disabled = false;
	}
});


// LanguageToolの誤字検索結果を画面へ表示する
function renderTypoScanResults(results) {
	const container = document.getElementById('ltTypoResults');
	const addSelectedButton = document.getElementById('addSelectedTyposButton');

	if (results.length === 0) {
		container.innerHTML = '<p>新たな誤字は見つかりませんでした。</p>';
		addSelectedButton.style.display = 'none';
		return;
	}

	let html = '<table><thead><tr>' +
		'<th><input type="checkbox" id="ltSelectAllCheckbox"></th>' +
		'<th>誤字</th><th>修正候補</th><th>説明</th>' +
		'</tr></thead><tbody>';

	results.forEach((r) => {
		html += `<tr>
            <td><input type="checkbox" class="lt-typo-checkbox" data-wrong="${escapeAttr(r.wrongWord)}" data-correct="${escapeAttr(r.suggestion)}" checked></td>
            <td>${escapeHtml(r.wrongWord)}</td>
            <td>${escapeHtml(r.suggestion)}</td>
            <td>${escapeHtml(r.message)}</td>
        </tr>`;
	});

	html += '</tbody></table>';

	container.innerHTML = html;
	addSelectedButton.style.display = 'inline-block';

	document.getElementById('ltSelectAllCheckbox')?.addEventListener('change', (e) => {
		document.querySelectorAll('.lt-typo-checkbox').forEach(cb => {
			cb.checked = e.target.checked;
		});
	});
}


// HTML属性へ埋め込む文字列をエスケープする
function escapeAttr(text) {
	return (text || '').replace(/&/g, '&amp;').replace(/"/g, '&quot;');
}
