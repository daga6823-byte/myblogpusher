/**
 * template_cell.js
 *
 * セル選択、セル種類の変更、セル内容の保存を担当する。
 */

import { templateState } from "./template_state.js";
import { callLayoutApi } from "./template_api.js";

/**
 * 選択セルを切り替える。
 */
export function selectCell(cell) {
	if (templateState.selectedCell) {
		templateState.selectedCell.classList.remove("selected");
	}

	templateState.selectedCell = cell;
	cell.classList.add("selected");
}

/**
 * セル表示を更新する。
 */
function updateCellDisplay(
	cell,
	cellType,
	content,
	imageUrl = null,
	imageWidth = null
) {
	cell.className = `template-cell ${cellType.toLowerCase()}`;
	cell.dataset.cellType = cellType;
	cell.replaceChildren();

	if (cellType === "IMAGE" && imageUrl) {
		const image = document.createElement("img");
		image.src = imageUrl;
		image.alt = "テンプレート画像";
		image.style.maxWidth = "100%";
		image.style.maxHeight = "100%";
		image.style.objectFit = "contain";

		if (imageWidth) {
			image.style.width = `${imageWidth}px`;
		}

		cell.appendChild(image);
		return;
	}

	cell.textContent = content;
}

/**
 * セル種類と内容をDBへ保存する。
 */
export async function changeCellType(cellType, values = {}) {
	if (!templateState.selectedCell) {
		alert("編集するセルを選択してください。");
		return;
	}

	const cell = templateState.selectedCell;

	const rowNumber = Number(cell.dataset.row) + 1;
	const columnNumber = Number(cell.dataset.column) + 1;

	const params = {
		cellType: cellType
	};

	if (cellType === "TEXT") {
		params.cellContent = values.cellContent ?? "";
	}

	if (cellType === "VARIABLE") {
		params.variableName = values.variableName ?? "";
	}

	if (cellType === "IMAGE") {
		if (!values.imageAssetId) {
			alert("画像を選択してください。");
			return;
		}

		params.imageAssetId = values.imageAssetId;
		params.imageWidth = values.imageWidth ?? null;
		params.imageHeight = values.imageHeight ?? null;
	}

	try {
		let result;

		if (cell.dataset.cellId) {
			result = await callLayoutApi(
				"/template/api/layout/cell/save",
				{
					cellId: cell.dataset.cellId,
					...params
				}
			);
		} else {
			result = await callLayoutApi(
				"/template/api/layout/cell/create",
				{
					rowNumber: rowNumber,
					columnNumber: columnNumber,
					...params
				}
			);

			if (!result || !result.cellId) {
				throw new Error("セルの登録結果を取得できませんでした。");
			}

			cell.dataset.cellId = result.cellId;
		}

		if (cellType === "IMAGE") {
			cell.dataset.imageAssetId = values.imageAssetId;
			cell.dataset.imageUrl = values.imageUrl ?? "";
		}

		let displayContent = "";

		if (cellType === "TEXT") {
			displayContent = values.cellContent ?? "";
		} else if (cellType === "VARIABLE") {
			displayContent = `{{${values.variableName ?? ""}}}`;
		} else if (cellType === "IMAGE") {
			displayContent = "画像";
		}

		updateCellDisplay(
			cell,
			cellType,
			displayContent,
			values.imageUrl,
			values.imageWidth
		);

	} catch (error) {
		alert(error.message);
	}
}

/**
 * 固定テキストセルを設定する。
 */
function setTextCell() {
	if (!templateState.selectedCell) {
		alert("編集するセルを選択してください。");
		return;
	}

	const content = prompt("セルに表示するテキストを入力してください。");

	if (content === null) {
		return;
	}

	changeCellType("TEXT", {
		cellContent: content
	});
}

/**
 * 変数セルを設定する。
 */
function setVariableCell() {
	if (!templateState.selectedCell) {
		alert("編集するセルを選択してください。");
		return;
	}

	const variableName = prompt("変数名を入力してください。");

	if (variableName === null) {
		return;
	}

	if (!/^[a-zA-Z][a-zA-Z0-9_]*$/.test(variableName)) {
		alert("変数名は半角英数字とアンダースコアで指定してください。");
		return;
	}

	changeCellType("VARIABLE", {
		variableName: variableName
	});
}

/**
 * 空白セルを設定する。
 */
function setEmptyCell() {
	changeCellType("EMPTY");
}

document.addEventListener("DOMContentLoaded", () => {
	document.getElementById("addTextCellButton")
		?.addEventListener("click", setTextCell);

	document.getElementById("addVariableCellButton")
		?.addEventListener("click", setVariableCell);

	document.getElementById("addEmptyCellButton")
		?.addEventListener("click", setEmptyCell);
});