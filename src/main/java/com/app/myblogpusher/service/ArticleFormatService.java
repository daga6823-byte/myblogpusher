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
			body = body.replaceFirst("^\\n+", "");

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
			// コードブロックの開始・終了を検出
			if (line.trim().startsWith("```")) {
				inCodeBlock = !inCodeBlock;
				result.append(line).append("\n");
				continue;
			}

			// コードブロック内はそのまま
			if (inCodeBlock) {
				result.append(line).append("\n");
				continue;
			}

			// 空行はそのまま
			if (line.isBlank()) {
				result.append("\n");
				continue;
			}

			// 箇条書きはそのまま
			if (line.startsWith("・")
					|| line.startsWith("-")
					|| line.startsWith("*")
					|| line.matches("^\\d+\\..*")) {
				result.append(line).append("\n");
				continue;
			}

			// 普通の文章だけ整形
			result.append(formatParagraph(line));
		}
		return result.toString();
	}

	private String formatParagraph(String body) {
		String normalized = body.replace(INDENT, "");

		StringBuilder result = new StringBuilder();
		StringBuilder currentSentence = new StringBuilder();

		for (int i = 0; i < normalized.length(); i++) {
			char c = normalized.charAt(i);
			currentSentence.append(c);

			if (SENTENCE_END_CHARS.indexOf(c) >= 0) {
				// 区切り文字が連続している場合は、続けて同じ文に含める
				int j = i + 1;
				while (j < normalized.length() && SENTENCE_END_CHARS.indexOf(normalized.charAt(j)) >= 0) {
					currentSentence.append(normalized.charAt(j));
					j++;
				}

				// 区切り文字の直後に閉じ括弧が続く場合も、同じ文に含める
				while (j < normalized.length() && CLOSING_BRACKET_CHARS.indexOf(normalized.charAt(j)) >= 0) {
					currentSentence.append(normalized.charAt(j));
					j++;
				}

				appendSentence(result, currentSentence.toString());
				currentSentence.setLength(0);
				i = j - 1;
				; // for文側のi++で次のjに進むよう調整
				continue;
			}
		}

		if (!currentSentence.isEmpty()) {
			String remaining = currentSentence.toString().trim();
			if (!remaining.isEmpty()) {
				if (result.length() > 0) {
					result.append(startsWithTrigger(remaining) ? "\n\n\n" : "\n\n");
				}
				result.append(INDENT).append(remaining);
			}
		}

		return result.append("\n").toString();
	}

	private void appendSentence(StringBuilder result, String sentence) {
		String trimmed = sentence.trim();
		if (trimmed.isEmpty()) {
			return;
		}

		if (result.length() > 0) {
			result.append(startsWithTrigger(trimmed) ? "\n\n\n" : "\n\n");
		}

		result.append(trimmed);
	}

	private boolean startsWithTrigger(String sentence) {
		return sentence.startsWith(PARAGRAPH_BREAK_TRIGGER);
	}
}