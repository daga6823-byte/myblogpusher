// =====================================================
// frontmatter.js
//
// Hugo Front Matter を管理する
// ・Front Matter解析
// ・日付生成
// ・カテゴリー取得
// ・Front Matter更新
// ・draft/comments初期化
// =====================================================

// -----------------------------------------------------
// Front Matterを解析する
// -----------------------------------------------------
function parseFrontMatter(text) {
	const match = text.match(/^\+\+\+\n([\s\S]*?)\+\+\+\n*/);

	if (!match) {
		return null;
	}

	return match[1];
}

// -----------------------------------------------------
// Hugo用の日付文字列を生成する
// 例：2026-07-13T20:15:30+09:00
// -----------------------------------------------------
function buildDateString() {

	const now = new Date();

	const offset = -now.getTimezoneOffset();
	const sign = offset >= 0 ? '+' : '-';

	const pad = (n) => String(Math.abs(n)).padStart(2, '0');

	const offsetStr =
		`${sign}${pad(offset / 60)}:${pad(offset % 60)}`;

	return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
		+ `T${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
		+ offsetStr;
}

// -----------------------------------------------------
// 選択中カテゴリー名を取得する
//
// categorySelectのvalueはcategoryIdなので
// 表示テキスト(フルパス)の最後をカテゴリー名として使用する
// -----------------------------------------------------
function getSelectedCategoryLabel() {

	const categorySelect = document.getElementById('categorySelect');
	const newCategoryName = document.getElementById('newCategoryName').value.trim();

	// 新規カテゴリー
	if (categorySelect.value === '__new__') {
		return newCategoryName;
	}

	const selectedOption =
		categorySelect.options[categorySelect.selectedIndex];

	const fullPath =
		selectedOption ? selectedOption.text : '';

	const segments = fullPath.split('/');

	return segments[segments.length - 1];
}

// -----------------------------------------------------
// Front Matterを最新状態へ更新する
//
// updateDate=true
//     日付も更新する
//
// updateDate=false
//     既存日付を維持する
// -----------------------------------------------------
function updateFrontMatterFields(updateDate) {

	const textarea = document.getElementById('content');

	const title = document.getElementById('title').value;
	const category = getSelectedCategoryLabel();

	const draft = document.getElementById('draftSelect').value;
	const comments = document.getElementById('commentsSelect').value;

	const currentText = textarea.value;

	const frontMatterPattern = /^\+\+\+\n[\s\S]*?\+\+\+\n*/;

	const existingFrontMatterBody =
		parseFrontMatter(currentText);

	let dateLine;

	if (updateDate || !existingFrontMatterBody) {

		dateLine = `date = '${buildDateString()}'`;

	} else {

		const dateMatch =
			existingFrontMatterBody.match(/^date = .+$/m);

		dateLine =
			dateMatch
				? dateMatch[0]
				: `date = '${buildDateString()}'`;
	}

	// Front Matterを除いた本文
	let bodyText = currentText;

	const endIndex = currentText.indexOf("\n+++\n");

	if (endIndex !== -1) {
		bodyText = currentText.substring(endIndex + 5);
	}

	const newFrontMatter =
		'+++\n'
		+ `title = '${title}'\n`
		+ `${dateLine}\n`
		+ `categories = ["${category}"]\n`
		+ `draft = ${draft}\n`
		+ `comments = ${comments}\n`
		+ '+++\n\n';

	textarea.value =
		newFrontMatter + bodyText;
}

// -----------------------------------------------------
// 保存・添削実行前にFront Matterを最新化する
// -----------------------------------------------------
document.querySelectorAll('[formaction]').forEach(btn => {

	btn.addEventListener('click', () => {
		updateFrontMatterFields(true);
	});

});

// -----------------------------------------------------
// 既存Front Matterから
// draft/commentsを読み込みプルダウンへ反映する
// -----------------------------------------------------
function initFrontMatterSelects() {

	const textarea = document.getElementById('content');

	if (!textarea) {
		return;
	}

	const frontMatterBody =
		parseFrontMatter(textarea.value);

	if (!frontMatterBody) {
		return;
	}

	const draftMatch =
		frontMatterBody.match(/^draft = (true|false)$/m);

	if (draftMatch) {
		document.getElementById('draftSelect').value =
			draftMatch[1];
	}

	const commentsMatch =
		frontMatterBody.match(/^comments = (true|false)$/m);

	if (commentsMatch) {
		document.getElementById('commentsSelect').value =
			commentsMatch[1];
	}
}