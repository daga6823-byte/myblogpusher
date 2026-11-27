/**
 * 色と文字配置に関する共通処理を担当する。
 *
 * CSSの色表現の変換、カラーピッカーとカラーコード入力欄の同期、
 * 文字配置(left/center/right)とjustify-contentの対応表を提供する。
 */

/**
 * 文字配置とjustify-contentの対応表
 */
export const TEXT_ALIGN_TO_JUSTIFY = {
	left: "flex-start",
	center: "center",
	right: "flex-end"
};

/**
 * 色の装飾項目と、対応する入力欄のID
 */
const COLOR_INPUT_IDS = {
	textColor: {
		picker: "cellTextColor",
		code: "cellTextColorCode"
	},
	backgroundColor: {
		picker: "cellBackgroundColor",
		code: "cellBackgroundColorCode"
	},
	textStrokeColor: {
		picker: "cellTextStrokeColor",
		code: "cellTextStrokeColorCode"
	}
};

/**
 * CSSの色表現を #RRGGBB に変換する。
 */
export function toHexColor(color) {

	const match = color.match(
		/^rgba?\((\d+),\s*(\d+),\s*(\d+)/
	);

	if (!match) {
		return "#000000";
	}

	return "#" + match.slice(1, 4)
		.map(value => Number(value).toString(16).padStart(2, "0"))
		.join("")
		.toUpperCase();
}

/**
 * 色コード入力欄とカラーピッカーを同期する。
 *
 * 未指定の場合、カラーピッカーは表示用の白色にし、
 * カラーコード入力欄は空欄にする。
 */
export function syncColorInputs(property, colorCode) {

	const ids = COLOR_INPUT_IDS[property];

	if (!ids) {
		return;
	}

	const picker = document.getElementById(ids.picker);
	const codeInput = document.getElementById(ids.code);

	const normalizedColor = colorCode
		? colorCode.toUpperCase()
		: "";

	if (picker) {
		picker.value = normalizedColor || "#FFFFFF";
	}

	if (codeInput) {
		codeInput.value = normalizedColor;
	}
}