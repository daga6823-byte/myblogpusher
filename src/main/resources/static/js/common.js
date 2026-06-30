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
