
/**
 * テンプレートセルの選択管理を担当する。
 *
 * 通常クリックによる単一選択と、
 * Ctrl / Commandクリックによる複数選択を管理する。
 */
import { templateState } from "./template_state.js";
import { restoreCellStyle } from "./template_cell_style.js";

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
}
