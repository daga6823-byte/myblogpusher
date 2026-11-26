/**
 * template_save.js
 *
 * テンプレート本体の新規作成・更新を担当する。
 *
 * 保存APIを非同期で呼び出し、ページを再読み込みせずに
 * 保存後のテンプレートIDを画面へ反映する。
 *
 * 背景色と透過率の入力値も同期する。
 */
import { getCsrfToken } from "./template_api.js";

/**
 * 8桁HEX形式の背景色を入力欄へ反映する。
 */
function initializeBackgroundColor() {

	const colorInput = document.getElementById("backgroundColor");
	const opacityInput = document.getElementById("backgroundOpacity");
	const opacityValue = document.getElementById("backgroundOpacityValue");
	const hiddenInput = document.getElementById("backgroundColorValue");

	if (!colorInput || !opacityInput || !opacityValue || !hiddenInput) {
		return;
	}

	/**
	 * 色と透過率から保存用の8桁HEX値を生成する。
	 */
	function updateBackgroundColor() {

		const color = colorInput.value;

		const transparency = Number(opacityInput.value);
		const alpha = Math.round((100 - transparency) * 255 / 100);

		const alphaHex = alpha.toString(16).padStart(2, "0").toUpperCase();

		hiddenInput.value = color.toUpperCase() + alphaHex;

		opacityValue.textContent = transparency + "%";
	}

	/**
	 * 保存済みの背景色を入力欄へ反映する。
	 */
	const savedColor = hiddenInput.value;

	if (/^#[0-9A-Fa-f]{8}$/.test(savedColor)) {

		colorInput.value = savedColor.substring(0, 7);

		const alpha = parseInt(savedColor.substring(7, 9), 16);
		const transparency = Math.round((255 - alpha) * 100 / 255);

		opacityInput.value = transparency;

	} else {

		colorInput.value = "#FFFFFF";
		opacityInput.value = 100;
	}

	colorInput.addEventListener("input", updateBackgroundColor);
	opacityInput.addEventListener("input", updateBackgroundColor);

	updateBackgroundColor();
}

/**
 * テンプレート保存処理を初期化する。
 */
function initializeTemplateSave() {

	const form = document.getElementById("templateForm");

	if (!form) return;

	initializeBackgroundColor();

	form.addEventListener("submit", async (event) => {

		event.preventDefault();

		const submitButton = form.querySelector('button[type="submit"]');

		if (submitButton?.disabled) return;

		try {

			if (submitButton) submitButton.disabled = true;

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

				throw new Error(message || "テンプレートの保存に失敗しました。");
			}

			const result = await response.json();

			const templateIdInput = form.querySelector('input[name="templateId"]');

			if (templateIdInput) {
				templateIdInput.value = result.templateId;
			}

			alert("テンプレートを保存しました。");

		} catch (error) {

			console.error("テンプレート保存エラー:", error);

			alert(error.message || "テンプレートの保存に失敗しました。");

		} finally {

			if (submitButton) submitButton.disabled = false;
		}
	});
}

document.addEventListener("DOMContentLoaded", initializeTemplateSave);