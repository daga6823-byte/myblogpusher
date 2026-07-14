/**
 * Markdown構造を判定するユーティリティ
 *
 * Markdownの構造を判定し、
 * インデント付与や文章整形の対象かどうかを判定する。
 */

package com.app.myblogpusher.util;

import org.springframework.stereotype.Component;

@Component
public class MarkdownStructureUtil {

	/** インデントを付与しない行の接頭辞 */
	private static final String[] NO_INDENT_PREFIXES = {
			"#",
			"【",
			"［",
			"•"
	};

	/** 箇条書きとして扱う接頭辞 */
	private static final String[] LIST_PREFIXES = {
			"・",
			"-",
			"*"
	};

	/**
	 * コードブロック開始・終了行か判定する
	 *
	 * @param line 1行分の文字列
	 * @return ```で始まる場合true
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
	 */
	public boolean isListLine(String line) {

		if (line == null) {
			return false;
		}

		String trimmed = line.trim();

		for (String prefix : LIST_PREFIXES) {
			if (trimmed.startsWith(prefix)) {
				return true;
			}
		}

		return trimmed.matches("^\\d+\\..*");
	}

	/**
	 * 文頭インデントを付与する対象か判定する
	 *
	 * 以下はインデントを付与しない。
	 * ・Markdown見出し
	 * ・【】見出し
	 * ・URL単体
	 * ・箇条書き
	 */
	public boolean shouldIndent(String line) {

		if (line == null) {
			return false;
		}

		String trimmed = line.trim();

		if (isUrlLine(trimmed) || isListLine(trimmed)) {
			return false;
		}

		for (String prefix : NO_INDENT_PREFIXES) {
			if (trimmed.startsWith(prefix)) {
				return false;
			}
		}

		return true;
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