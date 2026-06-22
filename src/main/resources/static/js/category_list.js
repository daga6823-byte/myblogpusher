document.getElementById('backButton').addEventListener('click', () => {
	window.location.href = '/home';
});

document.getElementById('addCategoryButton').addEventListener('click', () => {
	const categoryName = document.getElementById('newCategoryNameInput').value.trim();

	const params = new URLSearchParams();
	params.append('categoryName', categoryName);

	fetch('/category/add', {
		method: 'POST',
		headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
		body: params.toString()
	})
		.then(res => res.json())
		.then(data => {
			if (data.result === 'ok') {
				alert('カテゴリーを登録しました。');
				location.reload();
			} else {
				alert(data.message);
			}
		});
});

document.querySelectorAll('.btn-rename').forEach(btn => {
	btn.addEventListener('click', () => {
		const categoryId = btn.dataset.categoryId;
		const oldName = btn.dataset.categoryName;
		const newName = prompt('新しいカテゴリー名を入力してください', oldName);

		if (!newName || newName.trim() === '' || newName === oldName) {
			return;
		}

		const params = new URLSearchParams();
		params.append('categoryId', categoryId);
		params.append('newName', newName.trim());

		fetch('/category/rename', {
			method: 'POST',
			headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
			body: params.toString()
		})
			.then(res => res.json())
			.then(data => {
				if (data.result === 'ok') {
					alert('カテゴリー名を変更しました。');
					location.reload();
				} else {
					alert(data.message);
				}
			});
	});
});