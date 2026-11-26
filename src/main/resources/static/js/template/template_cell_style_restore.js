/**
 * 選択セルの装飾値をツールバーへ反映する処理を担当する。
 *
 * セルの現在の見た目から、色・縁取り・文字サイズ・太さ・配置を読み取り、
 * 各入力欄とボタンの状態を更新する。
 */
import {
	TEXT_ALIGN_TO_JUSTIFY,
	syncColorInputs,
	toHexColor
} from "./template_color_utils.js";

/**
 * 選択セルの装飾値をツールバーへ反映する。
 *
 * 複数選択時は最後に選択したセルの値を表示する。
 */
export function restoreCellStyle(cell) {

	if (!cell) {
		return;
	}

	const computedStyle = window.getComputedStyle(cell);

	// 文字色
	syncColorInputs(
		"textColor",
		toHexColor(computedStyle.color)
	);

	// 背景色
	syncColorInputs(
		"backgroundColor",
		toHexColor(computedStyle.backgroundColor)
	);

	// 文字の縁取り色
	const strokeColor = computedStyle.webkitTextStrokeColor;

	if (strokeColor) {
		syncColorInputs(
			"textStrokeColor",
			toHexColor(strokeColor)
		);
	}

	// 文字の縁取りの太さ
	const strokeWidth = parseFloat(
		computedStyle.webkitTextStrokeWidth
	);

	const strokeWidthInput =
		document.getElementById("cellTextStrokeWidth");

	if (strokeWidthInput && Number.isFinite(strokeWidth)) {
		strokeWidthInput.value = String(strokeWidth);
	}

	// 文字サイズ
	const fontSizeInput = document.getElementById("cellFontSize");

	if (fontSizeInput) {
		fontSizeInput.value = String(
			parseFloat(computedStyle.fontSize)
		);
	}

	// 文字の太さ
	const fontWeightInput = document.getElementById("cellFontWeight");

	if (fontWeightInput) {
		fontWeightInput.value =
			Number(computedStyle.fontWeight) >= 600
				? "bold"
				: "normal";
	}

	// 文字配置
	const textAlignGroup = document.getElementById("cellTextAlignGroup");

	if (textAlignGroup) {
		const textAlign = ["left", "center", "right"].find(
			align => cell.style.justifyContent === TEXT_ALIGN_TO_JUSTIFY[align]
		) ?? computedStyle.textAlign;

		textAlignGroup.querySelectorAll(".text-align-button")
			.forEach(button => {
				button.setAttribute(
					"aria-pressed",
					String(button.dataset.align === textAlign)
				);
			});
	}
}