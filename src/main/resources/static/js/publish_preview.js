function registerWord(btn) {
	const reading = btn.getAttribute('data-reading');
	const converted = btn.getAttribute('data-converted');
	const english = prompt('英単語を入力してください', converted);
	if (!english) return;

	fetch('/dictionary/register', {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ japanese: reading, english: english })
	}).then(res => {
		if (res.ok) {
			alert('登録しました');
			location.reload();
		}
	});
}

function publishSubmit() {

	if (!confirm("この記事を投稿しますか？")) {
		return false;
	}

	const button = document.getElementById("publishButton");

	button.disabled = true;
	button.textContent = "投稿中...";

	return true;
}

function syncEnglish(reading) {
	const converted = document.getElementById('converted_' + reading).value;
	document.getElementById('hidden_' + reading).value = converted;
}

function updateSlug() {
	const inputs = document.querySelectorAll('[id^="converted_"]');
	let parts = [];
	inputs.forEach(input => {
		if (input.value.trim()) {
			parts.push(input.value.trim().toLowerCase());
		}
	});
	document.getElementById('slugInput').value = parts.join('-');
}

document.querySelectorAll('[id^="converted_"]').forEach(input => {
	input.addEventListener('input', updateSlug);
});