document.getElementById('importButton').addEventListener('click', () => {
	const status = document.getElementById('importStatus');
	status.textContent = 'インポート中...';
	fetch('/image/import', { method: 'POST' })
		.then(res => res.json())
		.then(data => {
			status.textContent = `${data.importedCount}件インポートしました`;
			location.reload();
		})
		.catch(() => { status.textContent = 'インポートに失敗しました'; });
});

document.getElementById('imageCategorySelect')
	.addEventListener('change', function() {

		const categoryId = this.value;

		if (categoryId) {
			location.href =
				'/image/list?categoryId=' + categoryId;
		} else {
			location.href =
				'/image/list';
		}

	});