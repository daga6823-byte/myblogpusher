/**
 * template_grid.js
 *
 * グリッドの復元、セル生成、行・列の追加を担当する。
 */

import { templateState } from "./template_state.js";
import { callLayoutApi } from "./template_api.js";
import { selectCell } from "./template_cell.js";

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

	cell.addEventListener("click", () => {
		selectCell(cell);
	});

	return cell;
}

/**
 * DBから取得したレイアウト情報を読み込み、グリッドを復元する。
 */
export function restoreGrid() {
	const grid = templateState.grid;

	const rows = JSON.parse(grid.dataset.rows || "[]");
	const columns = JSON.parse(grid.dataset.columns || "[]");
	const cells = JSON.parse(grid.dataset.cells || "[]");

	templateState.rowCount = rows.length;
	templateState.columnCount = columns.length;
	templateState.selectedCell = null;

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

		if (cellData.fontSize) {
			cell.style.fontSize = `${cellData.fontSize}px`;
		}

		if (cellData.fontWeight) {
			cell.style.fontWeight = cellData.fontWeight;
		}

		if (cellData.textAlign) {
			cell.style.textAlign = cellData.textAlign;
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
				cell.textContent = "";
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

document.addEventListener("DOMContentLoaded", () => {
	const grid = document.getElementById("templateGrid");

	if (!grid) {
		return;
	}

	templateState.grid = grid;

	document.getElementById("addRowButton")
		?.addEventListener("click", addRow);

	document.getElementById("addColumnButton")
		?.addEventListener("click", addColumn);

	restoreGrid();
});