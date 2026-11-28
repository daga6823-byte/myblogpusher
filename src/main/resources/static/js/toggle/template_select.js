
/**
 * template_select.js
 *
 * 記事編集画面でテンプレートを選択し、
 * 記事固有の設定保存・復元と本文への挿入を担当する。
 */

import { openImageSelector } from "../image/image_gallery.js";
import { createCellFromData } from "../template/grid/template_grid_cell_render.js";

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

		renderTemplatePreview(result, selectedTemplateSettings);

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
function renderTemplatePreview(result, settings) {
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

	const templateElement = document.createElement("div");

	templateElement.className = "template-grid template-preview-grid";
	templateElement.style.setProperty("--column-count", columns.length);
	templateElement.style.display = "grid";
	templateElement.style.width = "fit-content";

	templateElement.style.gridTemplateColumns = columns.map(column => {
		const width = column.width ?? 1;
		const unit = column.widthUnit ?? "fr";

		return unit === "px"
			? `${width * 5}px`
			: `${width}${unit}`;
	}).join(" ");

	templateElement.style.gridTemplateRows = rows.map(row => {
		const height = row.height ?? "auto";
		const unit = row.heightUnit ?? "auto";

		if (unit === "auto") {
			return "auto";
		}

		return unit === "px"
			? `${height * 5}px`
			: `${height}${unit}`;
	}).join(" ");

	templateElement.style.position = "relative";

	const occupied = new Set();

	cells.forEach(cellData => {
		const row = cellData.rowNumber - 1;
		const column = cellData.columnNumber - 1;
		const rowSpan = cellData.rowSpan ?? 1;
		const columnSpan = cellData.columnSpan ?? 1;

		for (let r = row; r < row + rowSpan; r++) {
			for (let c = column; c < column + columnSpan; c++) {
				occupied.add(`${r}:${c}`);
			}
		}

		const cell = createCellFromData(cellData, false);

		templateElement.appendChild(cell);
	});

	for (let row = 0; row < rows.length; row++) {
		for (let column = 0; column < columns.length; column++) {
			if (occupied.has(`${row}:${column}`)) {
				continue;
			}

			const cell = document.createElement("div");

			cell.className = "template-cell empty";
			cell.dataset.row = row;
			cell.dataset.column = column;
			cell.dataset.cellType = "EMPTY";
			cell.style.gridRow = `${row + 1}`;
			cell.style.gridColumn = `${column + 1}`;
			cell.textContent = "空白";

			templateElement.appendChild(cell);
		}
	}

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

	renderBackgroundColorSelector(
		templateElement,
		assignments,
		settings
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
 * 背景色の選択欄を生成する。
 */
function renderBackgroundColorSelector(preview, assignments, settings) {
	const wrapper = document.createElement("div");
	wrapper.className = "template-background-color-assignment";

	const label = document.createElement("label");
	label.textContent = "テンプレート背景色";

	const enabled = document.createElement("input");
	enabled.type = "checkbox";
	enabled.checked = !!settings?.backgroundColor;

	const colorInput = document.createElement("input");
	colorInput.type = "color";
	colorInput.value = settings?.backgroundColor || "#ffffff";
	colorInput.disabled = !enabled.checked;

	if (enabled.checked) {
		preview.style.backgroundColor = settings.backgroundColor;
	}

	enabled.addEventListener("change", () => {
		colorInput.disabled = !enabled.checked;

		preview.style.backgroundColor = enabled.checked
			? colorInput.value
			: "";
	});

	colorInput.addEventListener("input", () => {
		if (enabled.checked) {
			preview.style.backgroundColor = colorInput.value;
		}
	});

	label.prepend(enabled);

	wrapper.appendChild(label);
	wrapper.appendChild(colorInput);

	assignments.appendChild(wrapper);

	preview.dataset.backgroundColorEnabled = String(enabled.checked);
	preview.dataset.backgroundColor = colorInput.value;

	enabled.addEventListener("change", () => {
		preview.dataset.backgroundColorEnabled = String(enabled.checked);
	});

	colorInput.addEventListener("input", () => {
		preview.dataset.backgroundColor = colorInput.value;
	});
}

/**
 * VARIABLEセルごとの入力欄を生成する。
 *
 * 同じ変数名のセルは1つの入力欄でまとめて編集する。
 */
function renderVariableInputs(preview, variableInputs, savedCells) {
	const variableElements = preview.querySelectorAll(
		'[data-cell-type="VARIABLE"]'
	);

	if (variableElements.length === 0) {
		return;
	}

	const savedCellMap = new Map(
		savedCells.map(cell => [
			String(cell.cellId),
			cell
		])
	);

	const variableGroups = new Map();

	variableElements.forEach(element => {
		const variableName = element.dataset.variableName;

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
		input.placeholder = `「${variableName}」の値を入力`;

		const savedValue = elements
			.map(element => savedCellMap.get(String(element.dataset.cellId)))
			.find(cell => cell?.variableValue != null)?.variableValue ?? "";

		input.value = savedValue;

		elements.forEach(element => {
			element.dataset.articleVariableValue = savedValue;
			element.textContent = savedValue;
		});

		label.appendChild(input);
		wrapper.appendChild(label);

		input.addEventListener("input", () => {
			elements.forEach(element => {
				element.dataset.articleVariableValue = input.value;
				element.textContent = input.value;
			});
		});

		variableInputs.appendChild(wrapper);
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
