
/**
 * template_select.js
 *
 * 記事編集画面でテンプレートを選択し、
 * 記事固有の設定保存・復元と本文への挿入を担当する。
 */

import { openImageSelector } from "../image/image_gallery.js";
import { restoreGrid } from "../facade/template_grid_facade.js";

let selectedTemplate = null;
let selectedTemplateSettings = null;
let insertPosition = null;

/**
 * 記事テンプレート設定の保存先IDを取得する。
 *
 * 下書きIDが存在する場合は下書きを優先する。
 */
function getArticleTemplateIds() {
	const workId = document.querySelector(
		'input[name="workId"]'
	)?.value;

	return {
		workId: workId ? Number(workId) : null,
		articleId: !workId && window.articleId
			? Number(window.articleId)
			: null
	};
}

/**
 * 記事テンプレート設定を取得する。
 */
async function fetchArticleTemplateSettings(templateId) {
	const ids = getArticleTemplateIds();

	const params = new URLSearchParams();

	if (ids.workId !== null) {
		params.set("workId", ids.workId);
	} else if (ids.articleId !== null) {
		params.set("articleId", ids.articleId);
	}

	if (params.size === 0) {
		return null;
	}

	const response = await fetch(
		`/template/api/${templateId}/article-settings?${params}`
	);

	if (!response.ok) {
		throw new Error("記事テンプレート設定の取得に失敗しました。");
	}

	return response.json();
}

/**
 * 保存済み画像のURLを取得する。
 *
 * 背景画像とIMAGEセルの画像IDをまとめて送信する。
 */
async function fetchTemplateImageUrls(settings) {
	const imageIds = new Set();

	if (settings?.backgroundImageAssetId != null) {
		imageIds.add(Number(settings.backgroundImageAssetId));
	}

	(settings?.cells ?? []).forEach(cell => {
		if (cell.imageAssetId != null) {
			imageIds.add(Number(cell.imageAssetId));
		}
	});

	if (imageIds.size === 0) {
		return {};
	}

	const csrfToken = document.querySelector(
		'input[name="_csrf"]'
	)?.value;

	const response = await fetch("/image/urls", {
		method: "POST",
		headers: {
			"Content-Type": "application/json",
			"X-CSRF-TOKEN": csrfToken ?? ""
		},
		body: JSON.stringify([...imageIds])
	});

	if (!response.ok) {
		throw new Error("保存済み画像のURL取得に失敗しました。");
	}

	return response.json();
}

/**
 * 保存済みの背景画像とIMAGEセル画像をプレビューへ反映する。
 */
function restoreTemplateImages(preview, assignments, settings, imageUrls) {

	// 背景画像を復元
	const backgroundImageId = settings?.backgroundImageAssetId;

	if (backgroundImageId != null) {
		const imageUrl = imageUrls[backgroundImageId];

		if (imageUrl) {
			const escapedUrl = imageUrl.replace(/["\\\n\r]/g, "\\$&");

			preview.style.backgroundImage = `url("${escapedUrl}")`;
			preview.style.backgroundSize = "cover";
			preview.style.backgroundPosition = "center";
			preview.style.backgroundRepeat = "no-repeat";
		}
	}

	// IMAGEセルの画像を復元
	const savedCellMap = new Map(
		(settings?.cells ?? []).map(cell => [
			String(cell.cellId),
			cell
		])
	);

	preview.querySelectorAll(
		'[data-cell-type="IMAGE"]'
	).forEach(cell => {
		const savedCell = savedCellMap.get(
			String(cell.dataset.cellId)
		);

		if (savedCell?.imageAssetId == null) {
			return;
		}

		const imageUrl = imageUrls[savedCell.imageAssetId];

		if (!imageUrl) {
			return;
		}

		const imageElement = document.createElement("img");

		imageElement.src = imageUrl;
		imageElement.alt = "";
		imageElement.dataset.imageAssetId = savedCell.imageAssetId;
		imageElement.style.maxWidth = "100%";
		imageElement.style.maxHeight = "100%";
		imageElement.style.objectFit = "contain";

		if (cell.dataset.imageWidth) {
			imageElement.style.width =
				`${cell.dataset.imageWidth}px`;
		}

		cell.replaceChildren(imageElement);

		cell.dataset.articleImageAssetId =
			savedCell.imageAssetId;
	});

	// 画像選択ボタンの表示を更新
	const backgroundButton = assignments.querySelector(
		".template-background-image-assignment button"
	);

	if (backgroundButton && backgroundImageId != null
		&& imageUrls[backgroundImageId]) {
		backgroundButton.textContent = "背景画像を変更";
	}

}

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
	selectedTemplateSettings = null;

	list.innerHTML = "<p>テンプレートを読み込んでいます。</p>";

	if (previewArea) {
		previewArea.style.display = "none";
	}

	document.getElementById("saveTemplateButton").disabled = true;
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
 * 選択したテンプレートと記事固有設定を取得する。
 */
async function selectTemplate(template) {
	try {
		const response = await fetch(
			`/template/api/${template.templateId}/data`
		);

		if (!response.ok) {
			throw new Error("テンプレートの取得に失敗しました。");
		}

		const result = await response.json();

		selectedTemplate = template;
		selectedTemplateSettings =
			await fetchArticleTemplateSettings(template.templateId);

		renderTemplatePreview(
			result,
			selectedTemplateSettings,
			template.backgroundColor
		);

		const preview = document.getElementById("templatePreview")
			.querySelector(".template-preview-grid");

		const assignments = document.getElementById(
			"templateImageAssignments"
		);

		const imageUrls = await fetchTemplateImageUrls(
			selectedTemplateSettings
		);

		restoreTemplateImages(
			preview,
			assignments,
			selectedTemplateSettings,
			imageUrls
		);

		document.getElementById("insertTemplateButton").disabled = false;

		const ids = getArticleTemplateIds();

		document.getElementById("saveTemplateButton").disabled =
			ids.workId === null && ids.articleId === null;

	} catch (error) {
		console.error(error);
		alert("テンプレートを表示できませんでした。");
	}
}

/**
 * 選択テンプレートをプレビュー表示する。
 */
function renderTemplatePreview(result, settings, masterBackgroundColor) {
	const previewArea = document.getElementById("templatePreviewArea");
	const preview = document.getElementById("templatePreview");
	const assignments = document.getElementById("templateImageAssignments");
	const variableInputs = document.getElementById("templateVariableInputs");

	if (!previewArea || !preview || !assignments || !variableInputs) {
		console.error("テンプレートプレビュー領域が見つかりません。");
		return;
	}

	const rows = JSON.parse(result.rowsJson || "[]");
	const columns = JSON.parse(result.columnsJson || "[]");
	const cells = JSON.parse(result.cellsJson || "[]");

	preview.replaceChildren();
	assignments.replaceChildren();
	variableInputs.replaceChildren();

	if (rows.length === 0 || columns.length === 0) {
		throw new Error("テンプレートの行・列情報がありません。");
	}

	// 編集画面と同じ復元処理を使い、プレビュー用グリッドを生成する。
	// 選択画面ではセル選択イベントやサイズ変更ハンドルは生成しない。
	const templateElement = document.createElement("div");

	templateElement.className = "template-grid template-preview-grid";
	templateElement.style.display = "grid";
	templateElement.style.width = "fit-content";
	templateElement.style.position = "relative";

	restoreGrid({
		grid: templateElement,
		rows,
		columns,
		cells
	});

	preview.appendChild(templateElement);

	renderVariableInputs(
		preview,
		variableInputs,
		settings?.cells ?? []
	);

	renderBackgroundImageSelector(
		templateElement,
		assignments,
		settings
	);

	// 	// マスター背景色を初期値として、記事側の設定があればそちらを優先する。
	renderBackgroundColorSelector(
		templateElement,
		assignments,
		settings,
		masterBackgroundColor
	);

	renderImageAssignments(
		templateElement,
		assignments,
		settings?.cells ?? []
	);

	previewArea.style.display = "block";
}

/**
 * 背景画像の選択欄を生成する。
 */
function renderBackgroundImageSelector(preview, assignments, settings) {
	const wrapper = document.createElement("div");
	wrapper.className = "template-background-image-assignment";

	const label = document.createElement("p");
	label.textContent = "テンプレート背景画像";

	const button = document.createElement("button");

	button.type = "button";
	button.textContent = settings?.backgroundImageAssetId
		? `背景画像設定済み（ID: ${settings.backgroundImageAssetId}）`
		: "背景画像を選択";

	if (settings?.backgroundImageAssetId) {
		preview.dataset.backgroundImageAssetId =
			settings.backgroundImageAssetId;
	}

	button.addEventListener("click", () => {
		button.disabled = true;

		try {
			openImageSelector(image => {
				const escapedUrl = image.url.replace(/["\\\n\r]/g, "\\$&");

				preview.style.backgroundImage = `url("${escapedUrl}")`;
				preview.style.backgroundSize = "cover";
				preview.style.backgroundPosition = "center";
				preview.style.backgroundRepeat = "no-repeat";

				preview.dataset.backgroundImageAssetId = image.imageId;

				button.textContent = "背景画像を変更";
			});
		} catch (error) {
			console.error(error);
			alert("背景画像を選択できませんでした。");
		} finally {
			button.disabled = false;
		}
	});

	wrapper.appendChild(label);
	wrapper.appendChild(button);

	assignments.appendChild(wrapper);
}

/**
 * 記事用テンプレートの背景色設定UIを生成する。
 *
 * テンプレート編集画面と同じ背景色・透過率の同期処理を使用する。
 * 記事側の保存値を優先し、未設定の場合はマスターの背景色を初期値にする。
 */
function renderBackgroundColorSelector(
	preview,
	assignments,
	settings,
	masterBackgroundColor
) {
	const wrapper = document.createElement("div");
	wrapper.className = "template-background-color-assignment";

	const label = document.createElement("label");
	label.textContent = "テンプレート背景色";

	const enabled = document.createElement("input");
	enabled.type = "checkbox";

	const colorInput = document.createElement("input");
	colorInput.type = "color";

	const opacityInput = document.createElement("input");
	opacityInput.type = "range";
	opacityInput.min = "0";
	opacityInput.max = "100";
	opacityInput.step = "1";

	const opacityValue = document.createElement("output");

	const hiddenInput = document.createElement("input");
	hiddenInput.type = "hidden";

	// 記事側の保存値がある場合だけ有効にする。
	// マスター背景色は初期表示に使用するだけで、記事側には保存しない。
	const hasArticleBackgroundColor = !!settings?.backgroundColor;

	const savedColor = settings?.backgroundColor
		|| masterBackgroundColor
		|| "#FFFFFF00";

	enabled.checked = hasArticleBackgroundColor;

	/**
	 * 背景色と透過率の入力値を同期する。
	 *
	 * テンプレート編集画面と同じ8桁HEX形式で保持する。
	 */
	function updateBackgroundColor() {
		const color = colorInput.value;
		const transparency = Number(opacityInput.value);

		const alpha = Math.round(
			(100 - transparency) * 255 / 100
		);

		const alphaHex = alpha
			.toString(16)
			.padStart(2, "0")
			.toUpperCase();

		hiddenInput.value = color.toUpperCase() + alphaHex;
		opacityValue.textContent = `${transparency}%`;

		preview.dataset.backgroundColorEnabled =
			String(enabled.checked);

		preview.dataset.backgroundColor = hiddenInput.value;

		preview.style.backgroundColor = enabled.checked
			? hiddenInput.value
			: "";
	}

	// 保存済みの8桁HEX形式の背景色を入力欄へ反映する。
	if (/^#[0-9A-Fa-f]{8}$/.test(savedColor)) {
		colorInput.value = savedColor.substring(0, 7);

		const alpha = parseInt(savedColor.substring(7, 9), 16);

		opacityInput.value = Math.round(
			(255 - alpha) * 100 / 255
		);
	} else {
		colorInput.value = "#FFFFFF";
		opacityInput.value = 100;
	}

	// マスター背景色は初期表示に反映するが、記事側の設定としては有効にしない。
	colorInput.disabled = !enabled.checked;
	opacityInput.disabled = !enabled.checked;

	preview.dataset.backgroundColorEnabled = String(enabled.checked);
	preview.dataset.backgroundColor = savedColor;
	preview.style.backgroundColor = savedColor;

	// 色・透過率の変更を同期する。
	colorInput.addEventListener("input", updateBackgroundColor);
	opacityInput.addEventListener("input", updateBackgroundColor);

	enabled.addEventListener("change", () => {
		colorInput.disabled = !enabled.checked;
		opacityInput.disabled = !enabled.checked;

		updateBackgroundColor();
	});

	label.prepend(enabled);

	wrapper.appendChild(label);
	wrapper.appendChild(colorInput);
	wrapper.appendChild(opacityInput);
	wrapper.appendChild(opacityValue);
	wrapper.appendChild(hiddenInput);

	assignments.appendChild(wrapper);

	// 初期値も8桁HEX形式に同期する。
	updateBackgroundColor();
}

/**
 * 記事用の変数入力欄を表示する。
 *
 * テンプレート編集画面で作成済みの表示内容は変更せず、
 * VARIABLEセルの表示文字だけを記事用の入力値に置き換える。
 *
 * 同じ変数名を持つセルは、ひとつの入力欄でまとめて更新する。
 * マスターテンプレートのHTML・CSSやセル構造は再生成しない。
 */
function renderVariableInputs(preview, variableInputs, savedCells) {
	const variableElements = preview.querySelectorAll(
		'[data-cell-type="VARIABLE"]'
	);

	const variableMap = new Map();

	variableElements.forEach(element => {
		const variableName = element.dataset.variableName;

		if (!variableName) {
			return;
		}

		if (!variableMap.has(variableName)) {
			variableMap.set(variableName, []);
		}

		variableMap.get(variableName).push(element);
	});

	variableMap.forEach((elements, variableName) => {
		const savedCell = savedCells?.find(
			cell => cell.variableName === variableName
		);

		const savedValue = savedCell?.variableValue ?? "";

		const label = document.createElement("label");
		label.textContent = variableName;

		const input = document.createElement("input");
		input.type = "text";
		input.name = "templateVariable_" + variableName;
		input.placeholder = `「${variableName}」の値を入力`;
		input.value = savedValue;

		const updateDisplay = value => {
			elements.forEach(element => {
				element.dataset.articleVariableValue = value;
				element.textContent = value;
			});
		};

		updateDisplay(savedValue);

		input.addEventListener("input", () => {
			updateDisplay(input.value);
		});

		label.appendChild(input);
		variableInputs.appendChild(label);
	});
}

/**
 * IMAGEセルの画像割り当て欄を生成する。
 */
function renderImageAssignments(templateElement, assignments, savedCells) {
	const savedCellMap = new Map(
		savedCells.map(cell => [
			String(cell.cellId),
			cell
		])
	);

	const imageCells = templateElement.querySelectorAll(
		'[data-cell-type="IMAGE"]'
	);

	imageCells.forEach(cell => {
		const savedCell = savedCellMap.get(String(cell.dataset.cellId));

		if (savedCell?.imageAssetId != null) {
			cell.dataset.articleImageAssetId = savedCell.imageAssetId;
		}

		const button = document.createElement("button");

		button.type = "button";
		button.textContent = savedCell?.imageAssetId
			? `画像設定済み（ID: ${savedCell.imageAssetId}）`
			: "画像を選択";

		button.addEventListener("click", () => {
			selectImageForCell(cell, button);
		});

		const label = document.createElement("p");
		label.textContent = `画像枠：${cell.dataset.cellId}`;

		assignments.appendChild(label);
		assignments.appendChild(button);
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
			imageElement.style.maxWidth = "100%";
			imageElement.style.maxHeight = "100%";
			imageElement.style.objectFit = "contain";

			if (placeholder.dataset.imageWidth) {
				imageElement.style.width =
					`${placeholder.dataset.imageWidth}px`;
			}

			placeholder.dataset.articleImageAssetId = image.imageId;

			placeholder.replaceChildren(imageElement);

			button.textContent = "画像を変更";
		});
	} catch (error) {
		console.error(error);
		alert("画像選択画面を開けませんでした。");
	} finally {
		button.disabled = false;
	}
}

/**
 * 記事固有のテンプレート設定を保存する。
 */
async function saveTemplateSettings() {
	if (!selectedTemplate) {
		return;
	}

	const ids = getArticleTemplateIds();

	if (ids.workId === null && ids.articleId === null) {
		alert("記事または下書きのIDがないため、テンプレート設定を保存できません。");
		return;
	}

	const templateElement = document.querySelector(
		"#templatePreview .template-preview-grid"
	);

	if (!templateElement) {
		return;
	}

	const cells = [];

	templateElement.querySelectorAll(
		'[data-cell-id][data-cell-type]'
	).forEach(cell => {
		const cellType = cell.dataset.cellType;

		if (cellType === "VARIABLE") {
			cells.push({
				cellId: Number(cell.dataset.cellId),
				variableValue: cell.dataset.articleVariableValue ?? ""
			});
		} else if (cellType === "IMAGE") {
			cells.push({
				cellId: Number(cell.dataset.cellId),
				imageAssetId: cell.dataset.articleImageAssetId
					? Number(cell.dataset.articleImageAssetId)
					: null
			});
		}
	});

	const backgroundColor = templateElement.dataset.backgroundColorEnabled === "true"
		? templateElement.dataset.backgroundColor
		: null;

	const requestBody = {
		workId: ids.workId,
		articleId: ids.articleId,
		templateId: selectedTemplate.templateId,
		backgroundImageAssetId:
			templateElement.dataset.backgroundImageAssetId
				? Number(templateElement.dataset.backgroundImageAssetId)
				: null,
		backgroundColor,
		cells
	};

	const csrfToken = document.querySelector(
		'input[name="_csrf"]'
	)?.value;

	const saveButton = document.getElementById("saveTemplateButton");

	saveButton.disabled = true;

	try {
		const response = await fetch("/template/api/article-settings", {
			method: "POST",
			headers: {
				"Content-Type": "application/json",
				"X-CSRF-TOKEN": csrfToken ?? ""
			},
			body: JSON.stringify(requestBody)
		});

		if (!response.ok) {
			throw new Error("テンプレート設定の保存に失敗しました。");
		}

		selectedTemplateSettings = {
			backgroundImageAssetId: requestBody.backgroundImageAssetId,
			backgroundColor: requestBody.backgroundColor,
			cells
		};

		alert("テンプレート設定を保存しました。");

	} catch (error) {
		console.error(error);
		alert("テンプレート設定を保存できませんでした。");
	} finally {
		saveButton.disabled = false;
	}
}

/**
 * 完成したテンプレートを記事本文へ挿入する。
 *
 * 設定の保存は行わず、本文へのHTML挿入だけを担当する。
 */
function insertTemplate() {
	const textarea = document.getElementById("content");
	const preview = document.getElementById("templatePreview");

	if (!textarea || !preview || !selectedTemplate) {
		return;
	}

	const templateElement = preview.querySelector(
		".template-preview-grid"
	);

	if (!templateElement) {
		return;
	}

	const clone = templateElement.cloneNode(true);

	clone.querySelectorAll("button").forEach(button => {
		button.remove();
	});

	const insertHtml = clone.outerHTML;
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

	document.getElementById("saveTemplateButton")
		?.addEventListener("click", saveTemplateSettings);

	document.getElementById("insertTemplateButton")
		?.addEventListener("click", insertTemplate);

	document.getElementById("closeTemplateModalButton")
		?.addEventListener("click", closeTemplateModal);
});
