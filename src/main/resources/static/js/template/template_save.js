/**
 * template_save.js
 *
 * テンプレート本体の新規作成・更新を担当する。
 *
 * 保存APIを非同期で呼び出し、ページを再読み込みせずに
 * 保存後のテンプレートIDを画面へ反映する。
 */

import { getCsrfToken } from "./template_api.js";

/**
 * テンプレート保存フォームを初期化する。
 */
function initializeTemplateSave() {

	const form = document.getElementById("templateForm");

	if (!form) {
		return;
	}

	form.addEventListener("submit", async (event) => {

		event.preventDefault();

		const submitButton = form.querySelector(
			'button[type="submit"]'
		);

		if (submitButton?.disabled) {
			return;
		}

		try {

			if (submitButton) {
				submitButton.disabled = true;
			}

			const formData = new URLSearchParams();

			new FormData(form).forEach((value, key) => {

				if (typeof value === "string") {
					formData.append(key, value);
				}

			});

			const headers = {
				"Content-Type": "application/x-www-form-urlencoded"
			};

			const csrfToken = getCsrfToken();

			if (csrfToken) {
				headers["X-CSRF-TOKEN"] = decodeURIComponent(csrfToken);
			}

			const response = await fetch("/template/api/save", {
				method: "POST",
				headers: headers,
				body: formData
			});

			if (!response.ok) {
				const message = await response.text();
				throw new Error(
					message || "テンプレートの保存に失敗しました。"
				);
			}

			const result = await response.json();

			// 新規作成時に発行されたIDをフォームへ反映する。
			const templateIdInput = form.querySelector(
				'input[name="templateId"]'
			);

			if (templateIdInput) {
				templateIdInput.value = result.templateId;
			}

			alert("テンプレートを保存しました。");

		} catch (error) {

			console.error("テンプレート保存エラー:", error);

			alert(error.message || "テンプレートの保存に失敗しました。");

		} finally {

			if (submitButton) {
				submitButton.disabled = false;
			}

		}

	});
}

document.addEventListener(
	"DOMContentLoaded",
	initializeTemplateSave
);