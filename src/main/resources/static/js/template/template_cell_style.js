
/**
 * テンプレートセルの装飾と色履歴を担当する。
 *
 * 文字色、背景色、文字サイズ、太字、文字配置の保存と表示更新、
 * ユーザーごとの色履歴の取得・登録を行う。
 */
import { templateState } from "./template_state.js";
import { callLayoutApi } from "./template_api.js";

let activeColorProperty = null;

/**
 * 色履歴を取得して画面に表示する。
 */
async function loadColorHistory() {
	const container = document.getElementById("templateColorHistory");

	if (!container) {
		return;
	}

	try {
		const response = await fetch("/template/color/history");

		if (!response.ok) {
			throw new Error("色履歴を取得できませんでした。");
		}

		const colors = await response.json();
		container.replaceChildren();

		colors.forEach(color => {
			if (!color.colorCode) {
				return;
			}

			const button = document.createElement("button");
			button.type = "button";
			button.className = "template-color-history-item";
			button.dataset.color = color.colorCode;
			button.title = color.colorCode;
			button.style.backgroundColor = color.colorCode;
			button.setAttribute("aria-label", color.colorCode);

			container.appendChild(button);
		});
	} catch (error) {
		console.error(error);
	}
}

/**
 * 使用した色を履歴に登録する。
 */
async function saveColorHistory(colorCode) {
	if (!colorCode) {
		return;
	}

	try {
		await callLayoutApi(
			"/template/color/save",
			{ colorValue: colorCode }
		);

		await loadColorHistory();
	} catch (error) {
		console.error("色履歴の登録に失敗しました。", error);
	}
}

/**
 * 指定した装飾項目だけを選択セルへ保存する。
 */
async function saveCellStyle(property, value) {
	const cells = templateState.selectedCells;

	if (!cells.length) {
		alert("装飾するセルを選択してください。");
		return;
	}

	if (value === null || value === undefined || value === "") {
		return;
	}

	const params = {};
	params[property] = value;

	try {
		for (const cell of cells) {
			if (!cell.dataset.cellId) {
				throw new Error(
					"未登録のセルが選択されています。先にセルを登録してください。"
				);
			}

			await callLayoutApi(
				"/template/api/layout/cell/save",
				{
					cellId: cell.dataset.cellId,
					...params
				}
			);
		}

		cells.forEach(cell => {
			switch (property) {
				case "textColor":
					cell.style.color = value;
					break;
				case "textStrokeColor":
					cell.style.webkitTextStrokeColor = value;
					cell.style.paintOrder = "stroke fill";
					break;
				case "textStrokeWidth":
					cell.style.webkitTextStrokeWidth = `${value}px`;
					cell.style.paintOrder = "stroke fill";
					break;
				case "backgroundColor":
					cell.style.backgroundColor = value;
					break;

				case "fontSize":
					cell.style.fontSize = `${value}px`;
					break;

				case "fontWeight":
					cell.style.fontWeight = value;
					break;

				case "textAlign":
					cell.style.textAlign = value;
					break;

				default:
					break;
			}
		});
	} catch (error) {
		alert(error.message);
	}
}

/**
 * 色コード入力欄とカラーピッカーを同期する。
 */
function syncColorInputs(property, colorCode) {
	const inputIds = {
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

	const ids = inputIds[property];

	if (!ids) {
		return;
	}

	const picker = document.getElementById(ids.picker);
	const codeInput = document.getElementById(ids.code);

	if (picker) {
		picker.value = colorCode;
	}

	if (codeInput) {
		codeInput.value = colorCode.toUpperCase();
	}
}

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
	const textAlignInput = document.getElementById("cellTextAlign");

	if (textAlignInput) {
		textAlignInput.value = computedStyle.textAlign;
	}
}

/**
 * CSSの色表現を #RRGGBB に変換する。
 */
function toHexColor(color) {

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
 * 色変更を保存する。
 */
async function applyColor(property, colorCode) {
	if (!/^#[0-9A-Fa-f]{6}$/.test(colorCode)) {
		alert("カラーコードは #RRGGBB 形式で入力してください。");
		return;
	}

	const normalizedColor = colorCode.toUpperCase();

	syncColorInputs(property, normalizedColor);

	await saveColorHistory(normalizedColor);
	await saveCellStyle(property, normalizedColor);
}

/**
 * 色履歴から選択した色を適用する。
 */
async function applyHistoryColor(button) {
	const colorCode = button.dataset.color;

	if (!colorCode || !activeColorProperty) {
		return;
	}

	await applyColor(activeColorProperty, colorCode);
}

/**
 * 装飾コントロールと色履歴のイベントを登録する。
 */
document.addEventListener("DOMContentLoaded", () => {
	const textColorInput = document.getElementById("cellTextColor");
	const textColorCodeInput = document.getElementById("cellTextColorCode");

	const backgroundColorInput = document.getElementById("cellBackgroundColor");
	const backgroundColorCodeInput = document.getElementById("cellBackgroundColorCode");

	const textStrokeColorInput = document.getElementById("cellTextStrokeColor");
	const textStrokeColorCodeInput = document.getElementById("cellTextStrokeColorCode");
	const textStrokeWidthInput = document.getElementById("cellTextStrokeWidth");

	const fontSizeInput = document.getElementById("cellFontSize");
	const fontWeightInput = document.getElementById("cellFontWeight");
	const textAlignInput = document.getElementById("cellTextAlign");

	const historyContainer = document.getElementById("templateColorHistory");

	// 文字色のカラーピッカー
	textColorInput?.addEventListener("focus", () => {
		activeColorProperty = "textColor";
	});

	textColorInput?.addEventListener("change", async () => {
		await applyColor("textColor", textColorInput.value);
	});

	// 文字色のカラーコード入力
	textColorCodeInput?.addEventListener("focus", () => {
		activeColorProperty = "textColor";
	});

	textColorCodeInput?.addEventListener("change", async () => {
		await applyColor("textColor", textColorCodeInput.value.trim());
	});

	// 背景色のカラーピッカー
	backgroundColorInput?.addEventListener("focus", () => {
		activeColorProperty = "backgroundColor";
	});

	backgroundColorInput?.addEventListener("change", async () => {
		await applyColor("backgroundColor", backgroundColorInput.value);
	});

	// 背景色のカラーコード入力

	backgroundColorCodeInput?.addEventListener("focus", () => {

		activeColorProperty = "backgroundColor";

	});

	backgroundColorCodeInput?.addEventListener("change", async () => {

		await applyColor(

			"backgroundColor",

			backgroundColorCodeInput.value.trim()

		);

	});

	// 文字の縁取り色：カラーピッカー

	textStrokeColorInput?.addEventListener("focus", () => {

		activeColorProperty = "textStrokeColor";

	});

	textStrokeColorInput?.addEventListener("change", async () => {

		await applyColor(

			"textStrokeColor",

			textStrokeColorInput.value

		);

	});

	// 文字の縁取り色：カラーコード

	textStrokeColorCodeInput?.addEventListener("focus", () => {

		activeColorProperty = "textStrokeColor";

	});

	textStrokeColorCodeInput?.addEventListener("change", async () => {

		await applyColor(

			"textStrokeColor",

			textStrokeColorCodeInput.value.trim()

		);

	});

	// 文字の縁取りの太さ

	textStrokeWidthInput?.addEventListener("change", () => {

		const width = Number(textStrokeWidthInput.value);

		if (!Number.isFinite(width) || width < 0 || width > 3) {

			alert("縁取りの太さは0～3pxで指定してください。");

			textStrokeWidthInput.value = "0";

			return;

		}

		saveCellStyle("textStrokeWidth", width);

	});

	// 文字サイズ
	fontSizeInput?.addEventListener("change", () => {
		saveCellStyle(
			"fontSize",
			fontSizeInput.value
		);
	});

	// 文字の太さ
	fontWeightInput?.addEventListener("change", () => {
		saveCellStyle(
			"fontWeight",
			fontWeightInput.value
		);
	});

	// 文字配置
	textAlignInput?.addEventListener("change", () => {
		saveCellStyle(
			"textAlign",
			textAlignInput.value
		);
	});

	// 色履歴
	historyContainer?.addEventListener("click", event => {
		const button = event.target.closest(
			".template-color-history-item"
		);

		if (button) {
			applyHistoryColor(button);
		}
	});

	loadColorHistory();
});
