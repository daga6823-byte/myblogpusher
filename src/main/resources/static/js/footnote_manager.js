/**
 * 脚注の共通処理を担当するモジュール
 *
 * 脚注番号の採番と、本文への脚注挿入を担当する。
 * 参考文献の取得・登録などの画面固有処理は担当しない。
 */
window.FootnoteManager = {

	/**
	 * 本文内の脚注番号から、次に使用する番号を取得する。
	 */
	getNextNumber(text) {
		const matches = [...text.matchAll(/\[\^(\d+)\]/g)];

		if (matches.length === 0) {
			return 1;
		}

		return Math.max(
			...matches.map(match => Number(match[1]))
		) + 1;
	},

	/**
	 * カーソル位置へ脚注番号を挿入し、本文末尾へ脚注定義を追加する。
	 */
	insert(textarea, reference) {
		if (!textarea || !reference) {
			return;
		}

		const nextNumber = this.getNextNumber(textarea.value);
		const marker = `[^${nextNumber}]`;
		const start = textarea.selectionStart;
		const end = textarea.selectionEnd;

		// 選択範囲を脚注番号に置き換える。
		textarea.value =
			textarea.value.substring(0, start)
			+ marker
			+ textarea.value.substring(end);

		// 本文末尾へ脚注定義を追加する。
		if (!textarea.value.endsWith('\n')) {
			textarea.value += '\n';
		}

		textarea.value += `\n${marker}: ${reference.referenceName}`;

		if (reference.url) {
			textarea.value += `\n${reference.url}`;
		}

		// 脚注番号の直後へカーソルを戻す。
		textarea.focus();
		textarea.setSelectionRange(
			start + marker.length,
			start + marker.length
		);

		// 本文変更を画面側へ通知する。
		textarea.dispatchEvent(
			new Event('input', { bubbles: true })
		);
	}
};