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
function convertLocalDate() {

	document.querySelectorAll('.local-date')
		.forEach(element => {

			const date =
				new Date(
					element.dataset.date + 'Z'
				);

			if (isNaN(date.getTime())) {
				return;
			}

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
}


if (document.readyState === 'loading') {

	document.addEventListener(
		'DOMContentLoaded',
		convertLocalDate
	);

} else {

	convertLocalDate();

}

console.log("common.js loaded");

console.log(
	document.querySelectorAll('.local-date').length
);
