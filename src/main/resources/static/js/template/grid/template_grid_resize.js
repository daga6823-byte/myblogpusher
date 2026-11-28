/**
 * template_grid_resize.js
 *
 * ハンドルのドラッグによる行高・列幅の変更操作を担当する。
 *
 * ドラッグ中は画面上のサイズだけを更新し、ドラッグ終了時にDBへ保存する。
 * 保存に失敗した場合やドラッグがキャンセルされた場合は、開始時のサイズへ戻す。
 * DBへの保存は template_grid_resize_save.js が担当する。
 */
import { templateState } from "../template_state.js";
import { getGridTrackSizes } from "./template_grid_tracks.js";
import { saveGridTrackSizes } from "./template_grid_resize_save.js";

/**
 * サイズ変更を開始する。
 *
 * 内部境界は隣接2トラックを調整し、
 * 外周は対象トラックだけを調整する。
 */
export function startGridResize(event, onSizeChanged) {

	event.preventDefault();
	event.stopPropagation();

	const handle = event.currentTarget;
	const type = handle.dataset.resizeType;
	const index = Number(handle.dataset.resizeIndex);
	const edge = handle.dataset.resizeEdge || null;

	const grid = templateState.grid;
	const sizes = getGridTrackSizes();

	const originalSizes = type === "column"
		? [...sizes.columns]
		: [...sizes.rows];

	const isOuter = edge !== null;

	if (
		!Number.isInteger(index)
		|| index < 0
		|| index >= originalSizes.length
	) {
		return;
	}

	if (isOuter && originalSizes.length < 2) {
		return;
	}

	if (!isOuter && index >= originalSizes.length - 1) {
		return;
	}

	if (
		isOuter
		&& (
			(edge === "start" && index !== 0)
			|| (edge === "end" && index !== originalSizes.length - 1)
		)
	) {
		return;
	}

	const originalFirstSize = originalSizes[index];
	const originalSecondSize = isOuter
		? null
		: originalSizes[index + 1];

	const totalSize = isOuter
		? null
		: originalFirstSize + originalSecondSize;

	const startPosition = type === "column"
		? event.clientX
		: event.clientY;

	const pointerId = event.pointerId;

	handle.setPointerCapture(pointerId);

	let newFirstSize = originalFirstSize;
	let newSecondSize = originalSecondSize;
	let newOuterSize = originalFirstSize;

	/**
	 * ドラッグ位置からサイズを計算する。
	 */
	const calculateSizes = currentPosition => {

		const difference = currentPosition - startPosition;

		if (isOuter) {

			// 左端・上端は逆方向、右端・下端は同方向に変化する。
			const direction = edge === "start" ? -1 : 1;

			newOuterSize = Math.max(
				20,
				originalFirstSize + difference * direction
			);

			return;
		}

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
	 * 現在のサイズをグリッドに反映する。
	 */
	const applySizes = () => {

		const definitions = originalSizes.map(
			(size, trackIndex) => {

				if (isOuter) {
					return trackIndex === index
						? `${newOuterSize}px`
						: `${size}px`;
				}

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
	 * ドラッグ開始時のサイズに戻す。
	 */
	const restoreOriginalSize = () => {

		const definitions = originalSizes.map(
			size => `${size}px`
		);

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

			if (isOuter) {

				// 既存APIに渡す隣接2トラックの位置を決める。
				const saveIndex = edge === "start"
					? 0
					: originalSizes.length - 2;

				const firstSize = edge === "start"
					? newOuterSize
					: originalSizes[saveIndex];

				const secondSize = edge === "start"
					? originalSizes[1]
					: newOuterSize;

				await saveGridTrackSizes(
					type,
					saveIndex,
					firstSize / 5,
					secondSize / 5
				);

			} else {

				await saveGridTrackSizes(
					type,
					index,
					newFirstSize / 5,
					newSecondSize / 5
				);
			}

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
