/**
 * テンプレートセル機能の読み込みを担当するファサード
 *
 * セル選択、セル種類変更、装飾、レイアウト操作を
 * 各専用モジュールへ分離する。
 */

// セル選択機能
export { selectCell } from "../template/template_cell_selection.js";

// セル種類変更機能
import "../template/template_cell_type.js";

// セル装飾・色履歴機能
import "../template/template_cell_style.js";

// セル結合・分割機能
import "../template/template_cell_layout.js";