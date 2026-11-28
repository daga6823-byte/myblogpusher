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

// 背景画像のプレビュー表示倍率。
// DBには保存せず、編集画面の表示にだけ適用する。
const PREVIEW_SCALE = 5;

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
 * 背景画像の移動操作を登録する。
 *
 * 通常時はセル操作を優先し、移動モード中だけ背景画像を動かす。
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

		if (grid.dataset.backgroundMoveMode !== "true") {
			return;
		}

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

		if (moved) {
			suppressNextClick = true;
		}
	};

	grid.addEventListener("pointerup", stopDragging);
	grid.addEventListener("pointercancel", stopDragging);

	grid.addEventListener("lostpointercapture", () => {
		dragging = false;
		pointerId = null;
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

	// キーボードによる背景画像移動。
	document.addEventListener("keydown", event => {

		if (grid.dataset.backgroundMoveMode !== "true") {
			return;
		}

		if (!grid.dataset.previewBackgroundImageUrl) {
			return;
		}

		const directions = {
			ArrowLeft: [-1, 0],
			ArrowRight: [1, 0],
			ArrowUp: [0, -1],
			ArrowDown: [0, 1]
		};

		const direction = directions[event.key];

		if (!direction) {
			return;
		}

		event.preventDefault();

		const step = event.shiftKey ? 10 : 1;

		const position = getComputedStyle(grid)
			.backgroundPosition.split(" ");

		const currentX = parseFloat(position[0]) || 0;
		const currentY = parseFloat(position[1]) || 0;

		grid.style.backgroundPosition =
			`${currentX + direction[0] * step}px ` +
			`${currentY + direction[1] * step}px`;
	});
}

/**
 * 背景画像の移動モードを切り替える。
 */
function toggleBackgroundMoveMode() {

	const grid = templateState.grid;
	const button = document.getElementById("moveBackgroundImageButton");

	if (!grid || !grid.dataset.previewBackgroundImageUrl) {
		alert("先に背景画像を選択してください。");
		return;
	}

	const enabled = grid.dataset.backgroundMoveMode !== "true";

	grid.dataset.backgroundMoveMode = String(enabled);

	grid.style.cursor = enabled ? "grab" : "";

	if (button) {
		button.textContent = enabled
			? "背景画像の移動を終了"
			: "背景画像を移動";

		button.setAttribute("aria-pressed", String(enabled));
	}
}

/**
 * 背景画像の位置を初期位置に戻す。
 */
function resetBackgroundImagePosition() {

	const grid = templateState.grid;

	if (!grid || !grid.dataset.previewBackgroundImageUrl) {
		alert("背景画像が選択されていません。");
		return;
	}

	grid.style.backgroundPosition = "0px 0px";
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
			grid.style.backgroundSize =
				`${background.naturalWidth * PREVIEW_SCALE}px ${background.naturalHeight * PREVIEW_SCALE}px`;
			grid.style.backgroundPosition = "0px 0px";
			grid.style.cursor = "";
			grid.dataset.backgroundMoveMode = "false";
			const moveButton = document.getElementById("moveBackgroundImageButton");

			if (moveButton) {
				moveButton.textContent = "背景画像を移動";
				moveButton.setAttribute("aria-pressed", "false");
			}
			
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

	const scaledImageHeight =
		image.naturalHeight * PREVIEW_SCALE;

	const requiredHeight =
		scaledImageHeight + verticalPadding + verticalBorder;

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

	grid.dataset.backgroundMoveMode = "false";

	const moveButton = document.getElementById("moveBackgroundImageButton");

	if (moveButton) {
		moveButton.textContent = "背景画像を移動";
		moveButton.setAttribute("aria-pressed", "false");
	}
}

document.addEventListener("DOMContentLoaded", () => {
	document.getElementById("addImageCellButton")
		?.addEventListener("click", setImageCell);

	document.getElementById("selectBackgroundImageButton")
		?.addEventListener("click", selectBackgroundImage);

	document.getElementById("clearBackgroundImageButton")
		?.addEventListener("click", clearBackgroundImage);

	document.getElementById("moveBackgroundImageButton")
		?.addEventListener("click", toggleBackgroundMoveMode);

	document.getElementById("resetBackgroundImagePositionButton")
		?.addEventListener("click", resetBackgroundImagePosition);

	enableBackgroundDrag(templateState.grid);
});
