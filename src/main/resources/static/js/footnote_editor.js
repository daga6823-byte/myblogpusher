// =====================================================
// footnote_editor.js
//
// 脚注挿入機能
// ・脚注入力ダイアログ表示
// ・カーソル位置へ [^n] を挿入
// ・本文末尾へ [^n]: 脚注テキスト を追記
// =====================================================

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
// 登録済み参考文献から脚注を作成する
//
// 脚注ボタン押下
// ↓
// 登録済み参考文献一覧表示
// ↓
// 選択した参考文献を本文へ挿入
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

		const categoryId =
			document.getElementById('categorySelect')
				.value;

		if (!categoryId || categoryId === '__new__') {
			alert('カテゴリーを選択してください');
			return;
		}

		const response =
			await fetch(
				`/category/reference/list?categoryId=${categoryId}`
			);

		const references =
			await response.json();

		const selector =
			document.getElementById('referenceSelector');

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

			label.append(
				document.createTextNode(
					reference.referenceName
				)
			);


			list.appendChild(label);

			list.appendChild(
				document.createElement('br')
			);

		});


		selector.style.display = 'block';
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
		`\n${marker}: [${reference.referenceName}](${reference.url})`;


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
//
// 入力した参考文献をカテゴリーへ登録する
// =====================================================

document.getElementById('saveReferenceButton')
	.addEventListener('click', async () => {

		const categoryId =
			document.getElementById('categorySelect').value;

		const referenceName =
			document.getElementById('referenceName').value.trim();

		const url =
			document.getElementById('referenceUrl').value.trim();


		if (!referenceName) {
			alert('参考文献名を入力してください');
			return;
		}


		await fetch('/category/reference/save', {

			method: 'POST',

			headers: {
				'Content-Type': 'application/x-www-form-urlencoded'
			},

			body:
				`categoryId=${categoryId}`
				+ `&referenceName=${encodeURIComponent(referenceName)}`
				+ `&url=${encodeURIComponent(url)}`

		});


		document.getElementById('referenceName').value = '';
		document.getElementById('referenceUrl').value = '';


		await loadReferences();

	});

// =====================================================
// 登録済み参考文献を再取得して表示更新
// =====================================================
async function loadReferences() {

	const categoryId =
		document.getElementById('categorySelect').value;


	const response =
		await fetch(
			`/category/reference/list?categoryId=${categoryId}`
		);


	const references =
		await response.json();


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
	