/**
 * template_grid_cell.js
 *
 * 空セル要素の生成を担当する。
 */
import { selectCell } from "../../facade/template_cell_facade.js";

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