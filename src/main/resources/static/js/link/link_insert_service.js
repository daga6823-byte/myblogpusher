
/**
 * link_insert_service.js
 *
 * 記事リンク挿入機能の本文挿入を担当するクラス。
 *
 * 選択した記事からMarkdownリンクを生成し、
 * 本文への挿入とカーソル位置の調整を担当する。
 * 記事の選択やモーダル表示は担当しない。
 */
export class LinkInsertService {

	/**
	 * Markdownリンクを生成する。
	 *
	 * @param {Object} article 選択した記事
	 * @param {string} text リンクテキスト
	 * @returns {string} Markdownリンク
	 */
	createMarkdown(article, text) {

		const linkText =
			(text || article.title).trim();

		return '[' + linkText + '](' + article.hugoPath + ')';
	}

	/**
	 * 本文の指定位置にMarkdownリンクを挿入する。
	 *
	 * @param {string} markdown 挿入するMarkdownリンク
	 * @param {number|null} insertPosition 挿入位置
	 */
	insertIntoContent(markdown, insertPosition) {

		const textarea =
			document.querySelector(
				'textarea[name="content"]'
			);

		if (!textarea) {
			return;
		}

		const position =
			insertPosition !== null
				? insertPosition
				: textarea.value.length;

		textarea.value =
			textarea.value.substring(0, position)
			+ markdown
			+ textarea.value.substring(position);

		textarea.dispatchEvent(
			new Event('input', { bubbles: true })
		);

		textarea.focus();

		const cursorPosition =
			position + markdown.length;

		textarea.setSelectionRange(
			cursorPosition,
			cursorPosition
		);
	}
}
