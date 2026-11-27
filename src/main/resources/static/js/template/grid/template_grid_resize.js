/**
 * template_grid_resize.js
 *
 * ハンドルのドラッグによる行高・列幅の変更操作を担当する。
 *
 * ドラッグ中は画面上のサイズだけを更新し、ドラッグ終了時にDBへ保存する。
 * 保存に失敗した場合やドラッグがキャンセルされた場合は、開始時のサイズへ戻す。
 * DBへの保存は template_grid_resize_save.js が担当する。
 */
import { templateState } from "./template_state.js";
import { getGridTrackSizes } from "./template_grid_tracks.js";
import { saveGridTrackSizes } from "./template_grid_resize_save.js";

/**
 * サイズ変更を開始する。
 *
 * onSizeChanged は、サイズを画面に反映するたびに呼ぶ処理
 * (ハンドル位置の更新)を受け取る。
 */
export function startGridResize(event, onSizeChanged) {

	event.preventDefault();
	event.stopPropagation();

	const handle = event.currentTarget;
	const type = handle.dataset.resizeType;
	const index = Number(handle.dataset.resizeIndex);
	const grid = templateState.grid;

	const sizes = getGridTrackSizes();

	const originalSizes = type === "column"
		? [...sizes.columns]
		: [...sizes.rows];

	if (
		!Number.isInteger(index) ||
		index < 0 ||
		index >= originalSizes.length - 1
	) {
		return;
	}

	const originalFirstSize = originalSizes[index];
	const originalSecondSize = originalSizes[index + 1];
	const totalSize = originalFirstSize + originalSecondSize;

	const startPosition = type === "column"
		? event.clientX
		: event.clientY;

	const pointerId = event.pointerId;

	handle.setPointerCapture(pointerId);

	let newFirstSize = originalFirstSize;
	let newSecondSize = originalSecondSize;

	/**
	 * 現在のドラッグ位置から隣接する2トラックのサイズを計算する。
	 */
	const calculateSizes = currentPosition => {

		const difference = currentPosition - startPosition;

		newFirstSize = Math.max(
			20,
			Math.min(
				totalSize - 20,
				originalFirstSize + difference
			)
		);

		newSecondSize = totalSize - newFirstSize;

	};

	/**
	 * ドラッグ中のサイズを画面に反映する。
	 *
	 * ドラッグ開始時の実サイズを基準にし、
	 * 操作対象の隣接2トラックだけを変更する。
	 */
	const applySizes = () => {

		const definitions = originalSizes.map(
			(size, trackIndex) => {

				if (trackIndex === index) {
					return `${newFirstSize}px`;
				}

				if (trackIndex === index + 1) {
					return `${newSecondSize}px`;
				}

				return `${size}px`;

			}
		);

		if (type === "column") {

			grid.style.gridTemplateColumns = definitions.join(" ");

		} else {

			grid.style.gridTemplateRows = definitions.join(" ");

		}

		onSizeChanged();

	};

	/**
	 * ドラッグ開始時の実サイズに戻す。
	 */
	const restoreOriginalSize = () => {

		const definitions = originalSizes.map(size => `${size}px`);

		if (type === "column") {

			grid.style.gridTemplateColumns = definitions.join(" ");

		} else {

			grid.style.gridTemplateRows = definitions.join(" ");

		}

		onSizeChanged();

	};

	const removePointerEvents = () => {

		handle.removeEventListener("pointermove", onPointerMove);
		handle.removeEventListener("pointerup", onPointerUp);
		handle.removeEventListener("pointercancel", onPointerCancel);

	};

	const onPointerMove = moveEvent => {

		if (moveEvent.pointerId !== pointerId) {
			return;
		}

		const currentPosition = type === "column"
			? moveEvent.clientX
			: moveEvent.clientY;

		calculateSizes(currentPosition);
		applySizes();

	};

	const onPointerUp = async upEvent => {

		if (upEvent.pointerId !== pointerId) {
			return;
		}

		removePointerEvents();

		const currentPosition = type === "column"
			? upEvent.clientX
			: upEvent.clientY;

		calculateSizes(currentPosition);
		applySizes();

		try {

			await saveGridTrackSizes(
				type,
				index,
				newFirstSize,
				newSecondSize
			);

		} catch (error) {

			restoreOriginalSize();
			alert(error.message);

		}

	};

	const onPointerCancel = cancelEvent => {

		if (cancelEvent.pointerId !== pointerId) {
			return;
		}

		removePointerEvents();
		restoreOriginalSize();

	};

	handle.addEventListener("pointermove", onPointerMove);
	handle.addEventListener("pointerup", onPointerUp);
	handle.addEventListener("pointercancel", onPointerCancel);

}