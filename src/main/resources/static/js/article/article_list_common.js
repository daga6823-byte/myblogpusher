/**
 * 記事一覧の共通処理
 *
 * カテゴリー絞り込み、記事名検索、並び替え、
 * ソート状態表示、日時表示を担当する。
 *
 * 各一覧画面の違いは初期化オプションで吸収する。
 */

/**
 * 記事一覧を初期化する。
 *
 * @param {Object} options 初期化オプション
 * @param {string} options.categoryDataKey カテゴリーを格納したdata属性名
 * @param {string} options.dateSuffix 日時文字列に付加する文字列
 */
window.initializeArticleList = function(options = {}) {

	const categoryDataKey =
		options.categoryDataKey || 'category';

	const dateSuffix =
		options.dateSuffix || '';

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
						element.dataset.date + dateSuffix
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
	 * 一覧行からカテゴリーを取得する。
	 */
	function getCategory(row) {

		return row.dataset[categoryDataKey] || '';

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
				dateElement.dataset.date + dateSuffix
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
	 * カテゴリーと記事名で一覧を絞り込み、
	 * 現在のソート条件で並び替える。
	 */
	function updateArticleList() {

		const list =
			document.getElementById('articleList');

		if (!list) {
			return;
		}

		const categoryFilter =
			document.getElementById('categoryFilter');

		const searchInput =
			document.getElementById('articleSearchInput');

		const rows =
			Array.from(
				list.querySelectorAll('tr')
			);

		const selectedCategory =
			categoryFilter?.value || '';

		const searchText =
			searchInput?.value.trim().toLowerCase() || '';

		/*
		 * カテゴリーと記事名で絞り込む。
		 */
		rows.forEach(row => {

			const category =
				getCategory(row);

			const title =
				getTitle(row).toLowerCase();

			const matchesCategory =
				!selectedCategory ||
				category === selectedCategory;

			const matchesTitle =
				!searchText ||
				title.includes(searchText);

			row.style.display =
				matchesCategory && matchesTitle
					? ''
					: 'none';

		});

		/*
		 * 表示対象の行だけを並び替える。
		 */
		const visibleRows =
			rows.filter(row =>
				row.style.display !== 'none'
			);

		sortRows(visibleRows);

		visibleRows.forEach(row => {
			list.appendChild(row);
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

						updateArticleList();

					}
				);

			});

	}

	/**
	 * 記事一覧の初期化。
	 */
	function initialize() {

		convertLocalDate();

		const categoryFilter =
			document.getElementById('categoryFilter');

		if (categoryFilter) {

			categoryFilter.addEventListener(
				'change',
				updateArticleList
			);

		}

		const searchInput =
			document.getElementById('articleSearchInput');

		if (searchInput) {

			searchInput.addEventListener(
				'input',
				updateArticleList
			);

		}

		initializeSortButtons();
		updateArticleList();

	}

	initialize();

};