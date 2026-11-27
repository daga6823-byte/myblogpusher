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

		const background = new Image();

		background.onload = () => {

			grid.style.backgroundImage = `url("${image.url}")`;
			grid.style.backgroundSize = "100% auto";
			grid.style.backgroundPosition = "top center";
			grid.style.backgroundRepeat = "no-repeat";

			if (grid.dataset.originalMinHeight === undefined) {

				grid.dataset.originalMinHeight = grid.style.minHeight;

			}

			// 画像のアスペクト比に合わせてグリッドの高さを調整する。
			adjustGridHeight(background);

			// プレビュー専用情報。テンプレートマスターには保存しない。
			grid.dataset.previewBackgroundImageId = image.imageId;
			grid.dataset.previewBackgroundImageUrl = image.url;

		};

		background.onerror = () => {

			alert("背景画像を読み込めませんでした。");

		};

		background.src = image.url;

	});

}

/**
 * 背景画像のアスペクト比に合わせてグリッドの高さを調整する。
 *
 * 背景画像は幅100%で表示するため、
 * 画像の元サイズから表示高さを計算する。
 *
 * @param {HTMLImageElement} image
 */
function adjustGridHeight(image) {

	const grid = templateState.grid;

	if (!grid || !image.naturalWidth || !image.naturalHeight) {

		return;

	}

	const style = getComputedStyle(grid);

	const horizontalPadding =
		parseFloat(style.paddingLeft)
		+ parseFloat(style.paddingRight);

	const horizontalBorder =
		parseFloat(style.borderLeftWidth)
		+ parseFloat(style.borderRightWidth);

	const contentWidth =
		grid.clientWidth
		- horizontalPadding
		- horizontalBorder;

	if (contentWidth <= 0) {

		return;

	}

	const imageHeight =
		contentWidth * image.naturalHeight / image.naturalWidth;

	const verticalPadding =
		parseFloat(style.paddingTop)
		+ parseFloat(style.paddingBottom);

	const verticalBorder =
		parseFloat(style.borderTopWidth)
		+ parseFloat(style.borderBottomWidth);

	const requiredHeight =
		imageHeight + verticalPadding + verticalBorder;

	grid.style.minHeight = `${requiredHeight}px`;

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

	if (grid.dataset.originalMinHeight !== undefined) {

		grid.style.minHeight = grid.dataset.originalMinHeight;

		delete grid.dataset.originalMinHeight;

	}

}

document.addEventListener("DOMContentLoaded", () => {

	document.getElementById("addImageCellButton")
		?.addEventListener("click", setImageCell);

	document.getElementById("selectBackgroundImageButton")
		?.addEventListener("click", selectBackgroundImage);

	document.getElementById("clearBackgroundImageButton")
		?.addEventListener("click", clearBackgroundImage);

	window.addEventListener("resize", () => {

		const grid = templateState.grid;

		const imageUrl = grid?.dataset.previewBackgroundImageUrl;

		if (!imageUrl) {
			return;
		}

		const background = new Image();

		background.onload = () => {

			adjustGridHeight(background);

		};
		background.src = imageUrl;
	});
});