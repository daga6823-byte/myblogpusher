// ============================================================
// type-dict-facade.js
// 誤字辞典一覧画面のファサード
//
// 誤字辞典一覧画面で使う機能の初期化をまとめて行う。
// HTMLはこのファイルだけを type="module" で読み込めばよい。
// 各機能の実装は typo-edit / typo-sort / typo-bulk / typo-pagination が担当する。
// ============================================================

import { initTypoEdit } from '../typo/typo_edit.js';
import { initTypoSort } from '../typo/typo_sort.js';
import { initTypoBulk } from '../typo/typo_bulk.js';
import { initTypoPagination } from '../typo/typo_pagination.js';

// 誤字辞典一覧画面の全機能を初期化する
export function initTypoDict() {

	// 誤字パターン編集機能
	initTypoEdit();

	// テーブルのソート機能
	initTypoSort();

	// 一括選択・一括カテゴリー変更・一括削除機能
	initTypoBulk();

	// ページ切り替え機能
	initTypoPagination();
}


// type="module" のスクリプトはDOMの解析完了後に実行されるため、
// 読み込まれた時点でそのまま初期化する
initTypoDict();
