/**
 * カテゴリーインデックス編集画面の機能を読み込むファサード
 *
 * 共通の脚注処理をグローバルへ公開してから、
 * インデックス編集画面固有の処理を読み込む。
 */

// 共通の脚注処理を読み込む。
const footnoteScript = document.createElement('script');
footnoteScript.src = '/js/footnote_manager.js';

footnoteScript.onload = () => {
	// 共通処理の読み込み完了後に画面固有の処理を読み込む。
	import('../index/index_reference.js');
};

footnoteScript.onerror = () => {
	console.error('脚注共通処理の読み込みに失敗しました');
};

document.head.appendChild(footnoteScript);
