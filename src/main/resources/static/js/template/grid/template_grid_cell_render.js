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
	// 後から装飾を変更するときに使う縁取り設定を保持する。
	cell.dataset.textStrokeColor = cellData.textStrokeColor ?? "";
	cell.dataset.textStrokeWidth = cellData.textStrokeWidth ?? "";

	// 文字の塗り部分を維持し、背面に縁取りを描画する。
	if (cellData.textStrokeColor
		&& cellData.textStrokeWidth != null
		&& cellData.textStrokeWidth > 0) {

		const strokeWidth =
			cellData.textStrokeWidth * PREVIEW_SCALE;

		const shadows = [];

		for (let i = 0; i < 16; i++) {

			const angle = (2 * Math.PI * i) / 16;

			const offsetX =
				(Math.cos(angle) * strokeWidth).toFixed(2);

			const offsetY =
				(Math.sin(angle) * strokeWidth).toFixed(2);

			shadows.push(
				`${offsetX}px ${offsetY}px 0 ${cellData.textStrokeColor}`
			);
		}

		cell.style.textShadow = shadows.join(", ");
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

			// 記事編集画面の変数入力欄から参照する属性
			cell.dataset.templateVariable =
				cell.dataset.variableName;

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
/**
 * DBのセル情報から、グリッドに配置できるセル要素を生成する。
 *
 * @param {Object} cellData DBから取得したセル情報
 * @param {boolean} enableSelection セル選択イベントを登録するか
 * @return {HTMLElement} 生成したセル要素
 */
export function createCellFromData(cellData, enableSelection = true) {
	const row = cellData.rowNumber - 1;
	const column = cellData.columnNumber - 1;
	const rowSpan = cellData.rowSpan ?? 1;
	const columnSpan = cellData.columnSpan ?? 1;

	// 記事編集画面では編集操作用のイベントを登録しない。
	const cell = enableSelection
		? createCell(row, column)
		: document.createElement("div");

	cell.dataset.row = row;
	cell.dataset.column = column;
	cell.dataset.cellId = cellData.cellId;

	cell.dataset.cellType = cellData.cellType;

	cell.dataset.imageAssetId = cellData.imageAssetId ?? "";

	// 記事編集画面の画像割り当てから参照する属性
	if (cellData.cellType === "IMAGE") {
		cell.dataset.templateImageCell = cellData.cellId;
		cell.dataset.imageWidth = cellData.imageWidth ?? "";
	}

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