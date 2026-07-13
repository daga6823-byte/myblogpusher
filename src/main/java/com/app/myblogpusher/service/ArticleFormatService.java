/**
 * 記事本文の整形処理を担当するサービス
 * フロントマターを保持したまま本文を句点区切りで整形する
 * コードブロック内は整形対象外
 */

package com.app.myblogpusher.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class ArticleFormatService {

	private static final Pattern FRONT_MATTER_PATTERN = Pattern.compile("^\\+\\+\\+[\\s\\S]*?\\+\\+\\+\\n*");
	private static final String PARAGRAPH_BREAK_TRIGGER = "さて";
	private static final String SENTENCE_END_CHARS = "。？！";
	private static final String CLOSING_BRACKET_CHARS = "」』";
	private static final String INDENT = "　";

	/**
	 * フロントマターを保持したまま、本文部分のみを句点区切りで整形する。
	 * 「さて」で始まる文の前は空行（段落区切り）、それ以外は単一改行とする。
	 */
	public String formatContent(String content) {
		if (content == null || content.isBlank()) {
			return content;
		}

		Matcher matcher = FRONT_MATTER_PATTERN.matcher(content);
		String frontMatter = "";
		String body = content;

		if (matcher.find()) {
			String rawFrontMatter = matcher.group();
			body = content.substring(matcher.end());

			// 本文先頭の空行を除去
			body = body.replaceFirst("^(?:[ \\t　]*\\n)+", "");

			// 末尾の改行をすべて取り除いてから、空行をひとつ確実に補う
			frontMatter = rawFrontMatter.replaceAll("\\n+$", "") + "\n\n";
		}

		String formattedBody = formatBody(body);

		return frontMatter + formattedBody;
	}

	private String formatBody(String body) {

		String[] lines = body.replace("\r\n", "\n").split("\n", -1);

		StringBuilder result = new StringBuilder();

		boolean inCodeBlock = false;

		for (String line : lines) {

			String trimmed = line.trim();

			// -------------------------------------------------
			// コードブロック開始・終了
			// -------------------------------------------------
			if (trimmed.startsWith("```")) {
				inCodeBlock = !inCodeBlock;
				result.append(line).append("\n");
				continue;
			}

			// -------------------------------------------------
			// コードブロック内は整形しない
			// -------------------------------------------------
			if (inCodeBlock) {
				result.append(line).append("\n");
				continue;
			}

			// -------------------------------------------------
			// 空行はそのまま
			// -------------------------------------------------
			if (trimmed.isEmpty()) {
				result.append("\n");
				continue;
			}

			// -------------------------------------------------
			// 箇条書き
			// Markdownリンクへ変換のみ行う
			// -------------------------------------------------
			if (isListLine(trimmed)) {
				result.append(formatReferenceLink(trimmed)).append("\n");
				continue;
			}

			// -------------------------------------------------
			// 見出し・URLなどは整形対象外
			// -------------------------------------------------
			if (isRawLine(trimmed)) {
				result.append(trimmed).append("\n");
				continue;
			}

			// -------------------------------------------------
			// 通常文章のみ句点区切り整形
			// -------------------------------------------------
			result.append(formatParagraph(trimmed));
		}

		// 文末の余計な改行を除去
		while (result.length() > 0
				&& result.charAt(result.length() - 1) == '\n') {
			result.deleteCharAt(result.length() - 1);
		}

		return result.toString();
	}

	/**
	 * 通常文章を句点区切りで整形する。
	 * 「さて」で始まる文は段落を区切り、それ以外は1行空けで整形する。
	 */
	private String formatParagraph(String body) {

		String normalized = body.replace(INDENT, "");

		StringBuilder result = new StringBuilder();
		StringBuilder currentSentence = new StringBuilder();

		for (int i = 0; i < normalized.length(); i++) {

			char c = normalized.charAt(i);
			currentSentence.append(c);

			// -------------------------------------------------
			// 文末記号を検出
			// -------------------------------------------------
			if (SENTENCE_END_CHARS.indexOf(c) >= 0) {

				int j = i + 1;

				// 「！！」「！？」「。。」など連続する記号を含める
				while (j < normalized.length()
						&& SENTENCE_END_CHARS.indexOf(normalized.charAt(j)) >= 0) {

					currentSentence.append(normalized.charAt(j));
					j++;
				}

				// 「。」の後ろに 」』 が続く場合も同じ文に含める
				while (j < normalized.length()
						&& CLOSING_BRACKET_CHARS.indexOf(normalized.charAt(j)) >= 0) {

					currentSentence.append(normalized.charAt(j));
					j++;
				}

				appendSentence(result, currentSentence.toString());

				currentSentence.setLength(0);

				i = j - 1;
			}
		}

		// -------------------------------------------------
		// 文末記号のない最後の文章
		// -------------------------------------------------
		if (!currentSentence.isEmpty()) {

			appendSentence(result, currentSentence.toString());

		}

		return result.toString();
	}

	/**
	 * 整形済み文章へ1文追加する。
	 *
	 * 「さて」で始まる文は段落を分けるため空行を1行追加する。
	 * それ以外は通常改行で繋ぐ。
	 */
	private void appendSentence(StringBuilder result, String sentence) {

		String trimmed = sentence.trim();

		if (trimmed.isEmpty()) {
			return;
		}

		// 最初の1文以外は改行を挿入
		if (result.length() > 0) {

			if (startsWithTrigger(trimmed)) {
				// 「さて」は段落を区切る
				result.append("\n\n");
			} else {
				// 通常は1行改行
				result.append("\n");
			}
		}

		// インデントは各文の先頭に付与
		result.append(INDENT).append(trimmed);

	}

	private boolean startsWithTrigger(String sentence) {
		return sentence.startsWith(PARAGRAPH_BREAK_TRIGGER);
	}

	/**
	 * 箇条書きの末尾にURLがある場合はMarkdownリンクへ変換する。
	 *
	 * 例)
	 * ・IMSDb https://example.com
	 * ↓
	 * ・[IMSDb](https://example.com)
	 */
	private String formatReferenceLink(String line) {

		// すでにMarkdownリンクならそのまま
		if (line.contains("](")) {
			return line;
		}

		Pattern pattern = Pattern.compile("^(\\s*[・\\-*]?\\s*)(.+?)\\s+(https?://\\S+)$");
		Matcher matcher = pattern.matcher(line);

		if (!matcher.matches()) {
			return line;
		}

		String prefix = matcher.group(1);
		String title = matcher.group(2).trim();
		String url = matcher.group(3);

		if (title.isEmpty()) {
			return line;
		}

		return prefix + "[" + title + "](" + url + ")";
	}

	/**
	 * 箇条書きかどうか判定する
	 */
	private boolean isListLine(String line) {

		return line.startsWith("・")
				|| line.startsWith("-")
				|| line.startsWith("*")
				|| line.matches("^\\d+\\..*");

	}

	/**
	 * 整形対象外の行かどうか判定する
	 *
	 * 以下は文章整形を行わず、そのまま出力する。
	 * ・Markdown見出し
	 * ・【】見出し
	 * ・URLのみの行
	 */
	private boolean isRawLine(String line) {

		String trimmed = line.trim();

		return trimmed.startsWith("#")
				|| trimmed.startsWith("【")
				|| trimmed.startsWith("http://")
				|| trimmed.startsWith("https://");

	}
}