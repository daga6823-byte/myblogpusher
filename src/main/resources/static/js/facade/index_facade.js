/**
 * カテゴリーインデックス編集画面の機能を読み込むファサード
 *
 * 共通の脚注処理と参考文献処理を読み込んでから、
 * インデックス編集画面固有の処理を読み込む。
 */

// 共通の脚注処理を読み込む。
const footnoteScript = document.createElement('script');

footnoteScript.src = '/js/footnote_manager.js';

footnoteScript.onload = () => {

	// 参考文献の共通処理を読み込む。
	const referenceScript = document.createElement('script');

	referenceScript.src = '/js/image/reference_manager.js';

	referenceScript.onload = () => {

		// 共通処理の読み込み完了後に画面固有の処理を読み込む。
		import('../index/index_reference.js');

	};

	referenceScript.onerror = () => {
		console.error('参考文献共通処理の読み込みに失敗しました');
	};

	document.head.appendChild(referenceScript);

};

footnoteScript.onerror = () => {
	console.error('脚注共通処理の読み込みに失敗しました');
};

document.head.appendChild(footnoteScript);