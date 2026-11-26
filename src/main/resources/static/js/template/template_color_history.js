/**
 * ユーザーごとの色履歴を担当する。
 *
 * 色履歴の取得と画面表示、使用した色の履歴登録を行う。
 */
import { callLayoutApi } from "./template_api.js";

/**
 * 色履歴を取得して画面に表示する。
 */
export async function loadColorHistory() {
	const container = document.getElementById("templateColorHistory");

	if (!container) {
		return;
	}

	try {
		const response = await fetch("/template/color/history");

		if (!response.ok) {
			throw new Error("色履歴を取得できませんでした。");
		}

		const colors = await response.json();
		container.replaceChildren();

		colors.forEach(color => {
			if (!color.colorCode) {
				return;
			}

			const button = document.createElement("button");
			button.type = "button";
			button.className = "template-color-history-item";
			button.dataset.color = color.colorCode;
			button.title = color.colorCode;
			button.style.backgroundColor = color.colorCode;
			button.setAttribute("aria-label", color.colorCode);

			container.appendChild(button);
		});
	} catch (error) {
		console.error(error);
	}
}

/**
 * 使用した色を履歴に登録する。
 */
export async function saveColorHistory(colorCode) {
	if (!colorCode) {
		return;
	}

	try {
		await callLayoutApi(
			"/template/color/save",
			{ colorValue: colorCode }
		);

		await loadColorHistory();
	} catch (error) {
		console.error("色履歴の登録に失敗しました。", error);
	}
}