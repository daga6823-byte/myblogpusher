/**
 * 投稿済み記事一覧の初期化
 *
 * 共通の記事一覧処理に委譲する。
 */

function initializePublishedArticleList() {

	window.initializeArticleList({
		categoryDataKey: 'category',
		dateSuffix: 'Z'
	});

}

if (document.readyState === 'loading') {

	document.addEventListener(
		'DOMContentLoaded',
		initializePublishedArticleList
	);

} else {

	initializePublishedArticleList();

}

console.log('article_published_list.js loaded');