/**
 * Markdown構造を判定するユーティリティ
 *
 * 文章整形処理を行う前に、
 * 「これは通常文章として扱ってよいか」
 * を判定する。
 */

package com.app.myblogpusher.util;

import org.springframework.stereotype.Component;

@Component
public class MarkdownStructureUtil {

	/**
	 * コードブロックか判定する
	 *
	 * @param line 1行分の文字列
	 * @return ```で開始する場合true
	 */
	public boolean isCodeBlockStart(String line) {

		if (line == null) {
			return false;
		}

		return line.trim().startsWith("```");
	}

	/**
	 * 箇条書きか判定する
	 *
	 * Markdown形式:
	 * ・
	 * -
	 * *
	 * 1.
	 *
	 * を対象
	 */
	public boolean isListLine(String line) {

		if (line == null) {
			return false;
		}

		String trimmed = line.trim();

		return trimmed.startsWith("・")
				|| trimmed.startsWith("-")
				|| trimmed.startsWith("*")
				|| trimmed.matches("^\\d+\\..*");
	}

	/**
	 * 文章整形対象外の行か判定する
	 *
	 * 対象外:
	 * ・Markdown見出し
	 * ・【】見出し
	 * ・URL単体
	 */
	public boolean isRawLine(String line) {

		if (line == null) {
			return false;
		}

		String trimmed = line.trim();

		return trimmed.startsWith("#")
				|| trimmed.startsWith("【")
				|| isUrlLine(trimmed);
	}

	/**
	 * URL単体の行か判定する
	 */
	public boolean isUrlLine(String line) {

		if (line == null) {
			return false;
		}

		String trimmed = line.trim();

		return trimmed.startsWith("http://")
				|| trimmed.startsWith("https://");
	}
}