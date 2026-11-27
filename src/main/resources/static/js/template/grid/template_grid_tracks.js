/**
 * template_grid_tracks.js
 *
 * グリッドの現在の行高・列幅の取得を担当する。
 */
import { templateState } from "./template_state.js";

/**
 * グリッドの現在の行高・列幅を取得する。
 *
 * ブラウザが計算した実際の表示サイズを使用する。
 */
export function getGridTrackSizes() {

	const grid = templateState.grid;

	if (!grid) {
		throw new Error("グリッドが見つかりません。");
	}

	const style = getComputedStyle(grid);

	/**
	 * 計算済みのトラックサイズを取得する。
	 *
	 * getComputedStyle() の結果は、
	 * ブラウザによって計算された px 単位の値として取得する。
	 */
	const parseTrackSizes = value => {

		const tracks = value.trim().split(/\s+/);

		const sizes = tracks.map(track => {

			if (!/^-?(?:\d+\.?\d*|\.\d+)px$/.test(track)) {
				throw new Error(
					`グリッドのトラックサイズが不正です: ${track}`
				);
			}

			return parseFloat(track);

		});

		return sizes;

	};

	const columns = parseTrackSizes(style.gridTemplateColumns);

	const rows = parseTrackSizes(style.gridTemplateRows);

	if (
		columns.length !== templateState.columnCount ||
		rows.length !== templateState.rowCount
	) {
		throw new Error("グリッドの行・列サイズを正しく取得できません。");
	}

	return {

		columns,

		rows,

		columnGap: parseFloat(style.columnGap) || 0,

		rowGap: parseFloat(style.rowGap) || 0,

		paddingLeft: parseFloat(style.paddingLeft) || 0,

		paddingTop: parseFloat(style.paddingTop) || 0

	};

}