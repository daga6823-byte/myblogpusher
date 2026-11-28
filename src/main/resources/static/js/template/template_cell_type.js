
/**
 * テンプレートセルの種類と内容の管理を担当する。
 *
 * TEXT・VARIABLE・EMPTY・IMAGE の設定とDB保存、
 * 保存後のセル表示更新を行う。
 */
import { templateState } from "./template_state.js";
import { callLayoutApi } from "./template_api.js";

/**
 * セル表示を更新する。
 */
function updateCellDisplay(
	cell,
	cellType,
	content,
	imageUrl = null,
	imageWidth = null,
	variableName = null
) {
	cell.className = `template-cell ${cellType.toLowerCase()}`;
	cell.dataset.cellType = cellType;

	delete cell.dataset.cellContent;
	delete cell.dataset.variableName;

	cell.replaceChildren();

	if (cellType === "TEXT") {
		cell.dataset.cellContent = content ?? "";
		cell.textContent = content ?? "";
		return;
	}

	if (cellType === "VARIABLE") {
		cell.dataset.variableName = variableName ?? "";
		cell.textContent = `{{${variableName ?? ""}}}`;
		return;
	}

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

	cell.textContent = content ?? "";
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
					rowNumber,
					columnNumber,
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
 * 固定テキストセルの編集欄を表示する。
 */
function setTextCell() {
	if (!templateState.selectedCell) {
		alert("編集するセルを選択してください。");
		return;
	}

	const cell = templateState.selectedCell;

	const editor = document.getElementById("cellContentEditor");
	const textEditor = document.getElementById("cellTextEditor");
	const variableEditor = document.getElementById("cellVariableEditor");
	const textInput = document.getElementById("cellTextContent");

	const currentContent =
		cell.dataset.cellType === "TEXT"
			? cell.dataset.cellContent ?? cell.textContent
			: "";

	editor.hidden = false;
	textEditor.hidden = false;
	variableEditor.hidden = true;

	textInput.value = currentContent;
}

/**
 * 変数セルの編集欄を表示する。
 */
function setVariableCell() {
	if (!templateState.selectedCell) {
		alert("編集するセルを選択してください。");
		return;
	}

	const cell = templateState.selectedCell;

	const editor = document.getElementById("cellContentEditor");
	const textEditor = document.getElementById("cellTextEditor");
	const variableEditor = document.getElementById("cellVariableEditor");
	const variableInput = document.getElementById("cellVariableName");

	const currentVariableName =
		cell.dataset.cellType === "VARIABLE"
			? cell.dataset.variableName
			?? cell.textContent.replace(/^\{\{|\}\}$/g, "")
			: "";

	editor.hidden = false;
	textEditor.hidden = true;
	variableEditor.hidden = false;

	variableInput.value = currentVariableName;
}

/**
 * 空白セルを設定する。
 */
function setEmptyCell() {
	changeCellType("EMPTY");
}

/**
 * セル種類変更ボタンを登録する。
 */
document.addEventListener("DOMContentLoaded", () => {
	document.getElementById("updateCellContentButton")
		?.addEventListener("click", async () => {
			const cell = templateState.selectedCell;

			if (!cell) {
				alert("編集するセルを選択してください。");
				return;
			}

			const textEditor =
				document.getElementById("cellTextEditor");

			const variableEditor =
				document.getElementById("cellVariableEditor");

			if (!textEditor.hidden) {
				await changeCellType("TEXT", {
					cellContent:
						document.getElementById("cellTextContent").value
				});

			} else if (!variableEditor.hidden) {
				const variableName =
					document.getElementById("cellVariableName").value;

				if (!/^[a-zA-Z][a-zA-Z0-9_]*$/.test(variableName)) {
					alert("変数名は半角英数字とアンダースコアで指定してください。");
					return;
				}

				await changeCellType("VARIABLE", {
					variableName
				});
			}
		});
});
