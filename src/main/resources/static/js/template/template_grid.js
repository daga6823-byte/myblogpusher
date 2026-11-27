/**
 * template_grid.js
 *
 * グリッドの復元、セル生成、行・列の追加を担当する。
 */

import { templateState } from "./template_state.js";
import { callLayoutApi } from "./template_api.js";
import { selectCell } from "../facade/template_cell_facade.js";
import { TEXT_ALIGN_TO_JUSTIFY } from "./template_color_utils.js";

/**
 * セル要素を生成する。
 */
export function createCell(row, column) {
	const cell = document.createElement("div");

	cell.className = "template-cell empty";
	cell.dataset.row = row;
	cell.dataset.column = column;
	cell.dataset.cellType = "EMPTY";

	cell.style.gridRow = `${row + 1}`;
	cell.style.gridColumn = `${column + 1}`;
	cell.textContent = "空白";

	cell.addEventListener("click", (event) => {
		selectCell(cell, event);
	});

	return cell;
}

/**
 * DBから取得したレイアウト情報を読み込み、グリッドを復元する。
 */
export function restoreGrid() {
	const grid = templateState.grid;

	// Thymeleafから渡されるレイアウト情報を取得する
	const rows = JSON.parse(
		grid.dataset.rowsJson || grid.dataset.rows || "[]"
	);

	const columns = JSON.parse(
		grid.dataset.columnsJson || grid.dataset.columns || "[]"
	);

	const cells = JSON.parse(
		grid.dataset.cellsJson || grid.dataset.cells || "[]"
	);

	templateState.rowCount = rows.length;
	templateState.columnCount = columns.length;
	templateState.selectedCell = null;
	templateState.selectedCells = [];

	// セルだけを再生成する。背景画像のプレビュー設定は維持する。
	grid.replaceChildren();

	if (templateState.rowCount === 0 || templateState.columnCount === 0) {
		return;
	}

	const columnWidths = columns.map(column => {
		const width = column.width ?? 1;
		const unit = column.widthUnit ?? "fr";
		return `${width}${unit}`;
	});

	grid.style.gridTemplateColumns = columnWidths.join(" ");

	const rowHeights = rows.map(row => {
		const height = row.height ?? "auto";
		const unit = row.heightUnit ?? "auto";
		return unit === "auto" ? "auto" : `${height}${unit}`;
	});

	grid.style.gridTemplateRows = rowHeights.join(" ");
	grid.style.setProperty("--column-count", templateState.columnCount);

	const occupied = new Set();

	cells.forEach(cellData => {
		const row = cellData.rowNumber - 1;
		const column = cellData.columnNumber - 1;
		const rowSpan = cellData.rowSpan ?? 1;
		const columnSpan = cellData.columnSpan ?? 1;

		for (let r = row; r < row + rowSpan; r++) {
			for (let c = column; c < column + columnSpan; c++) {
				occupied.add(`${r}:${c}`);
			}
		}

		const cell = createCell(row, column);

		cell.dataset.cellId = cellData.cellId;
		cell.dataset.cellType = cellData.cellType;
		cell.dataset.imageAssetId = cellData.imageAssetId ?? "";

		cell.style.gridRow =
			`${cellData.rowNumber} / span ${rowSpan}`;

		cell.style.gridColumn =
			`${cellData.columnNumber} / span ${columnSpan}`;

		if (cellData.backgroundColor) {
			cell.style.backgroundColor = cellData.backgroundColor;
		}

		if (cellData.textColor) {

			cell.style.color = cellData.textColor;

		}

		// 文字の縁取り色
		if (cellData.textStrokeColor) {

			cell.style.webkitTextStrokeColor = cellData.textStrokeColor;

		}

		// 文字の縁取りの太さ
		if (cellData.textStrokeWidth != null) {

			cell.style.webkitTextStrokeWidth =
				`${cellData.textStrokeWidth}px`;

			cell.style.paintOrder = "stroke fill";

		}

		if (cellData.fontSize) {

			cell.style.fontSize = `${cellData.fontSize}px`;

		}

		if (cellData.fontWeight) {
			cell.style.fontWeight = cellData.fontWeight;
		}

		if (cellData.textAlign) {

			cell.style.textAlign = cellData.textAlign;

			cell.style.justifyContent =
				TEXT_ALIGN_TO_JUSTIFY[cellData.textAlign]
				?? TEXT_ALIGN_TO_JUSTIFY.center;

		}

		if (cellData.verticalAlign) {
			cell.style.alignItems = cellData.verticalAlign;
		}

		if (cellData.padding != null) {
			const unit = cellData.paddingUnit ?? "px";
			cell.style.padding = `${cellData.padding}${unit}`;
		}

		switch (cellData.cellType) {
			case "TEXT":
				cell.textContent = cellData.cellContent ?? "";
				break;

			case "VARIABLE":
				cell.textContent =
					`{{${cellData.variableName ?? ""}}}`;
				break;

			case "IMAGE":
				if (cellData.imageUrl) {
					const image = document.createElement("img");

					image.src = cellData.imageUrl;
					image.alt = "テンプレート画像";
					image.style.maxWidth = "100%";
					image.style.maxHeight = "100%";
					image.style.objectFit = "contain";

					if (cellData.imageWidth) {
						image.style.width = `${cellData.imageWidth}px`;
					}

					cell.replaceChildren(image);
				} else {
					cell.textContent = "画像";
				}
				break;

			case "EMPTY":

				cell.textContent = "空白";

				break;
		}

		cell.className =
			`template-cell ${cellData.cellType.toLowerCase()}`;

		grid.appendChild(cell);
	});

	// DB未登録の空きセルを画面上に生成する。
	for (let row = 0; row < templateState.rowCount; row++) {
		for (let column = 0; column < templateState.columnCount; column++) {
			if (occupied.has(`${row}:${column}`)) {
				continue;
			}

			grid.appendChild(createCell(row, column));
		}
	}
}

/**
 * 最後の行に新しい行を追加する。
 */
async function addRow() {
	try {
		await callLayoutApi(
			"/template/api/layout/row/insert",
			{
				rowNumber: templateState.rowCount + 1
			},
			true
		);
	} catch (error) {
		alert(error.message);
	}
}

/**
 * 最後の列に新しい列を追加する。
 */
async function addColumn() {
	try {
		await callLayoutApi(
			"/template/api/layout/column/insert",
			{
				columnNumber: templateState.columnCount + 1
			},
			true
		);
	} catch (error) {
		alert(error.message);
	}
}

/**
 * 選択セルが属する行の高さを更新する。
 */
async function updateRowHeight() {

	const selectedCell = templateState.selectedCell;

	if (!selectedCell) {
		alert("高さを変更する行のセルを選択してください。");
		return;
	}

	const heightInput = document.getElementById("rowHeightInput");
	const heightUnit = document.getElementById("rowHeightUnit")?.value;

	const height = heightInput?.value.trim() ?? "";
	const rowNumber = Number(selectedCell.dataset.row) + 1;

	try {
		await callLayoutApi(
			"/template/api/layout/row/size",
			{
				rowNumber,
				height,
				heightUnit: height === "" ? "auto" : heightUnit
			}
		);

		const rows = JSON.parse(
			templateState.grid.dataset.rowsJson
			|| templateState.grid.dataset.rows
			|| "[]"
		);

		const row = rows.find(
			item => Number(item.rowNumber) === rowNumber
		);

		if (!row) {
			throw new Error("更新対象の行が見つかりません。");
		}

		row.height = height === "" ? null : Number(height);
		row.heightUnit = height === "" ? "auto" : heightUnit;

		templateState.grid.dataset.rowsJson = JSON.stringify(rows);

		templateState.grid.style.gridTemplateRows =
			rows.map(item => {
				if (item.height == null) {
					return "auto";
				}

				return `${item.height}${item.heightUnit ?? "px"}`;
			}).join(" ");

	} catch (error) {
		alert(error.message);
	}
}

/**
 * 選択セルが属する列の幅を更新する。
 */
async function updateColumnWidth() {

	const selectedCell = templateState.selectedCell;

	if (!selectedCell) {
		alert("幅を変更する列のセルを選択してください。");
		return;
	}

	const widthInput = document.getElementById("columnWidthInput");
	const widthUnit = document.getElementById("columnWidthUnit")?.value;

	const width = widthInput?.value.trim() ?? "";
	const columnNumber = Number(selectedCell.dataset.column) + 1;

	try {
		await callLayoutApi(
			"/template/api/layout/column/size",
			{
				columnNumber,
				width,
				widthUnit: width === "" ? "auto" : widthUnit
			}
		);

		const columns = JSON.parse(
			templateState.grid.dataset.columnsJson
			|| templateState.grid.dataset.columns
			|| "[]"
		);

		const column = columns.find(
			item => Number(item.columnNumber) === columnNumber
		);

		if (!column) {
			throw new Error("更新対象の列が見つかりません。");
		}

		column.width = width === "" ? null : Number(width);
		column.widthUnit = width === "" ? "auto" : widthUnit;

		templateState.grid.dataset.columnsJson =
			JSON.stringify(columns);

		templateState.grid.style.gridTemplateColumns =
			columns.map(item => {
				if (item.width == null) {
					return "auto";
				}

				return `${item.width}${item.widthUnit ?? "px"}`;
			}).join(" ");

	} catch (error) {
		alert(error.message);
	}
}

document.addEventListener("DOMContentLoaded", () => {
	const grid = document.getElementById("templateGrid");

	if (!grid) {
		return;
	}

	templateState.grid = grid;

	document.getElementById("addRowButton")
		?.addEventListener("click", addRow);

	document.getElementById("deleteRowButton")
		?.addEventListener("click", deleteRow);

	document.getElementById("addColumnButton")
		?.addEventListener("click", addColumn);

	document.getElementById("deleteColumnButton")
		?.addEventListener("click", deleteColumn);

	document.getElementById("applyRowHeightButton")
		?.addEventListener("click", updateRowHeight);

	document.getElementById("applyColumnWidthButton")
		?.addEventListener("click", updateColumnWidth);

	restoreGrid();
});

/**
 * 選択セルが属する行を削除する。
 */
async function deleteRow() {
	const selectedCell = templateState.selectedCell;

	if (!selectedCell) {
		alert("削除する行のセルを選択してください。");
		return;
	}

	const rowNumber = Number(selectedCell.dataset.row) + 1;

	if (!confirm(`${rowNumber}行目を削除しますか？`)) {
		return;
	}

	try {
		await callLayoutApi(
			"/template/api/layout/row/delete",
			{ rowNumber },
			true
		);
	} catch (error) {
		alert(error.message);
	}
}

/**
 * 選択セルが属する列を削除する。
 */
async function deleteColumn() {
	const selectedCell = templateState.selectedCell;

	if (!selectedCell) {
		alert("削除する列のセルを選択してください。");
		return;
	}

	const columnNumber = Number(selectedCell.dataset.column) + 1;

	if (!confirm(`${columnNumber}列目を削除しますか？`)) {
		return;
	}

	try {
		await callLayoutApi(
			"/template/api/layout/column/delete",
			{ columnNumber },
			true
		);
	} catch (error) {
		alert(error.message);
	}
}
