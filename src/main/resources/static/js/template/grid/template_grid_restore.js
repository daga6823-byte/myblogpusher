/**
 * template_grid_restore.js
 *
 * グリッドの復元を担当する。
 *
 * Thymeleafから渡された行・列・セルの情報を読み込み、
 * グリッドの行列定義、登録済みセル、DB未登録の空きセルを再生成する。
 */
import { templateState } from "../template_state.js";
import { createCell } from "./template_grid_cell.js";
import { createCellFromData } from "./template_grid_cell_render.js";
import { createGridResizeHandles } from "./template_grid_resize_handles.js";

/**
 * DBから取得したレイアウト情報を読み込み、グリッドを復元する。
 *
 * 引数なしの場合：
 *   templateState.grid を対象に編集画面用のグリッドを復元する。
 *
 * オプション指定時：
 *   指定されたグリッドとレイアウト情報を使ってプレビューを復元する。
 *   セル選択イベントやサイズ変更ハンドルは生成しない。
 *
 * テンプレート選択画面でも編集画面と同じ行列・セル描画処理を
 * 使用するための共通処理。
 */
export function restoreGrid(options = null) {

    const isPreview = options !== null;

    const grid = isPreview
        ? options.grid
        : templateState.grid;

    const rows = isPreview
        ? options.rows
        : JSON.parse(
            grid.dataset.rowsJson || grid.dataset.rows || "[]"
        );

    const columns = isPreview
        ? options.columns
        : JSON.parse(
            grid.dataset.columnsJson || grid.dataset.columns || "[]"
        );

    const cells = isPreview
        ? options.cells
        : JSON.parse(
            grid.dataset.cellsJson || grid.dataset.cells || "[]"
        );

    if (!grid) {
        throw new Error("グリッド要素が見つかりません。");
    }

    if (!isPreview) {
        templateState.rowCount = rows.length;
        templateState.columnCount = columns.length;
        templateState.selectedCell = null;
        templateState.selectedCells = [];
    }

    const rowCount = rows.length;
    const columnCount = columns.length;

    // セルだけを再生成する。背景画像・背景色の設定は維持する。
    grid.replaceChildren();

    if (rowCount === 0 || columnCount === 0) {
        return;
    }

    const PREVIEW_SCALE = 5;

    const columnWidths = columns.map(column => {
        const width = column.width ?? 1;
        const unit = column.widthUnit ?? "fr";

        return unit === "px"
            ? `${width * PREVIEW_SCALE}px`
            : `${width}${unit}`;
    });

    grid.style.gridTemplateColumns = columnWidths.join(" ");

    const rowHeights = rows.map(row => {
        const height = row.height ?? "auto";
        const unit = row.heightUnit ?? "auto";

        if (unit === "auto") {
            return "auto";
        }

        return unit === "px"
            ? `${height * PREVIEW_SCALE}px`
            : `${height}${unit}`;
    });

    grid.style.gridTemplateRows = rowHeights.join(" ");
    grid.style.setProperty("--column-count", columnCount);

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

        grid.appendChild(
            createCellFromData(cellData, !isPreview)
        );
    });

    // DB未登録の空きセルを生成する。
    for (let row = 0; row < rowCount; row++) {
        for (let column = 0; column < columnCount; column++) {
            if (occupied.has(`${row}:${column}`)) {
                continue;
            }

            if (isPreview) {
                const cell = document.createElement("div");

                cell.className = "template-cell empty";
                cell.dataset.row = row;
                cell.dataset.column = column;
                cell.dataset.cellType = "EMPTY";
                cell.style.gridRow = `${row + 1}`;
                cell.style.gridColumn = `${column + 1}`;
                cell.textContent = "空白";

                grid.appendChild(cell);
            } else {
                grid.appendChild(createCell(row, column));
            }
        }
    }

    // 編集画面だけサイズ変更ハンドルを生成する。
    if (!isPreview) {
        createGridResizeHandles();
    }
}