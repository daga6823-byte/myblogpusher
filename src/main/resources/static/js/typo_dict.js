document.querySelectorAll('.btn-edit-typo').forEach(btn => {
	btn.addEventListener('click', () => {
		const typoId = btn.dataset.typoId;
		const oldWrong = btn.dataset.wrongWord;
		const oldCorrect = btn.dataset.correctWord;

		const newWrong = prompt('誤字を入力してください', oldWrong);
		if (newWrong === null) return;

		const newCorrect = prompt('正しい表記を入力してください', oldCorrect);
		if (newCorrect === null) return;

		const params = new URLSearchParams();
		params.append('typoId', typoId);
		params.append('wrongWord', newWrong.trim());
		params.append('correctWord', newCorrect.trim());

		fetch('/typo-dict/update', {
			method: 'POST',
			headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
			body: params.toString()
		})
			.then(res => res.json())
			.then(data => {
				if (data.result === 'ok') {
					alert('誤字パターンを更新しました。');
					location.reload();
				}
			});
	});
});

document.getElementById('backButton').addEventListener('click', () => {
	window.location.href = '/home';
});

let sortState = {};

document.querySelectorAll('.sortable').forEach(th => {
	th.style.cursor = 'pointer';
	th.addEventListener('click', () => {
		const key = th.dataset.sort;
		const direction = sortState[key] === 'asc' ? 'desc' : 'asc';
		sortState = { [key]: direction };

		const tbody = document.getElementById('typoTableBody');
		const rows = Array.from(tbody.querySelectorAll('tr'));

		rows.sort((a, b) => {
			const valA = a.dataset[key] || '';
			const valB = b.dataset[key] || '';
			return direction === 'asc'
				? valA.localeCompare(valB, 'ja')
				: valB.localeCompare(valA, 'ja');
		});

		rows.forEach(row => tbody.appendChild(row));

		document.querySelectorAll('.sort-icon').forEach(icon => icon.textContent = '');
		th.querySelector('.sort-icon').textContent = direction === 'asc' ? '▲' : '▼';
	});
});

document.getElementById('categoryFilter').addEventListener('change', () => {
	const selected = document.getElementById('categoryFilter').value;
	const rows = document.querySelectorAll('#typoTableBody tr');

	rows.forEach(row => {
		const category = row.dataset.category;
		row.style.display = (selected === '' || category === selected) ? '' : 'none';
	});
});