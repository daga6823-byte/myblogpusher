
/**
 * template_image.js
 *
 * テンプレートのIMAGEセルを設定する。
 *
 * 画像そのものは保持せず、記事で画像を差し込むための
 * 配置枠と表示サイズを保存する。
 */

import { changeCellType } from "./template_cell.js";

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

document.addEventListener("DOMContentLoaded", () => {
	document.getElementById("addImageCellButton")
		?.addEventListener("click", setImageCell);
});
