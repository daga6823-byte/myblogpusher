/**
 * template_grid_facade.js
 *
 * グリッド機能の窓口となるファサード。
 *
 * 他のファイルはこのファイルだけをimportすれば、これまで通り使える。
 * 読み込み時にイベント登録(template/grid/template_grid_events.js)も行う。
 * 各機能の実装は、セル生成・グリッド復元・サイズ変更・行列操作の各ファイルが担当する。
 */
import "../template/grid/template_grid_events.js";

export { createCell } from "../template/grid/template_grid_cell.js";
export { restoreGrid } from "../template/grid/template_grid_restore.js";