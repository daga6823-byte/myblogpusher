//戻るボタン
const backButton = document.getElementById('backButton');

if (backButton) {
	backButton.addEventListener('click', () => {
		window.location.href = '/home';
	});
}
