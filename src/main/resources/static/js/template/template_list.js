/**
 * テンプレート一覧画面の操作を担当するJavaScript
 *
 * テンプレート追加・編集画面への遷移、
 * テンプレートの登録・更新・削除・複製を行う。
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

	// テンプレートを複製する
	document.querySelectorAll(".duplicateTemplateButton").forEach(button => {

		button.addEventListener("click", async () => {

			const originalName = button.dataset.name;

			const newTemplateName = prompt(
				"複製後のテンプレート名を入力してください。",
				originalName + "（コピー）"
			);

			if (newTemplateName === null) {
				return;
			}

			if (!newTemplateName.trim()) {
				alert("テンプレート名を入力してください。");
				return;
			}

			try {

				button.disabled = true;

				const formData = new URLSearchParams();
				formData.set("templateName", newTemplateName.trim());

				const response = await fetch(
					`/template/api/${button.dataset.id}/duplicate`,
					{
						method: "POST",
						headers: {
							"Content-Type": "application/x-www-form-urlencoded",
							"X-CSRF-TOKEN": getCsrfToken()
						},
						body: formData
					}
				);

				if (!response.ok) {

					const message = await response.text();

					throw new Error(
						message || "テンプレートの複製に失敗しました。"
					);
				}

				alert("テンプレートを複製しました。");

				location.reload();

			} catch (error) {

				console.error("テンプレート複製エラー:", error);

				alert(error.message);

			} finally {

				button.disabled = false;
			}
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