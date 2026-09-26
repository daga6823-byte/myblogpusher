/**
 * image_state.js
 *
 * 画像管理機能（image_folders.js / image_gallery.js / image_uploader.js）が
 * 共有する状態をまとめたモジュール。
 *
 * ESモジュールではimportした変数を他モジュールから再代入できないため、
 * 1つのオブジェクトにまとめ、各モジュールがプロパティを書き換える形にする。
 */

export const imageState = {

	// 画像挿入位置（本文テキストエリアのカーソル位置）
	insertPosition: null,

	// 画像一覧の現在ページ
	page: 0,

	// 画像一覧の絞り込み対象フォルダ名
	folderName: null,

	// 画像一覧のファイル名検索キーワード
	searchKeyword: '',

	// 画像一覧の並び替え種別
	sortType: 'dateDesc',

	// 画像一覧取得リクエストの識別番号（古いレスポンスの反映を防ぐ）
	loadRequestId: 0

};