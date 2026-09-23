/**
 * cursor_pad_viewport.js
 *
 * スマートフォンでソフトウェアキーボードが表示された際、
 * 十字キー（cursor-pad）がキーボードに隠れないよう、
 * キーボードのすぐ上に追従表示させる。
 *
 * 通常時（キーボード非表示時）はCSS側の固定位置（画面右下）を
 * そのまま使う。キーボードの開閉（＝ビューポートの高さ変化）に
 * 応じてのみ位置を調整し、本文スクロール中は位置を動かさない。
 */

const cursorPad = document.querySelector('.cursor-pad');
const contentTextarea = document.getElementById('content');

if (cursorPad && contentTextarea && window.visualViewport) {

	/**
	 * 十字キーの位置を更新する。
	 *
	 * キーボードが開いていると判断できる場合のみ、
	 * position: fixedでキーボード直上に固定する。
	 * それ以外はCSS本来の配置（画面右下）に戻す。
	 */
	function updateCursorPadPosition() {

		const viewport = window.visualViewport;

		// キーボード分だけ縮んだ高さを画面下からの距離として使う
		const offsetBottom =
			window.innerHeight - viewport.height;

		// キーボードが開いていないと判断できる場合は通常位置に戻す
		if (offsetBottom < 50) {
			resetCursorPadPosition();
			return;
		}

		cursorPad.style.position = 'fixed';
		cursorPad.style.bottom = `${offsetBottom + 8}px`;
		cursorPad.style.right = '16px';
		cursorPad.style.top = 'auto';
		cursorPad.style.left = 'auto';
		cursorPad.style.zIndex = '1000';

	}

	/**
	 * 十字キーの位置をCSS本来の配置に戻す。
	 */
	function resetCursorPadPosition() {

		cursorPad.style.position = '';
		cursorPad.style.bottom = '';
		cursorPad.style.right = '';
		cursorPad.style.top = '';
		cursorPad.style.left = '';
		cursorPad.style.zIndex = '';

	}

	/*
	 * ビューポートの高さが変わったとき（＝キーボードの開閉時）のみ
	 * 位置を再計算する。scrollイベントは監視しない
	 * （本文スクロール中に位置がガクガク動くのを防ぐため）。
	 */
	window.visualViewport.addEventListener(
		'resize',
		updateCursorPadPosition
	);

	// ページ読み込み時点の状態を反映しておく
	updateCursorPadPosition();

}