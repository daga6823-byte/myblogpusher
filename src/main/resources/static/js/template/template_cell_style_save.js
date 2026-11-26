/**
 * 選択セルへの装飾の保存と、画面表示への反映を担当する。
 *
 * 文字色、縁取り、背景色、文字サイズ、太字、文字配置のうち、
 * 指定した1項目をサーバーへ保存し、保存後にセルの見た目を更新する。
 */
import { templateState } from "./template_state.js";
import { callLayoutApi } from "./template_api.js";
import { TEXT_ALIGN_TO_JUSTIFY } from "./template_color_utils.js";

/**
 * 装飾項目の値をセルの見た目に反映する。
 */
function applyStyleToCell(cell, property, value) {
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
			cell.style.justifyContent = TEXT_ALIGN_TO_JUSTIFY[value];
			break;
		default:
			break;
	}
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

		cells.forEach(cell => applyStyleToCell(cell, property, value));
	} catch (error) {
		alert(error.message);
	}
}