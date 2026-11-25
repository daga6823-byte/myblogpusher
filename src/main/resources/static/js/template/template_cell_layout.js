
/**
 * テンプレートセルのレイアウト操作を担当する。
 *
 * 選択セル範囲の結合と、結合セルの分割を行う。
 */
import { templateState } from "./template_state.js";
import { callLayoutApi } from "./template_api.js";

/**
 * 選択したセル範囲を結合する。
 *
 * 選択範囲が隙間のない矩形であることを確認してから
 * サーバー側の結合APIを呼び出す。
 */
async function mergeCells() {

	const cells = templateState.selectedCells;

	if (cells.length < 2) {
		alert("結合するセルを複数選択してください。");
		return;
	}

	const rows = cells.map(cell => Number(cell.dataset.row) + 1);
	const columns = cells.map(cell => Number(cell.dataset.column) + 1);

	const startRow = Math.min(...rows);
	const startColumn = Math.min(...columns);
	const endRow = Math.max(...rows);
	const endColumn = Math.max(...columns);

	const rowSpan = endRow - startRow + 1;
	const columnSpan = endColumn - startColumn + 1;

	if (cells.length !== rowSpan * columnSpan) {
		alert("結合するセルは隙間なく矩形に選択してください。");
		return;
	}

	try {

		await callLayoutApi(
			"/template/api/layout/cell/merge",
			{
				startRow,
				startColumn,
				rowSpan,
				columnSpan
			},
			true
		);

	} catch (error) {
		alert(error.message);
	}
}

/**
 * 選択した結合セルを分割する。
 */
async function splitCell() {

	const cell = templateState.selectedCell;

	if (!cell || !cell.dataset.cellId) {
		alert("分割する結合セルを選択してください。");
		return;
	}

	try {

		await callLayoutApi(
			"/template/api/layout/cell/split",
			{
				cellId: cell.dataset.cellId
			},
			true
		);

	} catch (error) {
		alert(error.message);
	}
}

/**
 * 結合・分割ボタンを登録する。
 */
document.addEventListener("DOMContentLoaded", () => {

	document.getElementById("mergeCellsButton")
		?.addEventListener("click", mergeCells);

	document.getElementById("splitCellButton")
		?.addEventListener("click", splitCell);

});
