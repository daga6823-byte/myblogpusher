/**
 * image_manager.js
 *
 * 画像管理機能のエントリーポイント（ファサード）。
 *
 * 実装は image_state.js / image_folders.js / image_gallery.js /
 * image_uploader.js に分割されている。
 * この1ファイルをモジュールとして読み込むだけで、
 * 画像管理機能一式（状態管理・フォルダ取得・一覧表示・アップロード）が
 * 有効になる。
 */

import '../image/image_state.js';
import '../image/image_folders.js';
import '../image/image_gallery.js';
import '../image/image_uploader.js';
