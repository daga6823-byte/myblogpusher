/**
 * cursor_pad_control.js
 *
 * 十字キー（cursor-pad）によるカーソル移動を担当するJavaScript
 *
 * 各ボタンはタップで1回移動、長押しで一定間隔の
 * オートリピート移動を行う（PC・iPhoneのどちらでも動作するよう
 * pointerイベントを使用する）。
 */

const cursorPadTextarea = document.getElementById('content');

// 長押し開始までの待機時間（ms）
const REPEAT_START_DELAY = 400;

// オートリピート中の移動間隔（ms）
const REPEAT_INTERVAL = 80;

/**
 * 左へ1文字移動する。
 */
function moveCursorLeft() {

	cursorPadTextarea.focus();

	const position = cursorPadTextarea.selectionStart;

	if (position > 0) {
		cursorPadTextarea.setSelectionRange(
			position - 1,
			position - 1
		);
	}
}

/**
 * 右へ1文字移動する。
 */
function moveCursorRight() {

	cursorPadTextarea.focus();

	const position = cursorPadTextarea.selectionStart;

	if (position < cursorPadTextarea.value.length) {
		cursorPadTextarea.setSelectionRange(
			position + 1,
			position + 1
		);
	}
}

/**
 * 上下方向へカーソルを移動する。
 *
 * @param {number} direction -1で上、1で下
 */
function moveCursorVertically(direction) {

	cursorPadTextarea.focus();

	const position = cursorPadTextarea.selectionStart;
	const text = cursorPadTextarea.value;

	// 現在位置より前の最後の改行を探す。
	const currentLineStart = text.lastIndexOf('\n', position - 1) + 1;

	// 現在行のカーソル位置を取得する。
	const column = position - currentLineStart;

	if (direction < 0) {

		// 先頭行なら何もしない。
		if (currentLineStart === 0) {
			return;
		}

		const previousLineEnd = currentLineStart - 1;
		const previousLineStart =
			text.lastIndexOf('\n', previousLineEnd - 1) + 1;

		const previousLineLength =
			previousLineEnd - previousLineStart;

		const targetColumn =
			Math.min(column, previousLineLength);

		const targetPosition =
			previousLineStart + targetColumn;

		cursorPadTextarea.setSelectionRange(
			targetPosition,
			targetPosition
		);

	} else {

		const currentLineEnd =
			text.indexOf('\n', position);

		// 最終行なら何もしない。
		if (currentLineEnd === -1) {
			return;
		}

		const nextLineStart = currentLineEnd + 1;
		const nextLineEnd =
			text.indexOf('\n', nextLineStart);

		const nextLineLength =
			(nextLineEnd === -1
				? text.length
				: nextLineEnd) - nextLineStart;

		const targetColumn =
			Math.min(column, nextLineLength);

		const targetPosition =
			nextLineStart + targetColumn;

		cursorPadTextarea.setSelectionRange(
			targetPosition,
			targetPosition
		);
	}
}

/**
 * ボタンにタップ／長押しオートリピートを設定する。
 *
 * pointerdownで即座に1回移動し、一定時間後から
 * 一定間隔で移動を繰り返す。pointerup・pointerleave・
 * pointercancelで停止する。
 *
 * @param {HTMLElement} button 対象のボタン要素
 * @param {Function} moveAction 1回分の移動処理
 */
function setupCursorRepeatButton(button, moveAction) {

	if (!button) {
		return;
	}

	let startTimeoutId = null;
	let repeatIntervalId = null;

	function stopRepeat() {

		if (startTimeoutId !== null) {
			clearTimeout(startTimeoutId);
			startTimeoutId = null;
		}

		if (repeatIntervalId !== null) {
			clearInterval(repeatIntervalId);
			repeatIntervalId = null;
		}

	}

	button.addEventListener('pointerdown', event => {

		event.preventDefault();

		// タップ・長押し共通で、押した瞬間に1回移動する。
		moveAction();

		stopRepeat();

		// 長押しと判定できたらオートリピートを開始する。
		startTimeoutId = setTimeout(() => {

			repeatIntervalId = setInterval(() => {
				moveAction();
			}, REPEAT_INTERVAL);

		}, REPEAT_START_DELAY);

	});

	button.addEventListener('pointerup', stopRepeat);
	button.addEventListener('pointerleave', stopRepeat);
	button.addEventListener('pointercancel', stopRepeat);

}

setupCursorRepeatButton(
	document.getElementById('cursorLeftButton'),
	moveCursorLeft
);

setupCursorRepeatButton(
	document.getElementById('cursorRightButton'),
	moveCursorRight
);

setupCursorRepeatButton(
	document.getElementById('cursorUpButton'),
	() => moveCursorVertically(-1)
);

setupCursorRepeatButton(
	document.getElementById('cursorDownButton'),
	() => moveCursorVertically(1)
);
