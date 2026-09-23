/**
 * cursor_pad_viewport.js
 *
 * スマートフォンで十字キー（cursor-pad）を常に画面右下付近に
 * 固定表示し、ソフトウェアキーボードが開いている間は
 * キーボードのすぐ上に追従させる。
 *
 * 位置（position/right/bottom）は常にこのスクリプトが管理し、
 * CSS側のposition指定には委ねない。
 * bottomの変化自体はCSSのtransitionでなめらかにし、
 * キーボード開閉アニメーション中の複数回のresizeイベントによる
 * カクつきを吸収する。
 *
 * 着地位置にはホームインジケーター等のセーフエリア分を
 * 加味し、画面外に隠れないようにする。
 */

const cursorPad = document.querySelector('.cursor-pad');

// キーボードが開いていないときの基準位置（画面下からの距離）
const DEFAULT_BOTTOM = 90;
const SIDE_MARGIN = 16;

/**
 * ホームインジケーター等のセーフエリア（下端）の高さを取得する。
 */
function getSafeAreaBottom() {

	const value = getComputedStyle(document.documentElement)
		.getPropertyValue('--safe-area-bottom');

	const parsed = Number.parseFloat(value);

	return Number.isNaN(parsed) ? 0 : parsed;

}

if (cursorPad && window.visualViewport) {

	/**
	 * 十字キーの位置を更新する。
	 *
	 * キーボードが開いている場合はキーボード直上、
	 * それ以外はDEFAULT_BOTTOM＋セーフエリア分の位置に固定する。
	 */
	function updateCursorPadPosition() {

		const viewport = window.visualViewport;

		// キーボード分だけ縮んだ高さを画面下からの距離として使う
		const keyboardHeight =
			window.innerHeight - viewport.height;

		const isKeyboardOpen = keyboardHeight > 50;

		const safeAreaBottom = getSafeAreaBottom();

		const bottom = isKeyboardOpen
			? keyboardHeight + 8
			: DEFAULT_BOTTOM + safeAreaBottom;

		cursorPad.style.position = 'fixed';
		cursorPad.style.bottom = `${bottom}px`;
		cursorPad.style.right = `${SIDE_MARGIN}px`;
		cursorPad.style.top = 'auto';
		cursorPad.style.left = 'auto';
		cursorPad.style.zIndex = '1000';

	}

	/*
	 * ビューポートの高さが変わったとき（＝キーボードの開閉時）に
	 * 位置を再計算する。アニメーション中に複数回発火しても、
	 * CSS側のtransitionが吸収するため見た目はなめらかになる。
	 */
	window.visualViewport.addEventListener(
		'resize',
		updateCursorPadPosition
	);

	// ページ読み込み時点で基準位置に固定しておく
	updateCursorPadPosition();

}