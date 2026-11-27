/**
 * template_grid_restore.js
 *
 * グリッドの復元を担当する。
 *
 * Thymeleafから渡された行・列・セルの情報を読み込み、
 * グリッドの行列定義、登録済みセル、DB未登録の空きセルを再生成する。
 */
import { templateState } from "./template_state.js";
import { createCell } from "./template_grid_cell.js";
import { createCellFromData } from "./template_grid_cell_render.js";
import { createGridResizeHandles } from "./template_grid_resize_handles.js";

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

		grid.appendChild(createCellFromData(cellData));
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

	// 行・列のサイズ変更ハンドルを生成する。
	createGridResizeHandles();
}