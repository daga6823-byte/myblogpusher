
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

import com.app.myblogpusher.entity.Template.TemplateCellEntity;
import com.app.myblogpusher.entity.Template.TemplateColumnEntity;
import com.app.myblogpusher.entity.Template.TemplateEntity;
import com.app.myblogpusher.entity.Template.TemplateRowEntity;
import com.app.myblogpusher.facade.TemplateLayoutFacadeService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TemplateRenderService {

	private final TemplateService templateService;
	private final TemplateLayoutFacadeService templateLayoutFacadeService;

	/**
	 * テンプレートのHTMLとCSSを生成する。
	 *
	 * @param userId ログインユーザーID
	 * @param templateId テンプレートID
	 * @return HTMLとCSS
	 */
	public Map<String, String> generate(Long userId, Long templateId) {

		TemplateEntity template = templateService.getTemplate(userId, templateId);

		List<TemplateRowEntity> rows = templateLayoutFacadeService.getRows(templateId);
		List<TemplateColumnEntity> columns = templateLayoutFacadeService.getColumns(templateId);
		List<TemplateCellEntity> cells = templateLayoutFacadeService.getCells(templateId);

		String cssClass = "mbp-template-" + templateId;
		String cssFileName = generateCssFileName(template);

		String html = generateHtml(cells, cssClass);
		String css = generateCss(template, rows, columns, cells, cssClass);

		Map<String, String> result = new HashMap<>();
		result.put("html", html);
		result.put("css", css);
		result.put("cssFileName", cssFileName);

		return result;
	}

	/**
	 * テンプレート識別キーとIDからCSSファイル名を生成する。
	 *
	 * 同一識別キーを別ユーザーが使用しても、
	 * テンプレートIDによってファイル名が重複しない。
	 */
	private String generateCssFileName(TemplateEntity template) {

		String safeKey = template.getTemplateKey()
				.toLowerCase()
				.replaceAll("[^a-z0-9_-]", "-")
				.replaceAll("-+", "-")
				.replaceAll("^-|-$", "");

		if (safeKey.isBlank()) {
			safeKey = "template";
		}

		return safeKey + "-" + template.getTemplateId() + ".css";
	}

	/**
	 * テンプレート全体とセルのHTMLを生成する。
	 *
	 * IMAGEセルは画像を固定せず、記事編集画面で画像を割り当てる
	 * プレースホルダーとして出力する。
	 */
	private String generateHtml(
			List<TemplateCellEntity> cells,
			String cssClass) {

		StringBuilder html = new StringBuilder();

		html.append("<div class=\"mbp-template ")
				.append(cssClass)
				.append("\">");

		for (TemplateCellEntity cell : cells) {

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

				// 記事編集画面で画像選択ボタンを表示するための枠。
				// 画像自体は記事ごとに設定する。
				html.append("<div class=\"mbp-template-image-placeholder\"")
						.append(" data-template-image-cell=\"")
						.append(cell.getCellId())
						.append("\"></div>");
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
			List<TemplateRowEntity> rows,
			List<TemplateColumnEntity> columns,
			List<TemplateCellEntity> cells,
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
		css.append("gap: 2px;");
		css.append("box-sizing: border-box;");
		appendCssProperty(css, "background-color", template.getBackgroundColor());
		css.append("}\n");

		css.append(".").append(cssClass)
				.append(" .mbp-template-cell {");
		css.append("box-sizing: border-box;");
		css.append("min-width: 0;");
		css.append("min-height: 0;");
		css.append("overflow-wrap: anywhere;");
		css.append("border: 1px solid #777;");
		css.append("background-color: transparent;");
		css.append("font-size: 10px;");
		css.append("font-weight: normal;");
		css.append("}\n");

		// IMAGEセルの画像配置枠
		css.append(".").append(cssClass)
				.append(" .mbp-template-image-placeholder {");
		css.append("width: 100%;");
		css.append("min-height: 80px;");
		css.append("box-sizing: border-box;");
		css.append("display: flex;");
		css.append("align-items: center;");
		css.append("justify-content: center;");
		css.append("border: 1px dashed #999;");
		css.append("}\n");

		// 記事に割り当てられた画像
		css.append(".").append(cssClass)
				.append(" .mbp-template-image-placeholder img {");
		css.append("max-width: 100%;");
		css.append("height: auto;");
		css.append("object-fit: contain;");
		css.append("}\n");

		for (TemplateCellEntity cell : cells) {
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

			// 文字の縁取り
			appendCssProperty(css, "-webkit-text-stroke-color",
					cell.getTextStrokeColor());

			if (cell.getTextStrokeWidth() != null
					&& cell.getTextStrokeWidth().compareTo(BigDecimal.ZERO) > 0
					&& cell.getTextStrokeWidth().compareTo(new BigDecimal("3.0")) <= 0) {

				css.append("-webkit-text-stroke-width:")
						.append(cell.getTextStrokeWidth().toPlainString())
						.append("px;");

				css.append("paint-order:stroke fill;");
			}

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
	private String generateColumnSizes(List<TemplateColumnEntity> columns) {

		if (columns.isEmpty()) {
			return "1fr";
		}

		StringBuilder sizes = new StringBuilder();

		for (TemplateColumnEntity column : columns) {
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
	private String generateRowSizes(List<TemplateRowEntity> rows) {

		if (rows.isEmpty()) {
			return "auto";
		}

		StringBuilder sizes = new StringBuilder();

		for (TemplateRowEntity row : rows) {
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
