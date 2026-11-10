// ============================================================
// テーブルのソート機能
//
// class="sortable" のヘッダーをクリックすると、
// data-sort属性のキーで誤字一覧テーブルの行を昇順・降順に並べ替える。
// ============================================================

// 各列の現在のソート状態を保持するオブジェクト
// 例：{ category: 'asc' }
//     { wrong: 'desc' }
let sortState = {};


export function initTypoSort() {

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
}