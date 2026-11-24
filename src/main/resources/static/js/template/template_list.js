
/**
 * テンプレート一覧画面の操作を担当するJavaScript
 *
 * テンプレート追加・編集モーダルの表示、
 * テンプレートの登録・更新・削除を行う。
 */
document.addEventListener("DOMContentLoaded", () => {

	const modal = document.getElementById("templateModal");
	const modalTitle = document.getElementById("templateModalTitle");
	const templateId = document.getElementById("templateId");
	const templateName = document.getElementById("templateName");
	const templateKey = document.getElementById("templateKey");
	const templateDescription = document.getElementById("templateDescription");
	const addButton = document.getElementById("addTemplateButton");
	const saveButton = document.getElementById("saveTemplateButton");
	const cancelButton = document.getElementById("cancelTemplateButton");

	// 新規テンプレート追加モーダルを開く
	addButton.addEventListener("click", () => {
		templateId.value = "";
		templateName.value = "";
		templateKey.value = "";
		templateDescription.value = "";
		modalTitle.textContent = "テンプレート追加";
		modal.style.display = "block";
	});

	// モーダルを閉じる
	cancelButton.addEventListener("click", () => {
		modal.style.display = "none";
	});

	// テンプレートを登録・更新する
	saveButton.addEventListener("click", async () => {

		const id = templateId.value;

		const data = {
			templateName: templateName.value,
			templateKey: templateKey.value,
			templateDescription: templateDescription.value
		};

		if (!data.templateName.trim() || !data.templateKey.trim()) {
			alert("テンプレート名と識別名を入力してください。");
			return;
		}

		const url = id
			? `/template/api/${id}`
			: "/template/api";

		const method = id ? "PUT" : "POST";

		try {
			const response = await fetch(url, {
				method: method,
				headers: {
					"Content-Type": "application/json",
					"X-CSRF-TOKEN": getCsrfToken()
				},
				body: JSON.stringify(data)
			});

			if (!response.ok) {
				throw new Error("テンプレートの保存に失敗しました。");
			}

			location.reload();

		} catch (error) {
			alert(error.message);
		}
	});

	// 編集ボタンからテンプレート情報をモーダルに設定する
	document.querySelectorAll(".editTemplateButton").forEach(button => {
		button.addEventListener("click", () => {

			templateId.value = button.dataset.id;
			templateName.value = button.dataset.name;
			templateKey.value = button.dataset.key;
			templateDescription.value = button.dataset.description || "";

			modalTitle.textContent = "テンプレート編集";
			modal.style.display = "block";
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
