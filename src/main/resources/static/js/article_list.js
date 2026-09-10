/**
 * 下書き一覧のカテゴリー絞り込みと並び替え、
 * および日時表示を担当するJavaScript
 */

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


/**
 * 下書き一覧をカテゴリーで絞り込む
 * または指定された順番で並び替える
 */
function updateArticleList() {

	const list = document.getElementById('articleList');

	if (!list) {
		return;
	}

	const categoryFilter =
		document.getElementById('categoryFilter');

	const sortOrder =
		document.getElementById('sortOrder');

	if (!categoryFilter || !sortOrder) {
		return;
	}

	const rows =
		Array.from(list.querySelectorAll('tr'));

	const selectedCategory =
		categoryFilter.value;

	const selectedSort =
		sortOrder.value;

	/*
	 * カテゴリーで絞り込む。
	 * 元のDOM要素を削除せず、表示・非表示だけを切り替える。
	 */
	rows.forEach(row => {

		const category =
			row.dataset.categoryName || '';

		row.style.display =
			!selectedCategory ||
				category === selectedCategory
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

	visibleRows.sort((a, b) => {

		switch (selectedSort) {

			case 'title-asc':
				return getTitle(a).localeCompare(
					getTitle(b),
					'ja'
				);

			case 'title-desc':
				return getTitle(b).localeCompare(
					getTitle(a),
					'ja'
				);

			case 'update-asc':
				return getUpdateDate(a) -
					getUpdateDate(b);

			case 'update-desc':
			default:
				return getUpdateDate(b) -
					getUpdateDate(a);
		}
	});

	visibleRows.forEach(row => {
		list.appendChild(row);
	});
}


/**
 * 一覧行からタイトルを取得する
 */
function getTitle(row) {

	const titleCell =
		row.querySelector('td');

	return titleCell
		? titleCell.textContent.trim()
		: '';
}


/**
 * 一覧行から更新日時を取得する
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
 * 下書き一覧の初期化
 */
function initializeArticleList() {

	convertLocalDate();

	const categoryFilter =
		document.getElementById('categoryFilter');

	const sortOrder =
		document.getElementById('sortOrder');

	if (categoryFilter) {
		categoryFilter.addEventListener(
			'change',
			updateArticleList
		);
	}

	if (sortOrder) {
		sortOrder.addEventListener(
			'change',
			updateArticleList
		);
	}

	updateArticleList();
}


if (document.readyState === 'loading') {

	document.addEventListener(
		'DOMContentLoaded',
		initializeArticleList
	);

} else {

	initializeArticleList();

}

console.log("article_list.js loaded");
