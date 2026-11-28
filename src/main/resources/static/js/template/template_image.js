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
 * 背景画像をドラッグで移動する。
 *
 * セル上からもドラッグできる。
 * リサイズハンドル上ではドラッグしない。
 */
function enableBackgroundDrag(grid = templateState.grid) {
	if (!grid || grid.dataset.backgroundDragEnabled === "true") {
		return;
	}

	grid.dataset.backgroundDragEnabled = "true";

	let dragging = false;
	let moved = false;
	let startX = 0;
	let startY = 0;
	let backgroundX = 0;
	let backgroundY = 0;
	let pointerId = null;
	let suppressNextClick = false;

	grid.addEventListener("pointerdown", event => {
		if (!grid.dataset.previewBackgroundImageUrl) {
			return;
		}

		if (event.button !== 0) {
			return;
		}

		if (event.target.closest(".template-resize-handle")) {
			return;
		}

		const position = getComputedStyle(grid)
			.backgroundPosition.split(" ");

		backgroundX = parseFloat(position[0]) || 0;
		backgroundY = parseFloat(position[1]) || 0;

		startX = event.clientX;
		startY = event.clientY;
		pointerId = event.pointerId;

		dragging = true;
		moved = false;

		grid.setPointerCapture(pointerId);
	});

	grid.addEventListener("pointermove", event => {
		if (!dragging || event.pointerId !== pointerId) {
			return;
		}

		const moveX = event.clientX - startX;
		const moveY = event.clientY - startY;

		if (Math.abs(moveX) > 3 || Math.abs(moveY) > 3) {
			moved = true;
		}

		if (!moved) {
			return;
		}

		event.preventDefault();

		grid.style.backgroundPosition =
			`${backgroundX + moveX}px ${backgroundY + moveY}px`;
	});

	const stopDragging = event => {
		if (!dragging || event.pointerId !== pointerId) {
			return;
		}

		dragging = false;
		pointerId = null;

		grid.style.cursor = "grab";

		if (moved) {
			suppressNextClick = true;
		}
	};

	grid.addEventListener("pointerup", stopDragging);
	grid.addEventListener("pointercancel", stopDragging);

	grid.addEventListener("lostpointercapture", () => {
		dragging = false;
		pointerId = null;
		grid.style.cursor = "grab";
	});

	// 背景画像を動かした場合だけセル選択クリックを抑制する。
	grid.addEventListener("click", event => {
		if (!suppressNextClick) {
			return;
		}

		suppressNextClick = false;

		event.preventDefault();
		event.stopPropagation();
		event.stopImmediatePropagation();
	}, true);
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
			grid.style.backgroundSize = "auto";
			grid.style.backgroundPosition = "0px 0px";
			grid.style.cursor = "grab";
			grid.style.backgroundRepeat = "no-repeat";

			enableBackgroundDrag(grid);

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
 * 背景画像の原寸に合わせてグリッドの高さを調整する。
 *
 * 背景画像は原寸（1px : 1px）で表示する。
 *
 * @param {HTMLImageElement} image
 */
function adjustGridHeight(image) {
	const grid = templateState.grid;

	if (!grid || !image.naturalWidth || !image.naturalHeight) {
		return;
	}

	const style = getComputedStyle(grid);

	const verticalPadding =
		parseFloat(style.paddingTop)
		+ parseFloat(style.paddingBottom);

	const verticalBorder =
		parseFloat(style.borderTopWidth)
		+ parseFloat(style.borderBottomWidth);

	const requiredHeight =
		image.naturalHeight + verticalPadding + verticalBorder;

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
	grid.style.backgroundPosition = "0px 0px";
	grid.style.cursor = "";

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

	enableBackgroundDrag(templateState.grid);
});
