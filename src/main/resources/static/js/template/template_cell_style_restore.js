/**
 * 選択セルの装飾値をツールバーへ反映する処理を担当する。
 *
 * セルの現在の見た目から、色・縁取り・文字サイズ・太さ・横位置・縦位置を読み取り、
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
	// セル自身に個別指定がない場合は未指定として表示する。
	const backgroundColor = cell.style.backgroundColor;

	syncColorInputs(
		"backgroundColor",
		backgroundColor
			? toHexColor(backgroundColor)
			: ""
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
		// セル自身に文字サイズが指定されていない場合は空欄にする。
		fontSizeInput.value = cell.style.fontSize
			? String(parseFloat(cell.style.fontSize))
			: "";
	}

	// 文字の太さ
	const fontWeightInput = document.getElementById("cellFontWeight");

	if (fontWeightInput) {
		fontWeightInput.value =
			cell.style.fontWeight === "bold"
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

	// 文字の縦位置

	const verticalAlignGroup = document.getElementById(
		"cellVerticalAlignGroup"
	);

	if (verticalAlignGroup) {

		const verticalAlign = [
			"flex-start",
			"center",
			"flex-end"
		].includes(cell.style.alignItems)
			? cell.style.alignItems
			: "center";

		verticalAlignGroup.querySelectorAll(
			".vertical-align-button"
		).forEach(button => {

			button.setAttribute(
				"aria-pressed",
				String(
					button.dataset.verticalAlign === verticalAlign
				)
			);

		});

	}
}