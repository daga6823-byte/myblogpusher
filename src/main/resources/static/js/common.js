//戻るボタン
document.getElementById('backButton').addEventListener('click', () => {
	window.location.href = '/home';
});

//クリアボタン
document.getElementById('clearButton').addEventListener('click', function() {
	if (confirm('ワークスペースをクリアしてもよろしいですか？')) {
		fetch('/article/workspace/clear', {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json'
			}
		}).then(() => {
			location.reload();
		});
	}
});

/**
 * UTC日時をユーザー環境のタイムゾーンで表示する
 */
document.querySelectorAll('.local-date')
	.forEach(element => {

		const value = element.dataset.date;

		if (!value) {
			return;
		}

		const date = new Date(value + 'Z');

		element.textContent =
			date.toLocaleString(
				undefined,
				{
					year: 'numeric',
					month: '2-digit',
					day: '2-digit',
					hour: '2-digit',
					minute: '2-digit'
				}
			);

	});
