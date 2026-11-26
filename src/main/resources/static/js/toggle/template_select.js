
/**
 * template_select.js
 *
 * 記事編集画面でテンプレートを選択し、
 * IMAGEセルへの画像割り当てと本文への挿入を担当する。
 */
import { openImageSelector } from "../image/image_gallery.js";

let selectedTemplate = null;
let selectedTemplateHtml = "";
let selectedTemplateCss = "";
let insertPosition = null;

/**
 * テンプレート選択モーダルを開く。
 */
async function openTemplateModal() {

	const modal = document.getElementById("templateSelectModal");
	const list = document.getElementById("templateSelectList");
	const previewArea = document.getElementById("templatePreviewArea");

	if (!modal || !list) {
		console.error("テンプレート選択モーダルが見つかりません。");
		return;
	}

	const textarea = document.getElementById("content");

	insertPosition = textarea
		? textarea.selectionStart
		: null;

	selectedTemplate = null;
	selectedTemplateHtml = "";
	selectedTemplateCss = "";

	list.innerHTML = "<p>テンプレートを読み込んでいます。</p>";

	if (previewArea) {
		previewArea.style.display = "none";
	}

	document.getElementById("insertTemplateButton").disabled = true;

	modal.style.display = "block";

	try {

		const response = await fetch("/template/api/list");

		if (!response.ok) {
			throw new Error("テンプレート一覧の取得に失敗しました。");
		}

		const templates = await response.json();

		list.innerHTML = "";

		if (!templates.length) {
			list.innerHTML = "<p>登録済みテンプレートがありません。</p>";
			return;
		}

		templates.forEach(template => {

			const button = document.createElement("button");

			button.type = "button";
			button.textContent = template.templateName;
			button.dataset.templateId = template.templateId;

			button.addEventListener("click", () => {
				selectTemplate(template);
			});

			list.appendChild(button);
		});

	} catch (error) {

		console.error(error);
		list.innerHTML = "<p>テンプレートを取得できませんでした。</p>";
	}
}

/**
 * 選択したテンプレートのHTML・CSSを取得する。
 */
async function selectTemplate(template) {

	try {

		const response = await fetch(
			`/template/api/${template.templateId}/render`
		);

		if (!response.ok) {
			throw new Error("テンプレートの取得に失敗しました。");
		}

		const result = await response.json();

		selectedTemplate = template;
		selectedTemplateHtml = result.html;
		selectedTemplateCss = result.css;

		renderTemplatePreview();


		document.getElementById("insertTemplateButton").disabled = false;

	} catch (error) {

		console.error(error);
		alert("テンプレートを表示できませんでした。");
	}
}

/**
 * 選択テンプレートをプレビュー表示する。
 *
 * VARIABLEセルの入力欄とIMAGEセルの画像割り当て欄も生成する。
 */
function renderTemplatePreview() {

	const previewArea = document.getElementById("templatePreviewArea");
	const preview = document.getElementById("templatePreview");
	const assignments = document.getElementById("templateImageAssignments");
	const variableInputs = document.getElementById("templateVariableInputs");

	if (!previewArea || !preview || !assignments || !variableInputs) {
		console.error("テンプレートプレビュー領域が見つかりません。");
		return;
	}

	preview.innerHTML = selectedTemplateHtml;

	assignments.innerHTML = "";
	variableInputs.innerHTML = "";

	// プレビュー専用CSS
	let style = document.getElementById("templatePreviewStyle");

	if (!style) {
		style = document.createElement("style");
		style.id = "templatePreviewStyle";
		document.head.appendChild(style);
	}

	style.textContent = selectedTemplateCss;

	// VARIABLEセルの入力欄を生成する。
	renderVariableInputs(preview, variableInputs);

	// IMAGEセルの画像割り当て
	const placeholders = preview.querySelectorAll(
		"[data-template-image-cell]"
	);

	placeholders.forEach(placeholder => {

		const cellId = placeholder.dataset.templateImageCell;

		const button = document.createElement("button");
		button.type = "button";
		button.textContent = "画像を選択";

		button.addEventListener("click", () => {
			selectImageForCell(placeholder, button);
		});

		placeholder.appendChild(button);

		const label = document.createElement("p");
		label.textContent = `画像枠：${cellId}`;

		assignments.appendChild(label);
	});

	previewArea.style.display = "block";
}

/**
 * VARIABLEセルごとの入力欄を生成する。
 *
 * 同じ変数名が複数のセルに使用されている場合は、
 * 入力欄を1つにまとめ、すべての該当セルに値を反映する。
 */
function renderVariableInputs(preview, variableInputs) {

	const variableElements = preview.querySelectorAll(
		"[data-template-variable]"
	);

	if (variableElements.length === 0) {
		return;
	}

	const variableGroups = new Map();

	variableElements.forEach(element => {

		const variableName = element.dataset.templateVariable;

		if (!variableName) {
			return;
		}

		if (!variableGroups.has(variableName)) {
			variableGroups.set(variableName, []);
		}

		variableGroups.get(variableName).push(element);
	});

	if (variableGroups.size === 0) {
		return;
	}

	const heading = document.createElement("h3");
	heading.textContent = "テンプレート変数の入力";

	variableInputs.appendChild(heading);

	variableGroups.forEach((elements, variableName) => {

		const wrapper = document.createElement("div");
		wrapper.className = "template-variable-input";

		const label = document.createElement("label");
		label.textContent = variableName;

		const input = document.createElement("input");
		input.type = "text";
		input.name = "templateVariable_" + variableName;
		input.placeholder = "「" + variableName + "」の値を入力";
		input.value = elements[0].textContent;

		label.appendChild(input);
		wrapper.appendChild(label);

		// 入力値をプレビュー内の該当する変数セルへ反映する。
		input.addEventListener("input", () => {

			elements.forEach(element => {
				element.textContent = input.value;
			});
		});

		variableInputs.appendChild(wrapper);
	});
}

/**
 * 指定されたIMAGEセルに画像を割り当てる。
 */
function selectImageForCell(placeholder, button) {

	button.disabled = true;

	try {

		openImageSelector(image => {

			const imageElement = document.createElement("img");

			imageElement.src = image.url;
			imageElement.alt = image.fileName || "";
			imageElement.dataset.imageAssetId = image.imageId;

			// テンプレート側で指定された画像サイズをCSSで適用する。
			placeholder.replaceChildren(imageElement);

			const changeButton = document.createElement("button");

			changeButton.type = "button";
			changeButton.textContent = "画像を変更";

			changeButton.addEventListener("click", () => {
				selectImageForCell(placeholder, changeButton);
			});

			placeholder.appendChild(changeButton);
		});

	} catch (error) {

		console.error(error);
		alert("画像選択画面を開けませんでした。");

	} finally {

		button.disabled = false;
	}
}

/**
 * 完成したテンプレートを記事本文へ挿入する。
 *
 * プレビュー用の画像選択・変更ボタンを除去し、
 * 画像割り当て済みのHTMLだけを記事本文へ挿入する。
 */
function insertTemplate() {

	const textarea = document.getElementById("content");
	const preview = document.getElementById("templatePreview");

	if (!textarea || !preview || !selectedTemplate) {
		return;
	}

	// プレビューを複製して、元のプレビュー表示には影響させない。
	const clone = preview.cloneNode(true);

	// 画像選択・変更などの操作用ボタンを除去する。
	clone.querySelectorAll("button").forEach(button => {
		button.remove();
	});

	const insertHtml = clone.innerHTML;

	const position = insertPosition ?? textarea.selectionStart;

	textarea.value =
		textarea.value.substring(0, position)
		+ "\n"
		+ insertHtml
		+ "\n"
		+ textarea.value.substring(position);

	const cursorPosition = position + insertHtml.length + 2;

	textarea.focus();
	textarea.selectionStart = cursorPosition;
	textarea.selectionEnd = cursorPosition;

	// 既存の入力監視処理にも変更を通知する。
	textarea.dispatchEvent(new Event("input", {
		bubbles: true
	}));

	closeTemplateModal();
}

/**
 * テンプレート選択モーダルを閉じる。
 */
function closeTemplateModal() {

	const modal = document.getElementById("templateSelectModal");

	if (modal) {
		modal.style.display = "none";
	}
}

/**
 * イベント登録
 */
document.addEventListener("DOMContentLoaded", () => {

	document.getElementById("templateButton")
		?.addEventListener("click", openTemplateModal);

	document.getElementById("insertTemplateButton")
		?.addEventListener("click", insertTemplate);

	document.getElementById("closeTemplateModalButton")
		?.addEventListener("click", closeTemplateModal);
});
