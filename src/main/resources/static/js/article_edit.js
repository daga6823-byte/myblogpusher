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