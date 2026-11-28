/**
 * template_grid_cell_render.js
 *
 * DBから取得したセル情報をもとに、セル要素の見た目と中身を組み立てる。
 *
 * 位置と結合範囲、装飾(色・縁取り・文字サイズ・配置・余白)、
 * セル種別ごとの表示内容(文字・変数・画像・空白)を設定する。
 */
import { TEXT_ALIGN_TO_JUSTIFY } from "../template_color_utils.js";
import { createCell } from "./template_grid_cell.js";

// 編集画面のプレビュー表示倍率
const PREVIEW_SCALE = 5;

/**
 * セルの装飾をDBの値から設定する。
 */
function applyCellStyle(cell, cellData) {

	if (cellData.backgroundColor) {
		cell.style.backgroundColor = cellData.backgroundColor;
	}

	if (cellData.textColor) {

		cell.style.color = cellData.textColor;

	}

	// 文字の縁取り色
	if (cellData.textStrokeColor) {

		cell.style.webkitTextStrokeColor = cellData.textStrokeColor;

	}

	// 文字の縁取りの太さ
	if (cellData.textStrokeWidth != null) {
		cell.style.webkitTextStrokeWidth =
			`${cellData.textStrokeWidth * PREVIEW_SCALE}px`;
	}

	if (cellData.fontSize != null) {
		cell.style.fontSize =
			`${cellData.fontSize * PREVIEW_SCALE}px`;
	}

	if (cellData.fontWeight) {
		cell.style.fontWeight = cellData.fontWeight;
	}

	if (cellData.textAlign) {

		cell.style.textAlign = cellData.textAlign;

		cell.style.justifyContent =
			TEXT_ALIGN_TO_JUSTIFY[cellData.textAlign]
			?? TEXT_ALIGN_TO_JUSTIFY.center;

	}

	if (cellData.verticalAlign) {
		cell.style.alignItems = cellData.verticalAlign;
	}

	if (cellData.padding != null) {
		const unit = cellData.paddingUnit ?? "px";
		cell.style.padding =
			`${cellData.padding * PREVIEW_SCALE}${unit}`;
	}
}

/**
 * セル種別に応じた表示内容を設定する。
 */
function applyCellContent(cell, cellData) {
	switch (cellData.cellType) {
		case "TEXT":
			cell.dataset.cellContent =
				cellData.cellContent ?? "";
			cell.textContent = cell.dataset.cellContent;
			break;

		case "VARIABLE":
			cell.dataset.variableName =
				cellData.variableName ?? "";
			cell.textContent =
				`{{${cell.dataset.variableName}}}`;
			break;

		case "IMAGE":
			if (cellData.imageUrl) {
				const image = document.createElement("img");
				image.src = cellData.imageUrl;
				image.alt = "テンプレート画像";
				image.style.maxWidth = "100%";
				image.style.maxHeight = "100%";
				image.style.objectFit = "contain";

				if (cellData.imageWidth) {
					image.style.width =
						`${cellData.imageWidth}px`;
				}

				cell.replaceChildren(image);
			} else {
				cell.textContent = "画像";
			}
			break;

		case "EMPTY":
			cell.textContent = "空白";
			break;
	}
}

/**
 * DBのセル情報から、グリッドに配置できるセル要素を生成する。
 */
export function createCellFromData(cellData) {
	const row = cellData.rowNumber - 1;
	const column = cellData.columnNumber - 1;
	const rowSpan = cellData.rowSpan ?? 1;
	const columnSpan = cellData.columnSpan ?? 1;

	const cell = createCell(row, column);

	cell.dataset.cellId = cellData.cellId;
	cell.dataset.cellType = cellData.cellType;
	cell.dataset.imageAssetId = cellData.imageAssetId ?? "";

	// 既存セルの編集時に現在の内容を取得できるよう保持する。
	if (cellData.cellType === "TEXT") {
		cell.dataset.cellContent = cellData.cellContent ?? "";
	}

	if (cellData.cellType === "VARIABLE") {
		cell.dataset.variableName = cellData.variableName ?? "";
	}

	cell.style.gridRow =
		`${cellData.rowNumber} / span ${rowSpan}`;

	cell.style.gridColumn =
		`${cellData.columnNumber} / span ${columnSpan}`;

	applyCellStyle(cell, cellData);
	applyCellContent(cell, cellData);

	cell.className =
		`template-cell ${cellData.cellType.toLowerCase()}`;

	return cell;
}