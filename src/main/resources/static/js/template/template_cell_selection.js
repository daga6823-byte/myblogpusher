
/**
 * テンプレートセルの選択管理を担当する。
 *
 * 通常クリックによる単一選択と、
 * Ctrl / Commandクリックによる複数選択を管理する。
 */
import { templateState } from "./template_state.js";
import { restoreCellStyle } from "../facade/template_cell_style_facade.js";

/**
 * 選択セルが属する行高・列幅をツールバーへ反映する。
 */
function restoreSelectedCellSize(cell) {

	const rowHeightInput = document.getElementById("rowHeightInput");
	const rowHeightUnit = document.getElementById("rowHeightUnit");
	const columnWidthInput = document.getElementById("columnWidthInput");
	const columnWidthUnit = document.getElementById("columnWidthUnit");

	if (!cell) {
		if (rowHeightInput) rowHeightInput.value = "";
		if (columnWidthInput) columnWidthInput.value = "";
		return;
	}

	const grid = templateState.grid;

	try {
		const rows = JSON.parse(
			grid.dataset.rowsJson || grid.dataset.rows || "[]"
		);

		const columns = JSON.parse(
			grid.dataset.columnsJson || grid.dataset.columns || "[]"
		);

		const rowNumber = Number(cell.dataset.row) + 1;
		const columnNumber = Number(cell.dataset.column) + 1;

		const row = rows.find(item => item.rowNumber === rowNumber);
		const column = columns.find(item => item.columnNumber === columnNumber);

		if (rowHeightInput) {
			rowHeightInput.value = row?.height ?? "";
		}

		if (rowHeightUnit) {
			rowHeightUnit.value =
				row?.heightUnit && row.heightUnit !== "auto"
					? row.heightUnit
					: "px";
		}

		if (columnWidthInput) {
			columnWidthInput.value = column?.width ?? "";
		}

		if (columnWidthUnit) {
			columnWidthUnit.value =
				column?.widthUnit && column.widthUnit !== "auto"
					? column.widthUnit
					: "fr";
		}

	} catch (error) {
		console.error("行高・列幅の復元に失敗しました。", error);
	}
}

/**
 * セル選択を切り替える。
 *
 * @param {HTMLElement} cell 選択対象セル
 * @param {MouseEvent|null} event クリックイベント
 */
export function selectCell(cell, event = null) {

	if (!cell || !templateState.grid) {
		return;
	}

	const multiSelect = event?.ctrlKey || event?.metaKey;

	if (!multiSelect) {

		templateState.selectedCells.forEach(selected => {
			selected.classList.remove("selected");
		});

		templateState.selectedCells = [cell];

	} else {

		const index = templateState.selectedCells.indexOf(cell);

		if (index >= 0) {
			templateState.selectedCells.splice(index, 1);
		} else {
			templateState.selectedCells.push(cell);
		}
	}

	templateState.grid
		.querySelectorAll(".template-cell.selected")
		.forEach(selected => selected.classList.remove("selected"));

	templateState.selectedCells.forEach(selected => {
		selected.classList.add("selected");
	});

	templateState.selectedCell =
		templateState.selectedCells[
		templateState.selectedCells.length - 1
		] ?? null;

	// 最後に選択したセルの装飾値をツールバーへ反映する。
	restoreCellStyle(templateState.selectedCell);

	// 選択セルが属する行高・列幅をツールバーへ反映する。
	restoreSelectedCellSize(templateState.selectedCell);
}
