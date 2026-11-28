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

	// 列間のハンドル
	sizes.columns.forEach((width, index) => {
		if (index < sizes.columns.length - 1) {
			createResizeHandle("column", index);
		}
	});

	// 行間のハンドル
	sizes.rows.forEach((height, index) => {
		if (index < sizes.rows.length - 1) {
			createResizeHandle("row", index);
		}
	});

	// 左右の外周ハンドル
	if (sizes.columns.length >= 2) {
		createResizeHandle("column", 0, "start");
		createResizeHandle("column", sizes.columns.length - 1, "end");
	}

	// 上下の外周ハンドル
	if (sizes.rows.length >= 2) {
		createResizeHandle("row", 0, "start");
		createResizeHandle("row", sizes.rows.length - 1, "end");
	}

	updateGridResizeHandlePositions();

}

/**
 * サイズ変更ハンドルを生成する。
 *
 * @param {"row"|"column"} type
 * @param {number} index
 */
function createResizeHandle(type, index, edge = null) {

	const grid = templateState.grid;

	const handle = document.createElement("div");

	handle.className = `template-resize-handle ${type}-resize-handle`;

	handle.dataset.resizeType = type;

	handle.dataset.resizeIndex = index;

	if (edge) {
		handle.dataset.resizeEdge = edge;
	}

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

		if (edge === "start") {
			handle.style.left = "3px";
		} else if (edge === "end") {
			handle.style.right = "3px";
		}

	} else {

		handle.style.height = "10px";
		handle.style.left = "8px";
		handle.style.right = "8px";

		if (edge === "start") {
			handle.style.top = "3px";
		} else if (edge === "end") {
			handle.style.bottom = "3px";
		}

	}

	// ドラッグ中にハンドル位置を追従させるため、位置更新処理を渡す。
	handle.addEventListener("pointerdown", event => {
		startGridResize(event, updateGridResizeHandlePositions);
	});

	grid.appendChild(handle);

}

/**
 * サイズ変更ハンドルの位置を更新する。
 *
 * 内部ハンドルはトラック間の境界に配置し、
 * 外周ハンドルはグリッドの四辺に配置する。
 */
export function updateGridResizeHandlePositions() {

	const grid = templateState.grid;

	if (!grid || !grid.isConnected) {
		return;
	}

	const sizes = getGridTrackSizes();

	const handles = grid.querySelectorAll(
		".template-resize-handle"
	);

	handles.forEach(handle => {

		const type = handle.dataset.resizeType;
		const index = Number(handle.dataset.resizeIndex);
		const edge = handle.dataset.resizeEdge;

		if (type === "column") {

			if (edge === "start") {
				handle.style.left = "3px";
				handle.style.right = "";
				return;
			}

			if (edge === "end") {
				handle.style.left = "";
				handle.style.right = "3px";
				return;
			}

			const position = sizes.columns
				.slice(0, index + 1)
				.reduce((total, value) => total + value, 0)
				+ sizes.columnGap * index
				+ sizes.paddingLeft
				+ (parseFloat(getComputedStyle(grid).borderLeftWidth) || 0)
				+ sizes.columnGap / 2;

			handle.style.left = `${position - 5}px`;

		} else {

			if (edge === "start") {
				handle.style.top = "3px";
				handle.style.bottom = "";
				return;
			}

			if (edge === "end") {
				handle.style.top = "";
				handle.style.bottom = "3px";
				return;
			}

			const position = sizes.rows
				.slice(0, index + 1)
				.reduce((total, value) => total + value, 0)
				+ sizes.rowGap * index
				+ sizes.paddingTop
				+ (parseFloat(getComputedStyle(grid).borderTopWidth) || 0)
				+ sizes.rowGap / 2;

			handle.style.top = `${position - 5}px`;
		}
	});
}
