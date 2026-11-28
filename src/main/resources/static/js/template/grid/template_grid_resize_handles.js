/**
 * template_grid_resize_handles.js
 *
 * 行・列のサイズ変更ハンドルの生成と位置更新を担当する。
 *
 * ハンドルを操作したときのサイズ変更処理は、template_grid_resize.js が担当する。
 */
import { templateState } from "../template_state.js";
import { getGridTrackSizes } from "./template_grid_tracks.js";
import { startGridResize } from "./template_grid_resize.js";

/**
 * 行・列のサイズ変更ハンドルを生成する。
 */
export function createGridResizeHandles() {

	const grid = templateState.grid;

	if (!grid) {
		return;
	}

	grid.style.position = "relative";

	// 既存ハンドルを削除する。
	grid.querySelectorAll(".template-resize-handle")
		.forEach(handle => handle.remove());

	const sizes = getGridTrackSizes();

	sizes.columns.forEach((width, index) => {

		if (index < sizes.columns.length - 1) {

			createResizeHandle("column", index);

		}

	});

	sizes.rows.forEach((height, index) => {

		if (index < sizes.rows.length - 1) {

			createResizeHandle("row", index);

		}

	});

	updateGridResizeHandlePositions();

}

/**
 * サイズ変更ハンドルを生成する。
 *
 * @param {"row"|"column"} type
 * @param {number} index
 */
function createResizeHandle(type, index) {

	const grid = templateState.grid;

	const handle = document.createElement("div");

	handle.className = `template-resize-handle ${type}-resize-handle`;

	handle.dataset.resizeType = type;

	handle.dataset.resizeIndex = index;

	handle.style.position = "absolute";

	handle.style.zIndex = "20";

	handle.style.touchAction = "none";
	handle.style.userSelect = "none";

	handle.style.backgroundColor = "rgba(50, 120, 220, 0.45)";

	handle.style.cursor = type === "column"
		? "col-resize"
		: "row-resize";

	if (type === "column") {

		handle.style.width = "10px";
		handle.style.top = "8px";
		handle.style.bottom = "8px";

	} else {

		handle.style.height = "10px";
		handle.style.left = "8px";
		handle.style.right = "8px";

	}

	// ドラッグ中にハンドル位置を追従させるため、位置更新処理を渡す。
	handle.addEventListener("pointerdown", event => {
		startGridResize(event, updateGridResizeHandlePositions);
	});

	grid.appendChild(handle);

}

/**
 * サイズ変更ハンドルの位置を更新する。
 */
export function updateGridResizeHandlePositions() {

	const grid = templateState.grid;

	if (!grid || !grid.isConnected) {

		return;

	}

	const sizes = getGridTrackSizes();
	const style = getComputedStyle(grid);

	const borderLeft = parseFloat(style.borderLeftWidth) || 0;
	const borderTop = parseFloat(style.borderTopWidth) || 0;

	const handles = grid.querySelectorAll(".template-resize-handle");

	handles.forEach(handle => {

		const type = handle.dataset.resizeType;
		const index = Number(handle.dataset.resizeIndex);

		if (type === "column") {

			const position = sizes.columns
				.slice(0, index + 1)
				.reduce((total, value) => total + value, 0)
				+ sizes.columnGap * index
				+ sizes.paddingLeft
				+ borderLeft
				+ sizes.columnGap / 2;

			handle.style.left = `${position - 5}px`;

		} else {

			const position = sizes.rows
				.slice(0, index + 1)
				.reduce((total, value) => total + value, 0)
				+ sizes.rowGap * index
				+ sizes.paddingTop
				+ borderTop
				+ sizes.rowGap / 2;

			handle.style.top = `${position - 5}px`;

		}

	});

}