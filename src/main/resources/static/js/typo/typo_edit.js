// ============================================================
// 誤字パターン編集機能
//
// 編集ボタンのクリックで誤字・正しい表記をprompt入力させ、
// /typo-dict/update へPOST送信して一覧を再読み込みする。
// ============================================================

export function initTypoEdit() {

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
}