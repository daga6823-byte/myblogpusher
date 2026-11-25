
/**
 * テンプレート一覧画面の操作を担当するJavaScript
 *
 * テンプレート追加・編集モーダルの表示、
 * テンプレートの登録・更新・削除を行う。
 */
document.addEventListener("DOMContentLoaded", () => {

	const addButton = document.getElementById("addTemplateButton");

	// 新規テンプレート編集画面を開く
	addButton.addEventListener("click", () => {
		location.href = "/template/new";
	});

	// 編集ボタンからテンプレート編集画面を開く
	document.querySelectorAll(".editTemplateButton").forEach(button => {
		button.addEventListener("click", () => {
			location.href = `/template/edit?templateId=${button.dataset.id}`;
		});
	});

	// テンプレートを削除する
	document.querySelectorAll(".deleteTemplateButton").forEach(button => {
		button.addEventListener("click", async () => {

			if (!confirm("このテンプレートを削除しますか？")) {
				return;
			}

			try {
				const response = await fetch(
					`/template/api/${button.dataset.id}`,
					{
						method: "DELETE",
						headers: {
							"X-CSRF-TOKEN": getCsrfToken()
						}
					}
				);

				if (!response.ok) {
					throw new Error("テンプレートの削除に失敗しました。");
				}

				location.reload();

			} catch (error) {
				alert(error.message);
			}
		});
	});

	// CSRFトークンを取得する
	function getCsrfToken() {
		return document.cookie
			.split("; ")
			.find(row => row.startsWith("XSRF-TOKEN="))
			?.split("=")[1] || "";
	}
});
