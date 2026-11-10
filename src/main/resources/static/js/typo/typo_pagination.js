// ============================================================
// ページ切り替え機能
//
// ページ番号のコンボボックスが変更されたとき、
// 検索キーワードとカテゴリーフィルターを引き継いで一覧ページへ移動する。
// ============================================================

export function initTypoPagination() {

	// ページ番号選択用のコンボボックスが変更されたときの処理
	document.getElementById('pageSelect')?.addEventListener('change', function() {

		// 選択されたページ番号を取得する
		const page = this.value;


		// URLのクエリパラメータを作成するためのオブジェクト
		const params = new URLSearchParams();


		// ページ番号が指定されている場合
		if (page) {

			// pageパラメータに選択されたページ番号を設定する
			params.set('page', page);
		}


		// 検索フォームに入力されているキーワードを取得する
		const keyword =
			document.getElementById('typoSearch')?.value;


		// キーワードが入力されている場合
		if (keyword) {

			// 検索キーワードをURLパラメータに追加する
			params.set('keyword', keyword);
		}


		// カテゴリーフィルターで選択されているカテゴリーIDを取得する
		const categoryId =
			document.getElementById('categoryFilter')?.value;


		// カテゴリーが指定されている場合
		if (categoryId) {

			// カテゴリーIDをURLパラメータに追加する
			params.set('categoryId', categoryId);
		}


		// ページ番号・検索キーワード・カテゴリーIDを付けて
		// 誤字辞典一覧ページへ移動する
		window.location.href =
			'/typo-dict/list?' + params.toString();
	});
}