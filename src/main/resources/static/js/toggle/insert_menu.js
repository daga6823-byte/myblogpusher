// =====================================================
//
// insert_menu.js
//
// 挿入メニューの表示・非表示を管理する
// ・+ボタンによるメニュー開閉
// ・メニュー外クリックによるメニュー閉じる
//
// =====================================================

// -----------------------------------------------------
// 挿入メニューの表示・非表示切替
// -----------------------------------------------------

document.getElementById('insertMenuButton').addEventListener('click', function() {

	const menu = document.getElementById('insertMenu');

	menu.style.display =

		menu.style.display === 'none'

			? 'block'

			: 'none';

});

// -----------------------------------------------------
// 挿入メニューの外側をクリックしたら閉じる
// -----------------------------------------------------

document.addEventListener('click', function(event) {

	const menu = document.getElementById('insertMenu');

	const button = document.getElementById('insertMenuButton');

	// メニューまたは+ボタンが存在しない場合は何もしない。

	if (!menu || !button) {

		return;

	}

	// メニューが閉じている場合は何もしない。

	if (menu.style.display !== 'block') {

		return;

	}

	// メニュー内、または+ボタンをクリックした場合は閉じない。

	if (

		menu.contains(event.target)

		|| button.contains(event.target)

	) {

		return;

	}

	// メニュー外をクリックした場合は閉じる。

	menu.style.display = 'none';

});
