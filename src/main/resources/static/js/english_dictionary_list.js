/**
 * 英単語辞典一覧の並び替えを担当するJavaScript
 *
 * 日本語・英単語のヘッダーをクリックすると、
 * 同じ項目では昇順・降順を切り替える。
 */

// 現在のソート条件。
// 初期状態は日本語の昇順。
let currentSortKey = 'japanese';
let currentSortDirection = 'asc';

/**
 * 英単語辞典一覧を指定された項目で並び替える。
 */
function sortDictionaryRows(rows) {
	rows.sort((a, b) => {
		const columnIndex = {
			japanese: 0,
			english: 1
		}[currentSortKey];

		if (columnIndex === undefined) {
			return 0;
		}

		const aCell = a.querySelectorAll('td')[columnIndex];
		const bCell = b.querySelectorAll('td')[columnIndex];

		if (!aCell || !bCell) {
			return 0;
		}

		const result = aCell.textContent.trim()
			.localeCompare(bCell.textContent.trim(), 'ja');

		return currentSortDirection === 'asc' ? result : -result;
	});
}

/**
 * ソートボタンに現在のソート状態を表示する。
 */
function updateSortButtons() {
	document.querySelectorAll('.sort-button').forEach(button => {
		const sortKey = button.dataset.sort;
		const baseText = button.textContent.replace(/[↑↓]/g, '').trim();

		if (sortKey === currentSortKey) {
			const arrow = currentSortDirection === 'asc' ? ' ↑' : ' ↓';
			button.textContent = baseText + arrow;
		} else {
			button.textContent = baseText;
		}
	});
}

/**
 * 英単語辞典一覧を現在のソート条件で並び替える。
 */
function updateDictionaryList() {
	const list = document.getElementById('englishDictionaryList');

	if (!list) {
		return;
	}

	const rows = Array.from(list.querySelectorAll('tr'));

	sortDictionaryRows(rows);

	rows.forEach(row => list.appendChild(row));

	updateSortButtons();
}

/**
 * ソートボタンを初期化する。
 */
function initializeSortButtons() {
	document.querySelectorAll('.sort-button').forEach(button => {
		button.addEventListener('click', () => {
			const sortKey = button.dataset.sort;

			if (sortKey === currentSortKey) {
				currentSortDirection =
					currentSortDirection === 'asc' ? 'desc' : 'asc';
			} else {
				currentSortKey = sortKey;
				currentSortDirection = 'asc';
			}

			updateDictionaryList();
		});
	});
}

/**
 * 英単語辞典一覧を初期化する。
 */
function initializeDictionaryList() {
	initializeSortButtons();
	updateDictionaryList();
}

if (document.readyState === 'loading') {
	document.addEventListener('DOMContentLoaded', initializeDictionaryList);
} else {
	initializeDictionaryList();
}