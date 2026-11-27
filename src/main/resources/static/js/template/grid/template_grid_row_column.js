/**
 * template_grid_row_column.js
 *
 * グリッドの行・列の追加と削除を担当する。
 *
 * 追加は最後の行・列へ、削除は選択セルが属する行・列へ行う。
 * 処理後は callLayoutApi の第3引数(true)により画面を再読み込みする。
 */
import { templateState } from "../template_state.js";
import { callLayoutApi } from "../template_api.js";

/**
 * 最後の行に新しい行を追加する。
 */
export async function addRow() {
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
export async function addColumn() {
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
 * 選択セルが属する行を削除する。
 */
export async function deleteRow() {
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
export async function deleteColumn() {
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