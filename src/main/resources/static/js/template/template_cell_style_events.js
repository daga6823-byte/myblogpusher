/**
 * 装飾コントロールと色履歴のイベント登録を担当する。
 *
 * 文字色、背景色、縁取り、文字サイズ、太字、文字配置の操作を受け取り、
 * 色の適用(履歴登録と保存)を行う。
 * 保存はtemplate_cell_style_save.js、色履歴はtemplate_color_history.jsへ委譲する。
 */
import { syncColorInputs } from "./template_color_utils.js";
import { loadColorHistory, saveColorHistory } from "./template_color_history.js";
import { saveCellStyle } from "./template_cell_style_save.js";

// 色履歴をクリックしたときに適用する装飾項目。最後にフォーカスした色入力で決まる。
let activeColorProperty = null;

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
 * カラーピッカーとカラーコード入力欄に、フォーカスと変更のイベントを登録する。
 */
function bindColorInput(inputId, property) {
	const input = document.getElementById(inputId);

	input?.addEventListener("focus", () => {
		activeColorProperty = property;
	});

	input?.addEventListener("change", async () => {
		await applyColor(property, input.value.trim());
	});
}

/**
 * 文字の縁取りの太さの入力イベントを登録する。
 */
function bindTextStrokeWidth() {
	const textStrokeWidthInput = document.getElementById("cellTextStrokeWidth");

	textStrokeWidthInput?.addEventListener("change", () => {

		const width = Number(textStrokeWidthInput.value);

		if (!Number.isFinite(width) || width < 0 || width > 3) {
			alert("縁取りの太さは0～3pxで指定してください。");
			textStrokeWidthInput.value = "0";
			return;
		}

		saveCellStyle("textStrokeWidth", width);
	});
}

/**
 * 文字サイズ・文字の太さの入力イベントを登録する。
 */
function bindFontInputs() {
	const fontSizeInput = document.getElementById("cellFontSize");
	const fontWeightInput = document.getElementById("cellFontWeight");

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
}

/**
 * 文字配置ボタンのクリックイベントを登録する。
 */
function bindTextAlign() {
	const textAlignGroup = document.getElementById("cellTextAlignGroup");

	textAlignGroup?.addEventListener("click", async event => {
		const button = event.target.closest(".text-align-button");

		if (!button || !textAlignGroup.contains(button)) {
			return;
		}

		const align = button.dataset.align;

		if (!["left", "center", "right"].includes(align)) {
			return;
		}

		await saveCellStyle("textAlign", align);

		textAlignGroup.querySelectorAll(".text-align-button")
			.forEach(item => {
				item.setAttribute(
					"aria-pressed",
					String(item.dataset.align === align)
				);
			});
	});
}

/**
 * 色履歴のクリックイベントを登録する。
 */
function bindColorHistory() {
	const historyContainer = document.getElementById("templateColorHistory");

	historyContainer?.addEventListener("click", event => {
		const button = event.target.closest(
			".template-color-history-item"
		);

		if (button) {
			applyHistoryColor(button);
		}
	});
}

/**
 * 装飾コントロールと色履歴のイベントを登録する。
 */
document.addEventListener("DOMContentLoaded", () => {

	// 文字色
	bindColorInput("cellTextColor", "textColor");
	bindColorInput("cellTextColorCode", "textColor");

	// 背景色
	bindColorInput("cellBackgroundColor", "backgroundColor");
	bindColorInput("cellBackgroundColorCode", "backgroundColor");

	// 文字の縁取り色
	bindColorInput("cellTextStrokeColor", "textStrokeColor");
	bindColorInput("cellTextStrokeColorCode", "textStrokeColor");

	bindTextStrokeWidth();
	bindFontInputs();
	bindTextAlign();
	bindColorHistory();

	loadColorHistory();
});