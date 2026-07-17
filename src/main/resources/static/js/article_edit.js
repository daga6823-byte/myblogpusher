window.addEventListener('DOMContentLoaded', () => {
	const msg = document.getElementById('savedMessage');
	if (msg) {
		setTimeout(() => {
			msg.style.display = 'none';
		}, 3000);
	}

	const textarea = document.getElementById('content');
	if (textarea && textarea.value.trim() === '') {
		updateFrontMatterFields(true);
	} else {
		initFrontMatterSelects();
	}

	updateButtonState();
});

const copyAllButton = document.getElementById('copyAllButton');

if (copyAllButton) {
	copyAllButton.addEventListener('click', async () => {
		const title = document.getElementById('title').value;
		const content = document.getElementById('content').value;

		const combinedText = `${title}\n\n${content}`;

		try {
			await navigator.clipboard.writeText(combinedText);

			const originalLabel = copyAllButton.textContent;
			copyAllButton.textContent = 'コピーしました';

			setTimeout(() => {
				copyAllButton.textContent = originalLabel;
			}, 1500);

		} catch (err) {
			alert('コピーに失敗しました。');
		}
	});
}