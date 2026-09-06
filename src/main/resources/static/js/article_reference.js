/**
 * 参考文献・脚注挿入機能
 *
 * ・脚注入力ダイアログ表示
 * ・参考文献カテゴリー選択
 * ・登録済み参考文献の取得
 * ・参考文献の登録
 * ・カーソル位置へ [^n] を挿入
 * ・本文末尾へ [^n]: 脚注テキスト を追記
 *
 * 参考文献はカテゴリー経路単位で管理する。
 */

// -----------------------------------------------------
// 取得済み参考文献
// -----------------------------------------------------

let referenceCache = [];

// -----------------------------------------------------
// 次の脚注番号を取得
// -----------------------------------------------------

function getNextFootnoteNumber(text) {

	const matches = [...text.matchAll(/\[\^(\d+)\]/g)];

	if (matches.length === 0) {
		return 1;
	}

	const max = Math.max(
		...matches.map(m => Number(m[1]))
	);

	return max + 1;
}

// =====================================================
// 参考文献カテゴリーを作成
// =====================================================

function loadReferenceCategories() {

	const select =
		document.getElementById('referenceCategorySelect');

	if (!select) {
		return;
	}

	select.innerHTML = '';

	const allOption =
		document.createElement('option');

	allOption.value = '';
	allOption.textContent = 'すべて';

	select.appendChild(allOption);

	(window.linkCategories || []).forEach(category => {

		const option =
			document.createElement('option');

		option.value = category.groupId;
		option.textContent = category.categoryPath;

		select.appendChild(option);
	});
}

// =====================================================
// 参考文献一覧を表示
//
// API通信は行わず、取得済みのreferenceCacheから表示する。
// =====================================================

function displayReferences(references) {

	const list =
		document.getElementById('referenceList');

	list.innerHTML = '';

	references.forEach(reference => {

		const label =
			document.createElement('label');

		const checkbox =
			document.createElement('input');

		checkbox.type = 'checkbox';
		checkbox.name = 'referenceCheck';
		checkbox.value =
			JSON.stringify(reference);

		label.appendChild(checkbox);

		label.appendChild(
			document.createTextNode(
				reference.referenceName
			)
		);

		list.appendChild(label);

		list.appendChild(
			document.createElement('br')
		);
	});
}

// =====================================================
// 脚注ボタン
//
// ダイアログを開いたときに参考文献を一度だけ取得する。
// =====================================================

document.getElementById('footnoteButton')
	.addEventListener('click', async () => {

		const textarea =
			document.getElementById('content');

		if (!textarea) {
			return;
		}

		document.getElementById('insertMenu')
			.style.display = 'none';

		const selector =
			document.getElementById('referenceSelector');

		if (!selector) {
			return;
		}

		// カテゴリー選択肢を作成
		loadReferenceCategories();

		// 「すべて」を初期選択
		document.getElementById('referenceCategorySelect')
			.value = '';

		// 参考文献を全件取得
		const response =
			await fetch('/category/reference/list');

		referenceCache =
			await response.json();

		// 全件表示
		displayReferences(referenceCache);

		selector.style.display = 'block';
	});

// =====================================================
// カテゴリー変更
//
// API通信は行わず、取得済みデータを絞り込む。
// =====================================================

document.getElementById('referenceCategorySelect')
	.addEventListener('change', () => {

		const groupId =
			document.getElementById('referenceCategorySelect')
				.value;

		if (!groupId) {
			displayReferences(referenceCache);
			return;
		}

		const references =
			referenceCache.filter(
				reference =>
					String(reference.groupId) === String(groupId)
			);

		displayReferences(references);
	});

// -----------------------------------------------------
// 選択した参考文献を脚注として挿入
// -----------------------------------------------------

function insertFootnote(reference) {

	const textarea =
		document.getElementById('content');

	const nextNo =
		getNextFootnoteNumber(textarea.value);

	const marker =
		`[^${nextNo}]`;

	const start =
		textarea.selectionStart;

	const end =
		textarea.selectionEnd;

	// 本文へ脚注番号挿入
	textarea.value =
		textarea.value.substring(0, start)
		+ marker
		+ textarea.value.substring(end);

	// 末尾へ脚注定義追加
	if (!textarea.value.endsWith('\n')) {
		textarea.value += '\n';
	}

	textarea.value +=
		`\n${marker}: ${reference.referenceName}`;

	if (reference.url) {

		textarea.value +=
			`\n${reference.url}`;
	}

	textarea.focus();

	textarea.setSelectionRange(
		start + marker.length,
		start + marker.length
	);

	textarea.dispatchEvent(
		new Event('input')
	);
}

// =====================================================
// 参考文献登録
// =====================================================

document.getElementById('saveReferenceButton')
	.addEventListener('click', async () => {

		const groupId =
			document.getElementById('referenceCategorySelect')
				.value;

		const referenceName =
			document.getElementById('referenceName')
				.value.trim();

		const url =
			document.getElementById('referenceUrl')
				.value.trim();

		if (!groupId) {
			alert('参考文献を登録するカテゴリーを選択してください');
			return;
		}

		if (!referenceName) {
			alert('参考文献名を入力してください');
			return;
		}

		await fetch('/category/reference/save', {

			method: 'POST',

			headers: {
				'Content-Type':
					'application/x-www-form-urlencoded'
			},

			body:
				`groupId=${groupId}`
				+ `&referenceName=${encodeURIComponent(referenceName)}`
				+ `&url=${encodeURIComponent(url)}`
		});

		document.getElementById('referenceName').value = '';
		document.getElementById('referenceUrl').value = '';

		// 登録後は参考文献一覧を再取得せず、
		// 次回ダイアログを開いたときに最新状態を取得する。
		referenceCache = [];
	});

// =====================================================
// 選択した参考文献を脚注として挿入
// =====================================================

document.getElementById('insertReferenceButton')
	.addEventListener('click', () => {

		const checked =
			document.querySelector(
				'input[name="referenceCheck"]:checked'
			);

		if (!checked) {
			alert('参考文献を選択してください');
			return;
		}

		const reference =
			JSON.parse(checked.value);

		insertFootnote(reference);

		document.getElementById('referenceSelector')
			.style.display = 'none';
	});

// =====================================================
// キャンセル
// =====================================================

document.getElementById('cancelReferenceButton')
	.addEventListener('click', () => {

		document.getElementById('referenceSelector')
			.style.display = 'none';
	});