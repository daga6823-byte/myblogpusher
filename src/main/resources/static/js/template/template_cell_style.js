
/**
 * テンプレートセルの装飾と色履歴を担当する。
 *
 * 文字色、背景色、文字サイズ、太字の保存と表示更新、
 * ユーザーごとの色履歴の取得・登録を行う。
 */
import { templateState } from "./template_state.js";
import { callLayoutApi } from "./template_api.js";

let activeColorInput = null;

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

				case "backgroundColor":
					cell.style.backgroundColor = value;
					break;

				case "fontSize":
					cell.style.fontSize = `${value}px`;
					break;

				case "fontWeight":
					cell.style.fontWeight = value;
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
 * 色履歴から選択した色を入力欄へ適用する。
 */
async function applyHistoryColor(button) {

	const colorCode = button.dataset.color;

	if (!colorCode || !activeColorInput) {
		return;
	}

	activeColorInput.value = colorCode;

	const property =
		activeColorInput.id === "cellTextColor"
			? "textColor"
			: "backgroundColor";

	await saveCellStyle(property, colorCode);
}

/**
 * 装飾コントロールと色履歴のイベントを登録する。
 */
document.addEventListener("DOMContentLoaded", () => {

	const textColorInput = document.getElementById("cellTextColor");
	const backgroundColorInput = document.getElementById("cellBackgroundColor");
	const fontSizeInput = document.getElementById("cellFontSize");
	const fontWeightInput = document.getElementById("cellFontWeight");
	const historyContainer = document.getElementById("templateColorHistory");

	textColorInput?.addEventListener("focus", () => {
		activeColorInput = textColorInput;
	});

	backgroundColorInput?.addEventListener("focus", () => {
		activeColorInput = backgroundColorInput;
	});

	backgroundColorInput?.addEventListener("focus", () => {
		backgroundColorInput.dataset.active = "true";
		textColorInput?.removeAttribute("data-active");
	});

	textColorInput?.addEventListener("change", async () => {

		await saveColorHistory(textColorInput.value);

		await saveCellStyle(
			"textColor",
			textColorInput.value
		);
	});

	backgroundColorInput?.addEventListener("change", async () => {

		await saveColorHistory(backgroundColorInput.value);

		await saveCellStyle(
			"backgroundColor",
			backgroundColorInput.value
		);
	});

	fontSizeInput?.addEventListener("change", () => {

		saveCellStyle(
			"fontSize",
			fontSizeInput.value
		);
	});

	fontWeightInput?.addEventListener("change", () => {

		saveCellStyle(
			"fontWeight",
			fontWeightInput.value
		);
	});

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
