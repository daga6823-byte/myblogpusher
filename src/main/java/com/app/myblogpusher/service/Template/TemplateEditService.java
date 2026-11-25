
/**
 * テンプレート編集画面の表示データを準備するサービス
 *
 * 新規作成・編集画面に必要なテンプレート情報、
 * レイアウト情報、画像URL、色履歴を組み立てる。
 */
package com.app.myblogpusher.service.Template;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.myblogpusher.entity.Template.TemplateCell;
import com.app.myblogpusher.entity.Template.TemplateColumn;
import com.app.myblogpusher.entity.Template.TemplateEntity;
import com.app.myblogpusher.entity.Template.TemplateRow;
import com.app.myblogpusher.service.Image.ImageAssetQueryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TemplateEditService {

	private final TemplateService templateService;
	private final TemplateLayoutService templateLayoutService;
	private final TemplateColorService templateColorService;
	private final ImageAssetQueryService imageAssetQueryService;
	private final ObjectMapper objectMapper;

	/**
	 * 新規作成画面に渡す初期データを準備する。
	 *
	 * 初期状態として1行・1列・空セルを設定する。
	 *
	 * @param userId ログインユーザーID
	 * @return 画面表示用データ
	 */
	public Map<String, Object> prepareNewTemplate(Long userId)
			throws JsonProcessingException {

		TemplateEntity template = new TemplateEntity();

		TemplateRow row = new TemplateRow();
		row.setRowNumber(1);
		row.setHeightUnit("auto");

		TemplateColumn column = new TemplateColumn();
		column.setColumnNumber(1);
		column.setWidthUnit("fr");

		TemplateCell cell = new TemplateCell();
		cell.setRowNumber(1);
		cell.setColumnNumber(1);
		cell.setRowSpan(1);
		cell.setColumnSpan(1);
		cell.setCellType("EMPTY");

		List<TemplateRow> rows = List.of(row);
		List<TemplateColumn> columns = List.of(column);
		List<TemplateCell> cells = List.of(cell);

		return createEditModel(
				userId,
				template,
				rows,
				columns,
				cells);
	}

	/**
	 * 編集画面に渡す保存済みデータを準備する。
	 *
	 * テンプレートの所有権を確認し、
	 * 保存済みの行・列・セル情報を取得する。
	 *
	 * @param userId ログインユーザーID
	 * @param templateId テンプレートID
	 * @return 画面表示用データ
	 */
	public Map<String, Object> prepareEditTemplate(
			Long userId,
			Long templateId) throws JsonProcessingException {

		TemplateEntity template = templateService.getTemplate(
				userId,
				templateId);

		List<TemplateRow> rows = templateLayoutService.getRows(templateId);

		List<TemplateColumn> columns = templateLayoutService.getColumns(templateId);

		List<TemplateCell> cells = templateLayoutService.getCells(templateId);

		return createEditModel(
				userId,
				template,
				rows,
				columns,
				cells);
	}

	/**
	 * 新規作成・編集画面で共通して使用する表示データを組み立てる。
	 *
	 * セル情報のJSON化とIMAGEセルの画像URL設定を行う。
	 */
	private Map<String, Object> createEditModel(
			Long userId,
			TemplateEntity template,
			List<TemplateRow> rows,
			List<TemplateColumn> columns,
			List<TemplateCell> cells) throws JsonProcessingException {

		Map<String, Object> model = new LinkedHashMap<>();

		model.put("template", template);
		model.put("rows", rows);
		model.put("columns", columns);
		model.put("cells", cells);

		// JavaScriptでレイアウト情報を扱えるようJSON化する。
		model.put("rowsJson", objectMapper.writeValueAsString(rows));
		model.put("columnsJson", objectMapper.writeValueAsString(columns));

		// セル情報をJSON配列に変換する。
		ArrayNode cellsJson = objectMapper.valueToTree(cells);

		// IMAGEセルには所有者確認済みの画像URLを設定する。
		for (JsonNode node : cellsJson) {

			if (!(node instanceof ObjectNode cellJson)) {
				continue;
			}

			if (!"IMAGE".equals(cellJson.path("cellType").asText())) {
				continue;
			}

			if (cellJson.path("imageAssetId").isNull()
					|| cellJson.path("imageAssetId").isMissingNode()) {
				continue;
			}

			Long imageAssetId = cellJson.path("imageAssetId").asLong();

			try {
				String imageUrl = imageAssetQueryService.getImageUrl(
						userId,
						imageAssetId);

				cellJson.put("imageUrl", imageUrl);

			} catch (IllegalArgumentException e) {
				// 画像が削除済み、または所有者が異なる場合はURLを設定しない。
				cellJson.putNull("imageUrl");
			}
		}

		model.put(
				"cellsJson",
				objectMapper.writeValueAsString(cellsJson));

		model.put(
				"colors",
				templateColorService.getColors(userId));

		return model;
	}
}
