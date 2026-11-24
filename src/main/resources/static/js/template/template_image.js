/**
 * template_image.js
 *
 * 既存の画像管理機能と連携し、
 * テンプレートのIMAGEセルを設定する。
 */

import { changeCellType } from "./template_cell.js";
import { openImageSelector } from "../image/image_gallery.js";

/**
 * 画像セルを設定する。
 */
function setImageCell() {
	const imageWidthInput = document.getElementById("imageWidth");

	openImageSelector(image => {
		const widthValue = imageWidthInput?.value.trim() ?? "";
		const imageWidth = widthValue === "" ? null : Number(widthValue);

		if (imageWidth !== null &&
			(!Number.isFinite(imageWidth) || imageWidth <= 0)) {
			alert("画像幅は0より大きい数値で指定してください。");
			return;
		}

		if (!image.imageId) {
			alert("画像IDを取得できませんでした。");
			return;
		}

		changeCellType("IMAGE", {
			imageAssetId: image.imageId,
			imageUrl: image.url,
			imageWidth: imageWidth,
			imageHeight: null
		});
	});
}

document.addEventListener("DOMContentLoaded", () => {
	document.getElementById("addImageCellButton")
		?.addEventListener("click", setImageCell);
});