
/**
 * link_facade.js
 *
 * 記事リンク挿入機能のファサードクラス。
 *
 * 各機能クラスを生成して連携させ、記事リンク挿入機能全体の
 * 初期化を担当する。
 *
 * HTML側はlink_manager.jsだけを読み込めばよい。
 */
import { LinkDataService } from '../link/link_data_service.js';
import { LinkListManager } from '../link/link_list_manager.js';
import { LinkInsertService } from '../link/link_insert_service.js';
import { LinkModalManager } from '../link/link_modal_manager.js';

class LinkFacade {

	constructor() {

		// 記事・カテゴリーの取得とキャッシュ
		this.linkDataService =
			new LinkDataService();

		// 記事一覧と選択状態の管理
		this.linkListManager =
			new LinkListManager(
				this.linkDataService
			);

		// Markdown生成と本文への挿入
		this.linkInsertService =
			new LinkInsertService();

		// モーダルと各機能の連携
		this.linkModalManager =
			new LinkModalManager(
				this.linkDataService,
				this.linkListManager,
				this.linkInsertService
			);
	}

	/**
	 * 記事リンク挿入機能を初期化する。
	 */
	initialize() {
		this.linkModalManager.initialize();
	}
}

// ファサードクラスを外部から利用可能にする。
export { LinkFacade };

// 記事リンク挿入機能の初期化
document.addEventListener('DOMContentLoaded', () => {

	const linkFacade = new LinkFacade();

	linkFacade.initialize();

});
