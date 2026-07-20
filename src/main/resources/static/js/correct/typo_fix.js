/**
 * 誤字一括修正処理
 *
 * ・正しい単語内の誤置換を防止する
 * ・選択された誤字を本文へ安全に置換する
 */

/**
 * 正しい単語の内部かどうか判定する
 *
 * 正しい単語へ置換済みの箇所は再置換しない。
 */
function isAlreadyCorrect(
	text,
	start,
	matchedLength,
	correctWord
) {

	if (!correctWord) {
		return false;
	}

	const searchFrom =
		Math.max(
			0,
			start - correctWord.length
		);

	let idx =
		text.indexOf(
			correctWord,
			searchFrom
		);

	while (
		idx !== -1 &&
		idx <= start
	) {

		const correctEnd =
			idx + correctWord.length;

		if (
			start + matchedLength <= correctEnd
		) {
			return true;
		}

		idx =
			text.indexOf(
				correctWord,
				idx + 1
			);

	}

	return false;

}

/**
 * 誤字を安全に一括置換する
 *
 * 正しい単語の内部は置換対象から除外する。
 */
function safeReplaceAll(
	text,
	wrong,
	correct
) {

	let result = '';
	let cursor = 0;

	let idx =
		text.indexOf(
			wrong,
			cursor
		);

	while (idx !== -1) {

		if (
			isAlreadyCorrect(
				text,
				idx,
				wrong.length,
				correct
			)
		) {

			result +=
				text.slice(
					cursor,
					idx + wrong.length
				);

		} else {

			result +=
				text.slice(cursor, idx) +
				correct;

		}

		cursor =
			idx + wrong.length;

		idx =
			text.indexOf(
				wrong,
				cursor
			);

	}

	result +=
		text.slice(cursor);

	return result;

}