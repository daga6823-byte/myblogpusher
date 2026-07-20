/**
 * ハイライト表示制御
 *
 * ・誤字候補と推敲候補を本文上でハイライト表示する
 * ・誤字を優先し、推敲候補との重複表示を防ぐ
 * ・textareaとハイライト表示領域のスクロール位置を同期する
 */

/**
 * 推敲対象範囲
 * proofread.jsで更新される
 */
let proofreadRanges = [];

/**
 * 推敲実行時の本文スナップショット
 * proofread.jsで更新される
 */
let proofreadContentSnapshot = '';

/**
 * 本文のハイライトを描画する
 */
function renderHighlight() {

	const textarea = document.getElementById('content');
	const highlightDiv = document.getElementById('contentHighlight');
	const text = textarea.value;

	const typoPairs = Array.from(document.querySelectorAll('.typo-checkbox'))
		.map(cb => ({
			wrong: cb.dataset.wrong,
			correct: cb.dataset.correct
		}));

	const typoMatches = getHighlightMatches(text, typoPairs)
		.map(m => ({
			...m,
			type: 'typo'
		}));

	// 推敲範囲のうち誤字と重複しないものだけ採用
	const proofMatches = (proofreadRanges || [])
		.filter(p => !typoMatches.some(t => p.start < t.end && p.end > t.start))
		.map(p => ({
			start: p.start,
			end: p.end,
			type: 'proof'
		}));

	const allMatches =
		[...typoMatches, ...proofMatches]
			.sort((a, b) => a.start - b.start);

	let html = '';
	let cursor = 0;

	allMatches.forEach(({ start, end, type }) => {

		html += escapeHtml(text.slice(cursor, start));

		const cls = type === 'proof'
			? ' class="proof"'
			: '';

		html += `<mark${cls}>`
			+ escapeHtml(text.slice(start, end))
			+ '</mark>';

		cursor = end;

	});

	html += escapeHtml(text.slice(cursor));

	highlightDiv.innerHTML = html;

}

/**
 * 誤字ハイライト範囲を取得する
 *
 * 長い単語を優先し、
 * 重複ハイライトを防止する。
 */
function getHighlightMatches(text, typoPairs) {

	const sorted =
		[...typoPairs]
			.sort((a, b) => b.wrong.length - a.wrong.length);

	const matches = [];

	sorted.forEach(({ wrong, correct }) => {

		if (!wrong) {
			return;
		}

		let idx = text.indexOf(wrong);

		while (idx !== -1) {

			const end = idx + wrong.length;

			const overlaps =
				matches.some(m =>
					idx < m.end &&
					end > m.start);

			if (
				!overlaps &&
				!isAlreadyCorrect(text, idx, wrong.length, correct)
			) {

				matches.push({
					start: idx,
					end: end
				});

			}

			idx = text.indexOf(wrong, idx + 1);

		}

	});

	matches.sort((a, b) => a.start - b.start);

	return matches;

}

/**
 * HTML特殊文字をエスケープする
 */
function escapeHtml(text) {

	return text
		.replace(/&/g, '&amp;')
		.replace(/</g, '&lt;')
		.replace(/>/g, '&gt;');

}

/**
 * textareaとハイライト領域のスクロール位置を同期する
 */
document.getElementById('content')?.addEventListener('scroll', () => {

	const textarea = document.getElementById('content');
	const highlightDiv = document.getElementById('contentHighlight');

	highlightDiv.scrollTop = textarea.scrollTop;

});

/**
 * 初期表示時にハイライトを描画する
 */
window.addEventListener('DOMContentLoaded', renderHighlight);
