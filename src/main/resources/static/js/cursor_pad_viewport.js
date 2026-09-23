/**
 * cursor_pad_viewport.js
 *
 * スマートフォンでソフトウェアキーボードが表示された際、
 * 十字キー（cursor-pad）がキーボードに隠れないよう、
 * キーボードのすぐ上に追従表示させる。
 *
 * visualViewport APIでキーボード表示によるビューポートの
 * 高さ変化を検知し、本文入力中のみ十字キーの位置を固定する。
 */

const cursorPad = document.querySelector('.cursor-pad');
const contentTextarea = document.getElementById('content');

if (cursorPad && contentTextarea && window.visualViewport) {

	let isContentFocused = false;

	/**
	 * 十字キーの位置を更新する。
	 *
	 * 本文にフォーカスがあり、かつキーボードが
	 * 表示されていると判断できる場合のみ、
	 * position: fixedでキーボード直上に固定する。
	 */
	function updateCursorPadPosition() {

		if (!isContentFocused) {
			resetCursorPadPosition();
			return;
		}

		const viewport = window.visualViewport;

		// キーボード分だけ縮んだ高さを画面下からの距離として使う
		const offsetBottom =
			window.innerHeight
			- (viewport.height + viewport.offsetTop);

		// キーボードが開いていないと判断できる場合は通常位置に戻す
		if (offsetBottom < 50) {
			resetCursorPadPosition();
			return;
		}

		cursorPad.style.position = 'fixed';
		cursorPad.style.bottom = `${offsetBottom + 8}px`;
		cursorPad.style.right = '8px';
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

	contentTextarea.addEventListener('focus', () => {
		isContentFocused = true;
		updateCursorPadPosition();
	});

	contentTextarea.addEventListener('blur', () => {
		isContentFocused = false;
		resetCursorPadPosition();
	});

	window.visualViewport.addEventListener(
		'resize',
		updateCursorPadPosition
	);

	window.visualViewport.addEventListener(
		'scroll',
		updateCursorPadPosition
	);

}