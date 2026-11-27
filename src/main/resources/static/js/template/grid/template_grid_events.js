/**
 * template_grid_events.js
 *
 * 画面読み込み時のグリッド初期化とイベント登録を担当する。
 *
 * グリッド要素をtemplateStateへ登録し、行・列の追加削除ボタンを結び付け、
 * グリッドを復元する。ウィンドウサイズが変わったときはハンドル位置を更新する。
 */
import { templateState } from "./template_state.js";
import { restoreGrid } from "./template_grid_restore.js";
import { updateGridResizeHandlePositions } from "./template_grid_resize_handles.js";
import {
	addRow,
	addColumn,
	deleteRow,
	deleteColumn
} from "./template_grid_row_column.js";

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

	restoreGrid();

	window.addEventListener(
		"resize",
		updateGridResizeHandlePositions
	);
});