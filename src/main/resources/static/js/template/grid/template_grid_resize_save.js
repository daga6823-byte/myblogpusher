/**
 * template_grid_resize_save.js
 *
 * ドラッグで変更した行高・列幅のDB保存を担当する。
 *
 * 隣接する2トラックを1回のリクエストで保存し、
 * 保存成功後に画面復元用のデータ(data属性)を更新する。
 */
import { templateState } from "./template_state.js";
import { callLayoutApi } from "./template_api.js";

/**
 * ドラッグで変更した隣接する行高・列幅をDBに一括保存する。
 *
 * @param {"row"|"column"} type
 * @param {number} index
 * @param {number} firstSize
 * @param {number} secondSize
 */
export async function saveGridTrackSizes(
	type,
	index,
	firstSize,
	secondSize
) {

	const isColumn = type === "column";

	const dataKey = isColumn ? "columnsJson" : "rowsJson";
	const fallbackKey = isColumn ? "columns" : "rows";

	const grid = templateState.grid;

	const items = JSON.parse(
		grid.dataset[dataKey]
		|| grid.dataset[fallbackKey]
		|| "[]"
	);

	const firstNumber = index + 1;
	const secondNumber = index + 2;

	const getItem = number => {

		return items.find(item => {

			const itemNumber = isColumn
				? item.columnNumber
				: item.rowNumber;

			return Number(itemNumber) === number;

		});

	};

	const firstItem = getItem(firstNumber);
	const secondItem = getItem(secondNumber);

	if (!firstItem || !secondItem) {
		throw new Error("サイズの保存先が見つかりません。");
	}

	const endpoint = isColumn
		? "/template/api/layout/column/sizes"
		: "/template/api/layout/row/sizes";

	const payload = isColumn
		? {
			firstColumnNumber: firstNumber,
			firstWidth: String(firstSize),
			secondColumnNumber: secondNumber,
			secondWidth: String(secondSize)
		}
		: {
			firstRowNumber: firstNumber,
			firstHeight: String(firstSize),
			secondRowNumber: secondNumber,
			secondHeight: String(secondSize)
		};

	// 隣接する2トラックを1回のリクエストで保存する。
	await callLayoutApi(endpoint, payload);

	// DB保存成功後に画面復元用のデータを更新する。
	if (isColumn) {

		firstItem.width = firstSize;
		firstItem.widthUnit = "px";

		secondItem.width = secondSize;
		secondItem.widthUnit = "px";

	} else {

		firstItem.height = firstSize;
		firstItem.heightUnit = "px";

		secondItem.height = secondSize;
		secondItem.heightUnit = "px";

	}

	grid.dataset[dataKey] = JSON.stringify(items);

}