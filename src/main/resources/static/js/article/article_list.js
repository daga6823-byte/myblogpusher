/**
 * 下書き一覧の初期化
 *
 * 共通の記事一覧処理に委譲する。
 */

function initializeDraftArticleList() {

	window.initializeArticleList({
		categoryDataKey: 'categoryName',
		dateSuffix: ''
	});

}

if (document.readyState === 'loading') {

	document.addEventListener(
		'DOMContentLoaded',
		initializeDraftArticleList
	);

} else {

	initializeDraftArticleList();

}

console.log('article_list.js loaded');