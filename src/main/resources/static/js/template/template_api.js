/**
 * template_api.js
 *
 * テンプレート編集機能のAPI通信を担当する。
 * テンプレートID取得、CSRFトークン取得、レイアウト保存を行う。
 */

export function getTemplateId() {
	return document.querySelector(
		'input[name="templateId"]'
	)?.value || null;
}

export function getCsrfToken() {
	return document.cookie
		.split("; ")
		.find(row => row.startsWith("XSRF-TOKEN="))
		?.split("=")[1] ?? null;
}

/**
 * テンプレート操作APIを呼び出す。
 */
export async function callLayoutApi(url, params, reloadAfterSave = false) {
	const templateId = getTemplateId();

	if (!templateId) {
		throw new Error("先にテンプレート情報を保存してください。");
	}

	const formData = new URLSearchParams({
		templateId: templateId
	});

	Object.entries(params).forEach(([key, value]) => {
		if (value !== undefined && value !== null && value !== "") {
			formData.set(key, value);
		}
	});

	const headers = {
		"Content-Type": "application/x-www-form-urlencoded"
	};

	const csrfToken = getCsrfToken();

	if (csrfToken) {
		headers["X-CSRF-TOKEN"] = decodeURIComponent(csrfToken);
	}

	const response = await fetch(url, {
		method: "POST",
		headers: headers,
		body: formData
	});

	if (!response.ok) {
		const message = await response.text();
		throw new Error(
			message || "レイアウトの保存に失敗しました。"
		);
	}

	const responseText = await response.text();
	let result = null;

	if (responseText) {
		try {
			result = JSON.parse(responseText);
		} catch {
			result = responseText;
		}
	}

	if (reloadAfterSave) {
		location.reload();
	}

	return result;
}