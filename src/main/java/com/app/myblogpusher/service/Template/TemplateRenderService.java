
/**
 * テンプレートのHTML・CSS生成を担当するサービス
 *
 * DBに保存されたテンプレート、行、列、セルの情報から
 * テンプレート専用のHTMLとCSSを生成する。
 *
 * 画像URLは既存のImageAssetQueryServiceから取得する。
 */
package com.app.myblogpusher.service.Template;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.myblogpusher.entity.Template.TemplateCell;
import com.app.myblogpusher.entity.Template.TemplateColumn;
import com.app.myblogpusher.entity.Template.TemplateEntity;
import com.app.myblogpusher.entity.Template.TemplateRow;
import com.app.myblogpusher.service.Image.ImageAssetQueryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TemplateRenderService {

	private final TemplateService templateService;
	private final TemplateLayoutService templateLayoutService;
	private final ImageAssetQueryService imageAssetQueryService;

	/**
	 * テンプレートのHTMLとCSSを生成する。
	 *
	 * @param userId ログインユーザーID
	 * @param templateId テンプレートID
	 * @return HTMLとCSS
	 */
	public Map<String, String> generate(Long userId, Long templateId) {

		TemplateEntity template = templateService.getTemplate(userId, templateId);

		List<TemplateRow> rows = templateLayoutService.getRows(templateId);
		List<TemplateColumn> columns = templateLayoutService.getColumns(templateId);
		List<TemplateCell> cells = templateLayoutService.getCells(templateId);

		Map<Long, String> imageUrlCache = new HashMap<>();

		String cssClass = "mbp-template-" + templateId;

		String html = generateHtml(userId, cells, imageUrlCache, cssClass);
		String css = generateCss(template, rows, columns, cells, cssClass);

		Map<String, String> result = new HashMap<>();
		result.put("html", html);
		result.put("css", css);

		return result;
	}

	/**
	 * テンプレート全体とセルのHTMLを生成する。
	 */
	private String generateHtml(
			Long userId,
			List<TemplateCell> cells,
			Map<Long, String> imageUrlCache,
			String cssClass) {

		StringBuilder html = new StringBuilder();

		// Hugoに配置したテンプレート専用CSSを読み込む。
		html.append("<link rel=\"stylesheet\" href=\"/css/templates/template_")
		        .append(cssClass.substring("mbp-template-".length()))
		        .append(".css\">");

		html.append("<div class=\"mbp-template ")
		        .append(cssClass)
		        .append("\">");

		for (TemplateCell cell : cells) {
			html.append("<div class=\"mbp-template-cell mbp-cell-")
					.append(cell.getCellId())
					.append("\">");

			String cellType = cell.getCellType();

			if ("TEXT".equals(cellType)) {
				html.append(escapeHtml(cell.getCellContent()));

			} else if ("VARIABLE".equals(cellType)) {
				html.append("<span data-template-variable=\"")
						.append(escapeHtml(cell.getVariableName()))
						.append("\">")
						.append(escapeHtml(cell.getCellContent()))
						.append("</span>");

			} else if ("IMAGE".equals(cellType)) {
				Long imageAssetId = cell.getImageAssetId();

				if (imageAssetId != null) {
					String imageUrl = imageUrlCache.computeIfAbsent(
							imageAssetId,
							id -> imageAssetQueryService.getImageUrl(userId, id));

					if (imageUrl != null && !imageUrl.isBlank()) {
						html.append("<img src=\"")
								.append(escapeHtml(imageUrl))
								.append("\" alt=\"\" loading=\"lazy\">");
					}
				}
			}

			html.append("</div>");
		}

		html.append("</div>");

		return html.toString();
	}

	/**
	 * テンプレート専用CSSを生成する。
	 */
	private String generateCss(
			TemplateEntity template,
			List<TemplateRow> rows,
			List<TemplateColumn> columns,
			List<TemplateCell> cells,
			String cssClass) {

		StringBuilder css = new StringBuilder();

		css.append(".").append(cssClass).append(" {");
		css.append("display: grid;");
		css.append("grid-template-columns: ")
				.append(generateColumnSizes(columns))
				.append(";");
		css.append("grid-template-rows: ")
				.append(generateRowSizes(rows))
				.append(";");
		css.append("box-sizing: border-box;");
		appendCssProperty(css, "background-color", template.getBackgroundColor());
		css.append("}\n");

		css.append(".").append(cssClass)
				.append(" .mbp-template-cell {");
		css.append("box-sizing: border-box;");
		css.append("min-width: 0;");
		css.append("min-height: 0;");
		css.append("overflow-wrap: anywhere;");
		css.append("}\n");

		for (TemplateCell cell : cells) {
			css.append(".").append(cssClass)
					.append(" .mbp-cell-")
					.append(cell.getCellId())
					.append(" {");

			css.append("grid-column: ")
					.append(cell.getColumnNumber())
					.append(" / span ")
					.append(cell.getColumnSpan())
					.append(";");

			css.append("grid-row: ")
					.append(cell.getRowNumber())
					.append(" / span ")
					.append(cell.getRowSpan())
					.append(";");

			appendCssProperty(css, "background-color", cell.getBackgroundColor());
			appendCssProperty(css, "color", cell.getTextColor());
			appendCssProperty(css, "font-weight", cell.getFontWeight());
			appendCssProperty(css, "text-align", cell.getTextAlign());

			if (cell.getFontSize() != null) {
				css.append("font-size:")
						.append(cell.getFontSize().toPlainString())
						.append("px;");
			}

			if (cell.getPadding() != null) {
				css.append("padding:")
						.append(cell.getPadding().toPlainString())
						.append(validUnit(cell.getPaddingUnit(), "px"))
						.append(";");
			}

			if ("MIDDLE".equalsIgnoreCase(cell.getVerticalAlign())
					|| "CENTER".equalsIgnoreCase(cell.getVerticalAlign())) {
				css.append("display:flex;align-items:center;");
			} else if ("BOTTOM".equalsIgnoreCase(cell.getVerticalAlign())) {
				css.append("display:flex;align-items:flex-end;");
			} else if ("TOP".equalsIgnoreCase(cell.getVerticalAlign())) {
				css.append("display:flex;align-items:flex-start;");
			}

			css.append("}\n");

			if (cell.getImageWidth() != null || cell.getImageHeight() != null) {
				css.append(".").append(cssClass)
						.append(" .mbp-cell-")
						.append(cell.getCellId())
						.append(" img {");

				css.append("max-width:100%;object-fit:contain;");

				appendPixelSize(css, "width", cell.getImageWidth());
				appendPixelSize(css, "height", cell.getImageHeight());

				css.append("}\n");
			}
		}

		return css.toString();
	}

	/**
	 * 列幅をCSS Grid用の値に変換する。
	 */
	private String generateColumnSizes(List<TemplateColumn> columns) {

		if (columns.isEmpty()) {
			return "1fr";
		}

		StringBuilder sizes = new StringBuilder();

		for (TemplateColumn column : columns) {
			if (sizes.length() > 0) {
				sizes.append(" ");
			}

			sizes.append(toGridSize(column.getWidth(), column.getWidthUnit()));
		}

		return sizes.toString();
	}

	/**
	 * 行の高さをCSS Grid用の値に変換する。
	 */
	private String generateRowSizes(List<TemplateRow> rows) {

		if (rows.isEmpty()) {
			return "auto";
		}

		StringBuilder sizes = new StringBuilder();

		for (TemplateRow row : rows) {
			if (sizes.length() > 0) {
				sizes.append(" ");
			}

			sizes.append(toGridSize(row.getHeight(), row.getHeightUnit()));
		}

		return sizes.toString();
	}

	/**
	 * 数値と単位からGridサイズを生成する。
	 */
	private String toGridSize(BigDecimal value, String unit) {

		if (value == null) {
			return "auto";
		}

		return value.toPlainString() + validUnit(unit, "px");
	}

	/**
	 * CSSの数値サイズを追加する。
	 */
	private void appendPixelSize(
			StringBuilder css,
			String property,
			BigDecimal value) {

		if (value != null) {
			css.append(property)
					.append(":")
					.append(value.toPlainString())
					.append("px;");
		}
	}

	/**
	 * CSSプロパティを追加する。
	 */
	private void appendCssProperty(
			StringBuilder css,
			String property,
			String value) {

		if (value == null || value.isBlank()) {
			return;
		}

		String safeValue = value.trim();

		if (safeValue.matches("[a-zA-Z0-9#(),.%\\s-]+")) {
			css.append(property)
					.append(":")
					.append(safeValue)
					.append(";");
		}
	}

	/**
	 * CSSの単位を検証する。
	 */
	private String validUnit(String unit, String defaultUnit) {

		if (unit == null || unit.isBlank()) {
			return defaultUnit;
		}

		return switch (unit.toLowerCase()) {
		case "px", "%", "rem", "em", "vw", "vh", "fr" -> unit;
		default -> defaultUnit;
		};
	}

	/**
	 * HTML出力用に文字列をエスケープする。
	 */
	private String escapeHtml(String value) {

		if (value == null) {
			return "";
		}

		return value
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&#39;");
	}
}
