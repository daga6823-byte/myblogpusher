/**
 * template_facade.js
 *
 * テンプレート編集機能のエントリーポイント（ファサード）。
 *
 * 実装は template_state.js / template_api.js /
 * template_grid.js / template_cell.js / template_image.js
 * に分割されている。
 *
 * このファイルを読み込むだけで、
 * テンプレート編集機能一式が有効になる。
 */

import "../template/template_state.js";
import "../template/template_api.js";
import "../template/template_grid.js";
import "../template/template_cell.js";
import "../template/template_image.js";

import "./image_facade.js";