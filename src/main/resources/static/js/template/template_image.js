/**
 * template_image.js
 *
 * テンプレートのIMAGEセル設定と背景画像プレビューを担当する。
 *
 * IMAGEセルは画像配置用の枠だけを設定する。
 * 背景画像は編集画面のプレビュー専用であり、
 * テンプレートマスターには保存しない。
 */

import { changeCellType } from "../facade/template_cell_facade.js";
import { openImageSelector } from "../image/image_gallery.js";
import { templateState } from "./template_state.js";

/**
 * 画像配置用のセルを設定する。
 */
function setImageCell() {

	const imageWidthInput = document.getElementById("imageWidth");

	const widthValue = imageWidthInput?.value.trim() ?? "";

	const imageWidth = widthValue === "" ? null : Number(widthValue);

	if (imageWidth !== null &&
		(!Number.isFinite(imageWidth) || imageWidth <= 0)) {

		alert("画像幅は0より大きい数値で指定してください。");
		return;
	}

	changeCellType("IMAGE", {
		imageWidth: imageWidth,
		imageHeight: null
	});
}

/**
 * 背景画像を選択してプレビューに反映する。
 */
function selectBackgroundImage() {

	openImageSelector(image => {

		const grid = templateState.grid;

		if (!grid) {
			alert("テンプレートのグリッドが見つかりません。");
			return;
		}

		grid.style.backgroundImage = `url("${image.url}")`;

		applyBackgroundImageSettings();

		// プレビュー専用情報。テンプレートマスターには保存しない。
		grid.dataset.previewBackgroundImageId = image.imageId;
		grid.dataset.previewBackgroundImageUrl = image.url;
	});
}

/**
 * 背景画像の表示設定をグリッドに反映する。
 */
function applyBackgroundImageSettings() {

	const grid = templateState.grid;

	if (!grid || !grid.style.backgroundImage ||
		grid.style.backgroundImage === "none") {
		return;
	}

	const size = document.getElementById("backgroundImageSize")?.value;
	const position = document.getElementById("backgroundImagePosition")?.value;
	const repeat = document.getElementById("backgroundImageRepeat")?.value;

	if (size) {
		grid.style.backgroundSize = size;
	}

	if (position) {
		grid.style.backgroundPosition = position;
	}

	if (repeat) {
		grid.style.backgroundRepeat = repeat;
	}
}

/**
 * 背景画像のプレビューを解除する。
 */
function clearBackgroundImage() {

	const grid = templateState.grid;

	if (!grid) {
		return;
	}

	grid.style.backgroundImage = "none";

	delete grid.dataset.previewBackgroundImageId;
	delete grid.dataset.previewBackgroundImageUrl;
}

document.addEventListener("DOMContentLoaded", () => {

	document.getElementById("addImageCellButton")
		?.addEventListener("click", setImageCell);

	document.getElementById("selectBackgroundImageButton")
		?.addEventListener("click", selectBackgroundImage);

	document.getElementById("clearBackgroundImageButton")
		?.addEventListener("click", clearBackgroundImage);

	document.getElementById("backgroundImageSize")
		?.addEventListener("change", applyBackgroundImageSettings);

	document.getElementById("backgroundImagePosition")
		?.addEventListener("change", applyBackgroundImageSettings);

	document.getElementById("backgroundImageRepeat")
		?.addEventListener("change", applyBackgroundImageSettings);
});