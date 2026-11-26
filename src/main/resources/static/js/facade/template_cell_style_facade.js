/**
 * テンプレートセルの装飾機能の窓口となるファサード。
 *
 * 他のファイルはこのファイルだけをimportすれば、これまで通り使える。
 * 読み込み時にイベント登録(template_cell_style_events.js)も行う。
 * 各機能の実装は、色変換・色履歴・装飾の保存・装飾の復元・イベント登録の各ファイルが担当する。
 */
import "../template/template_cell_style_events.js";

export { restoreCellStyle } from "../template/template_cell_style_restore.js";
