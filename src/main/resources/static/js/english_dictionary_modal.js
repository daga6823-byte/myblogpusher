/**
 * 英単語辞典の登録・編集モーダルを担当するJavaScript
 *
 * 新規登録・編集フォームの表示、辞典データの保存、
 * モーダルのキャンセル処理を担当する。
 */

// モーダル要素
const modal = document.getElementById('englishDictionaryModal');
const modalTitle = document.getElementById('englishDictionaryModalTitle');
const dictionaryIdInput = document.getElementById('dictionaryId');
const japaneseInput = document.getElementById('dictionaryJapanese');
const englishInput = document.getElementById('dictionaryEnglish');

let editMode = false;

/**
 * 編集モーダルを開く。
 */
document.querySelectorAll('.btn-update').forEach(btn => {
	btn.addEventListener('click', () => {
		editMode = true;

		modalTitle.textContent = '英単語辞典編集';
		dictionaryIdInput.value = btn.dataset.id;
		japaneseInput.value = btn.dataset.japanese;
		englishInput.value = btn.dataset.english;

		modal.style.display = 'block';
	});
});

/**
 * 新規登録モーダルを開く。
 */
const addDictionaryButton = document.getElementById('addDictionaryButton');

if (addDictionaryButton) {
	addDictionaryButton.addEventListener('click', () => {
		editMode = false;

		modalTitle.textContent = '英単語辞典追加';
		dictionaryIdInput.value = '';
		japaneseInput.value = '';
		englishInput.value = '';

		modal.style.display = 'block';
	});
}

/**
 * 英単語辞典を新規登録または更新する。
 */
const saveDictionaryButton = document.getElementById('saveDictionaryButton');

if (saveDictionaryButton) {
	saveDictionaryButton.addEventListener('click', async () => {
		const japanese = japaneseInput.value.trim();
		const english = englishInput.value.trim();

		if (!japanese || !english) {
			alert('日本語と英単語を入力してください。');
			return;
		}

		const params = new URLSearchParams();
		params.append('japanese', japanese);
		params.append('english', english);

		if (editMode) {
			params.append('id', dictionaryIdInput.value);
		}

		try {
			const response = await fetch('/english-dictionary/save', {
				method: 'POST',
				headers: {
					'Content-Type': 'application/x-www-form-urlencoded'
				},
				body: params.toString()
			});

			if (!response.ok) {
				throw new Error('保存に失敗しました。');
			}

			location.href = response.url;

		} catch (error) {
			console.error(error);
			alert('保存に失敗しました。');
		}
	});
}

/**
 * モーダルを閉じる。
 */
const cancelDictionaryButton = document.getElementById('cancelDictionaryButton');

if (cancelDictionaryButton) {
	cancelDictionaryButton.addEventListener('click', () => {
		modal.style.display = 'none';
	});
}