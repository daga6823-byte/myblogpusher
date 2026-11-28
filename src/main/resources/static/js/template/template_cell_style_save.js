/**
 * 選択セルへの装飾の保存と、画面表示への反映を担当する。
 *
 * 文字色、縁取り、背景色、文字サイズ、太字、文字配置、
 * 縦位置のうち、
 * 指定した1項目をサーバーへ保存し、保存後にセルの見た目を更新する。
 */
import { templateState } from "./template_state.js";
import { callLayoutApi } from "./template_api.js";
import { TEXT_ALIGN_TO_JUSTIFY } from "./template_color_utils.js";

/**
 * 装飾項目の値をセルの見た目に反映する。
 */
function applyStyleToCell(cell, property, value) {

	if (property === "textStrokeColor") {
		cell.dataset.textStrokeColor = value;
	}

	if (property === "textStrokeWidth") {
		cell.dataset.textStrokeWidth = value;
	}

	switch (property) {
		case "textColor":
			cell.style.color = value;
			break;
		case "textStrokeColor":
		case "textStrokeWidth":
			applyTextStrokeShadow(cell);
			break;

		case "backgroundColor":
			cell.style.backgroundColor = value ?? "transparent";
			break;

		case "fontSize":

			cell.style.fontSize = value === null
				? ""
				: `${value}px`;

			break;

		case "fontWeight":
			cell.style.fontWeight = value;
			break;

		case "textAlign":

			cell.style.textAlign = value;

			cell.style.justifyContent = TEXT_ALIGN_TO_JUSTIFY[value];

			break;

		case "verticalAlign":

			cell.style.alignItems = value;

			break;

		default:

			break;
	}
}

/**
 * 文字本体を侵食しない縁取りをtext-shadowで描画する。
 */
function applyTextStrokeShadow(cell) {

	const color = cell.dataset.textStrokeColor;
	const width = Number(cell.dataset.textStrokeWidth);

	if (!color || !Number.isFinite(width) || width <= 0) {
		cell.style.textShadow = "";
		return;
	}

	const shadows = [];

	for (let i = 0; i < 16; i++) {

		const angle = (2 * Math.PI * i) / 16;

		const offsetX = (Math.cos(angle) * width).toFixed(2);
		const offsetY = (Math.sin(angle) * width).toFixed(2);

		shadows.push(
			`${offsetX}px ${offsetY}px 0 ${color}`
		);
	}

	cell.style.textShadow = shadows.join(", ");
}

/**
 * 指定した装飾項目だけを選択セルへ保存する。
 */
export async function saveCellStyle(property, value) {
	const cells = templateState.selectedCells;

	if (!cells.length) {
		alert("装飾するセルを選択してください。");
		return;
	}

	if (
		value === undefined ||
		(value === "" && property !== "backgroundColor")
	) {
		return;
	}

	const params = {};

	if (property === "fontSize" && value === null) {
		params.clearFontSize = true;
	} else if (
		property === "backgroundColor" &&
		(value === null || value === "")
	) {
		params.clearBackgroundColor = true;
	} else {
		params[property] = value;
	}

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

		cells.forEach(cell => applyStyleToCell(cell, property, value));
	} catch (error) {
		alert(error.message);
	}
}