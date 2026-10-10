/**
 * 公開済みインデックス一覧のカテゴリー階層絞り込みと並び替え、
 * および日時表示を担当するJavaScript
 */

/**
 * 現在のソート条件。
 *
 * 初期状態は更新日時の新しい順にする。
 */
let currentSortKey = 'updateDate';
let currentSortDirection = 'desc';

/**
 * UTC日時をユーザー環境のタイムゾーンで表示する。
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

/**
 * 一覧行からタイトルを取得する。
 */
function getTitle(row) {

	const titleCell =
		row.querySelector('td');

	return titleCell
		? titleCell.textContent.trim()
		: '';

}

/**
 * 一覧行からカテゴリー階層を取得する。
 */
function getCategory(row) {

	return row.dataset.category || '';

}

/**
 * 一覧行から更新日時を取得する。
 */
function getUpdateDate(row) {

	const dateElement =
		row.querySelector('.local-date');

	if (!dateElement) {
		return 0;
	}

	const date =
		new Date(
			dateElement.dataset.date + 'Z'
		);

	return isNaN(date.getTime())
		? 0
		: date.getTime();

}

/**
 * 指定された項目で一覧を並び替える。
 */
function sortRows(rows) {

	rows.sort((a, b) => {

		let result = 0;

		switch (currentSortKey) {

			case 'title':

				result = getTitle(a).localeCompare(
					getTitle(b),
					'ja'
				);

				break;

			case 'category':

				result = getCategory(a).localeCompare(
					getCategory(b),
					'ja'
				);

				break;

			case 'updateDate':

				result =
					getUpdateDate(a) -
					getUpdateDate(b);

				break;

			default:

				result =
					getUpdateDate(a) -
					getUpdateDate(b);

				break;

		}

		return currentSortDirection === 'asc'
			? result
			: -result;

	});

}

/**
 * ソートボタンに現在のソート状態を表示する。
 */
function updateSortButtons() {

	document.querySelectorAll('.sort-button')
		.forEach(button => {

			const sortKey =
				button.dataset.sort;

			const baseText =
				button.textContent
					.replace(/[↑↓]/g, '')
					.trim();

			if (sortKey === currentSortKey) {

				const arrow =
					currentSortDirection === 'asc'
						? ' ↑'
						: ' ↓';

				button.textContent =
					baseText + arrow;

			} else {

				button.textContent =
					baseText;

			}

		});

}

/**
 * 公開済みインデックス一覧を現在のソート条件で並び替える。
 * ソートボタン押下時のみ実行する。
 */
function sortIndexRows() {

	const list =
		document.getElementById('indexList');

	if (!list) {
		return;
	}

	const rows =
		Array.from(list.querySelectorAll('tr'));

	sortRows(rows);

	rows.forEach(row => {
		list.appendChild(row);
	});

	updateSortButtons();
}

/**
 * 公開済みインデックス一覧をカテゴリー階層で絞り込み、
 * 現在のソート条件で並び替える。
 */
function updateIndexList() {

	const list =
		document.getElementById('indexList');

	if (!list) {
		return;
	}

	const categoryFilter =
		document.getElementById('categoryFilter');

	if (!categoryFilter) {
		return;
	}

	const rows =
		Array.from(
			list.querySelectorAll('tr')
		);

	const selectedCategory =
		categoryFilter.value;

	/*
	 * カテゴリー階層で絞り込む。
	 * 元のDOM要素を削除せず、表示・非表示だけを切り替える。
	 */
	rows.forEach(row => {

		const category =
			row.dataset.category || '';

		row.style.display =
			!selectedCategory ||
				category === selectedCategory
				? ''
				: 'none';

	});

	updateSortButtons();

}

/**
 * ソートボタンを初期化する。
 */
function initializeSortButtons() {

	document.querySelectorAll('.sort-button')
		.forEach(button => {

			button.addEventListener(
				'click',
				() => {

					const sortKey =
						button.dataset.sort;

					if (sortKey === currentSortKey) {

						currentSortDirection =
							currentSortDirection === 'asc'
								? 'desc'
								: 'asc';

					} else {

						currentSortKey = sortKey;

						currentSortDirection = 'asc';

					}

					sortIndexRows();

				}
			);

		});

}

/**
 * 公開済みインデックス一覧の初期化。
 */
function initializeIndexList() {

	convertLocalDate();

	const categoryFilter =
		document.getElementById('categoryFilter');

	if (categoryFilter) {

		categoryFilter.addEventListener(
			'change',
			updateIndexList
		);

	}

	initializeSortButtons();
	updateSortButtons();

}

if (document.readyState === 'loading') {

	document.addEventListener(
		'DOMContentLoaded',
		initializeIndexList
	);

} else {

	initializeIndexList();

}

console.log('index_published_list.js loaded');