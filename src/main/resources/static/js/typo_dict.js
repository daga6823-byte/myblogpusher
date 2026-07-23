// ============================================================
// 誤字パターン編集機能
// ============================================================

// class="btn-edit-typo" が付いている編集ボタンをすべて取得する
// 取得した各ボタンにクリックイベントを設定する
document.querySelectorAll('.btn-edit-typo').forEach(btn => {

	// 編集ボタンがクリックされたときに実行する処理
	btn.addEventListener('click', () => {

		// HTMLのdata-typo-id属性から、編集対象の誤字パターンIDを取得する
		const typoId = btn.dataset.typoId;

		// HTMLのdata-wrong-word属性から、現在登録されている誤字を取得する
		const oldWrong = btn.dataset.wrongWord;

		// HTMLのdata-correct-word属性から、現在登録されている正しい表記を取得する
		const oldCorrect = btn.dataset.correctWord;


		// 現在の誤字を初期値として表示し、新しい誤字の入力を受け付ける
		// キャンセルされた場合はnullが返る
		const newWrong = prompt('誤字を入力してください', oldWrong);

		// キャンセルされた場合は更新処理を中止する
		if (newWrong === null) return;


		// 現在の正しい表記を初期値として表示し、新しい正しい表記の入力を受け付ける
		// キャンセルされた場合はnullが返る
		const newCorrect = prompt('正しい表記を入力してください', oldCorrect);

		// キャンセルされた場合は更新処理を中止する
		if (newCorrect === null) return;


		// POST送信用のパラメータを作成する
		// URLSearchParamsを使用することで、
		// application/x-www-form-urlencoded形式のデータを作成できる
		const params = new URLSearchParams();

		// 更新対象となる誤字パターンのIDを設定する
		params.append('typoId', typoId);

		// 入力された誤字を前後の空白を削除して設定する
		params.append('wrongWord', newWrong.trim());

		// 入力された正しい表記を前後の空白を削除して設定する
		params.append('correctWord', newCorrect.trim());


		// サーバーの誤字パターン更新APIにPOSTリクエストを送信する
		fetch('/typo-dict/update', {

			// HTTPメソッドはPOSTを使用する
			method: 'POST',

			// 送信するデータがフォーム形式であることを指定する
			headers: {
				'Content-Type': 'application/x-www-form-urlencoded'
			},

			// URLSearchParamsで作成したパラメータを文字列に変換して送信する
			body: params.toString()
		})

			// サーバーから返されたレスポンスをJSON形式に変換する
			.then(res => res.json())

			// JSONに変換されたレスポンスデータを受け取る
			.then(data => {

				// サーバーから「更新成功」を示すresult=okが返された場合
				if (data.result === 'ok') {

					// 更新成功のメッセージを表示する
					alert('誤字パターンを更新しました。');

					// ページを再読み込みして、更新後のデータを画面に反映する
					location.reload();
				}
			});
	});
});


// ============================================================
// テーブルのソート機能
// ============================================================

// 各列の現在のソート状態を保持するオブジェクト
// 例：{ category: 'asc' }
//     { wrong: 'desc' }
let sortState = {};


// class="sortable" が付いているテーブルヘッダーをすべて取得する
// 取得した各ヘッダーにクリックイベントを設定する
document.querySelectorAll('.sortable').forEach(th => {

	// ソート可能な項目であることが分かるようにマウスカーソルを変更する
	th.style.cursor = 'pointer';


	// テーブルヘッダーがクリックされたときに実行する処理
	th.addEventListener('click', () => {

		// data-sort属性からソート対象のキーを取得する
		// 例：data-sort="category" → "category"
		//     data-sort="wrong"    → "wrong"
		//     data-sort="correct"  → "correct"
		const key = th.dataset.sort;


		// 現在のソート方向を確認し、
		// 現在が昇順なら降順、それ以外なら昇順に切り替える
		const direction = sortState[key] === 'asc' ? 'desc' : 'asc';


		// 今回クリックされた項目のソート状態だけを保持する
		// これにより、複数の列が同時にソート状態になることを防ぐ
		sortState = { [key]: direction };


		// 誤字一覧テーブルのtbody要素を取得する
		const tbody = document.getElementById('typoTableBody');

		// tbody内の全行を取得し、通常の配列に変換する
		const rows = Array.from(tbody.querySelectorAll('tr'));


		// テーブルの行をソートする
		rows.sort((a, b) => {

			// 各行のdata属性から比較対象の値を取得する
			// 値が存在しない場合は空文字を使用する
			const valA = a.dataset[key] || '';
			const valB = b.dataset[key] || '';


			// 昇順の場合はA→Bの順で比較する
			// 降順の場合はB→Aの順で比較する
			// 'ja'を指定することで日本語の文字列として比較する
			return direction === 'asc'
				? valA.localeCompare(valB, 'ja')
				: valB.localeCompare(valA, 'ja');
		});


		// ソート後の行をtbodyに順番に追加する
		// 既存の行を再度appendChildすることで、DOM上の並び順が変更される
		rows.forEach(row => tbody.appendChild(row));


		// すべてのソートアイコンを一旦空にする
		// 他の列に表示されていた▲・▼を消す
		document.querySelectorAll('.sort-icon').forEach(icon => icon.textContent = '');


		// 現在ソートした列にソート方向を示すアイコンを表示する
		// 昇順：▲
		// 降順：▼
		th.querySelector('.sort-icon').textContent =
			direction === 'asc' ? '▲' : '▼';
	});
});


// ============================================================
// 一括選択・一括操作
// ============================================================


// 一覧の全項目を選択・解除するチェックボックスを取得する
const selectAllTypoCheckbox =
	document.getElementById('selectAllTypoCheckbox');


// 全選択チェックボックスの状態が変更されたときに実行する
selectAllTypoCheckbox?.addEventListener('change', () => {

	// 各誤字パターンのチェックボックスをすべて取得する
	document.querySelectorAll('.bulk-typo-checkbox').forEach(cb => {

		// 全選択チェックボックスと同じ状態にする
		// 全選択がON → すべてON
		// 全選択がOFF → すべてOFF
		cb.checked = selectAllTypoCheckbox.checked;
	});
});


// ============================================================
// 選択された誤字パターンIDを取得する共通関数
// ============================================================

// 現在チェックされている誤字パターンのIDを配列で返す
function getSelectedTypoIds() {

	// チェックされているチェックボックスだけを取得する
	return Array.from(
		document.querySelectorAll('.bulk-typo-checkbox:checked')
	)

		// 各チェックボックスのvalue属性から誤字パターンIDを取得する
		.map(cb => cb.value);
}


// ============================================================
// 一括カテゴリー変更機能
// ============================================================


// 「カテゴリーを変更」ボタンがクリックされたときの処理
document.getElementById('bulkCategoryButton')?.addEventListener('click', () => {

	// 現在選択されている誤字パターンのIDを取得する
	const selectedIds = getSelectedTypoIds();


	// 1件も選択されていない場合
	if (selectedIds.length === 0) {

		// エラーメッセージを表示する
		alert('変更する項目が選択されていません。');

		// 処理を中止する
		return;
	}


	// カテゴリー選択コンボボックスからカテゴリーIDを取得する
	const categoryId =
		document.getElementById('bulkCategorySelect').value;


	// 選択されているカテゴリーの表示名を取得する
	// 確認ダイアログに表示するために使用する
	const categoryLabel =
		document.getElementById('bulkCategorySelect')
			.selectedOptions[0].textContent;


	// 変更対象の件数と変更先カテゴリーを表示して確認する
	const ok = confirm(
		`選択した${selectedIds.length}件のカテゴリーを「${categoryLabel}」に変更してもよろしいですか？`
	);


	// キャンセルされた場合は処理を中止する
	if (!ok) return;


	// 一括カテゴリー変更用の非表示フォームを取得する
	const form = document.getElementById('bulkCategoryForm');


	// フォーム内に以前の送信データが残っている可能性があるため、
	// 一度すべての子要素を削除する
	form.innerHTML = '';


	// 選択された誤字パターンIDを1件ずつ処理する
	selectedIds.forEach(id => {

		// hidden形式のinput要素を新しく作成する
		const input = document.createElement('input');

		// inputの種類をhiddenにする
		// 画面には表示されない
		input.type = 'hidden';

		// サーバー側で複数のIDを受け取れるよう、
		// すべてのinputに同じname="typoIds"を設定する
		input.name = 'typoIds';

		// 選択された誤字パターンIDを設定する
		input.value = id;

		// 作成したhidden inputをフォームに追加する
		form.appendChild(input);
	});


	// カテゴリーIDが指定されている場合
	// 空欄の場合は「汎用（全カテゴリー共通）」として扱う
	if (categoryId) {

		// カテゴリーID送信用のhidden inputを作成する
		const categoryInput = document.createElement('input');

		// inputの種類をhiddenにする
		categoryInput.type = 'hidden';

		// サーバー側でカテゴリーIDとして受け取るためのnameを設定する
		categoryInput.name = 'categoryId';

		// 選択されたカテゴリーIDを設定する
		categoryInput.value = categoryId;

		// フォームにカテゴリーIDを追加する
		form.appendChild(categoryInput);
	}


	// hidden inputを追加したフォームをサーバーに送信する
	form.submit();
});


// ============================================================
// 一括削除機能
// ============================================================


// 「選択項目を削除」ボタンがクリックされたときの処理
document.getElementById('bulkDeleteButton')?.addEventListener('click', () => {

	// 現在選択されている誤字パターンのIDを取得する
	const selectedIds = getSelectedTypoIds();


	// 1件も選択されていない場合
	if (selectedIds.length === 0) {

		// エラーメッセージを表示する
		alert('削除する項目が選択されていません。');

		// 処理を中止する
		return;
	}


	// 削除対象の件数を表示して確認する
	const ok = confirm(
		`選択した${selectedIds.length}件を削除してもよろしいですか？`
	);


	// キャンセルされた場合は処理を中止する
	if (!ok) return;


	// 一括削除用の非表示フォームを取得する
	const form = document.getElementById('bulkDeleteForm');


	// フォーム内に以前の送信データが残っている可能性があるため、
	// 一度すべての子要素を削除する
	form.innerHTML = '';


	// 選択された誤字パターンIDを1件ずつ処理する
	selectedIds.forEach(id => {

		// hidden形式のinput要素を新しく作成する
		const input = document.createElement('input');

		// inputの種類をhiddenにする
		input.type = 'hidden';

		// サーバー側で複数のIDを受け取れるよう、
		// name="typoIds"を設定する
		input.name = 'typoIds';

		// 削除対象の誤字パターンIDを設定する
		input.value = id;

		// hidden inputをフォームに追加する
		form.appendChild(input);
	});


	// hidden inputを追加したフォームをサーバーに送信する
	form.submit();
});


// ============================================================
// ページ切り替え機能
// ============================================================


// ページ番号選択用のコンボボックスが変更されたときの処理
document.getElementById('pageSelect')?.addEventListener('change', function() {

	// 選択されたページ番号を取得する
	const page = this.value;


	// URLのクエリパラメータを作成するためのオブジェクト
	const params = new URLSearchParams();


	// ページ番号が指定されている場合
	if (page) {

		// pageパラメータに選択されたページ番号を設定する
		params.set('page', page);
	}


	// 検索フォームに入力されているキーワードを取得する
	const keyword =
		document.getElementById('typoSearch')?.value;


	// キーワードが入力されている場合
	if (keyword) {

		// 検索キーワードをURLパラメータに追加する
		params.set('keyword', keyword);
	}


	// カテゴリーフィルターで選択されているカテゴリーIDを取得する
	const categoryId =
		document.getElementById('categoryFilter')?.value;


	// カテゴリーが指定されている場合
	if (categoryId) {

		// カテゴリーIDをURLパラメータに追加する
		params.set('categoryId', categoryId);
	}


	// ページ番号・検索キーワード・カテゴリーIDを付けて
	// 誤字辞典一覧ページへ移動する
	window.location.href =
		'/typo-dict/list?' + params.toString();
});